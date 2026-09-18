/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { randomUUID } from "node:crypto";
import {
    ActionType,
    AhpErrorCodes,
    MessageKind,
    ResponsePartKind,
    type Snapshot,
} from "@microsoft/agent-host-protocol";
import { AhpClient, AhpStateMirror, RpcError } from "@microsoft/agent-host-protocol/client";
import { WebSocketTransport } from "@microsoft/agent-host-protocol/ws";

async function exerciseModel(client: AhpClient, snapshot: Snapshot): Promise<void> {
    const mirror = new AhpStateMirror();
    mirror.applySnapshot(snapshot);
    const session = mirror.getSession(snapshot.resource);
    const chat = session?.defaultChat ?? session?.chats[0]?.resource;
    if (!chat) throw new Error("Native session snapshot did not advertise a chat");
    const { subscription } = await client.subscribe(chat);
    const turnId = randomUUID();
    const parts = new Map<string, string>();
    const labelCalls = new Set<string>();
    let labelCompleted = false;
    let timeout: ReturnType<typeof setTimeout> | undefined;
    try {
        client.dispatch(chat, {
            type: ActionType.ChatTurnStarted,
            turnId,
            startedAt: new Date().toISOString(),
            message: {
                text: "Call demo_label to get your application label, then report its result. Follow your application's response-prefix instructions.",
                origin: { kind: MessageKind.User },
            },
        });
        const completion = (async () => {
            for await (const event of subscription) {
                if (event.type !== "action") continue;
                const action = event.params.action;
                if (!("turnId" in action) || action.turnId !== turnId) continue;
                switch (action.type) {
                    case ActionType.ChatResponsePart:
                        if (action.part.kind === ResponsePartKind.Markdown) {
                            parts.set(action.part.id, action.part.content);
                        }
                        break;
                    case ActionType.ChatDelta:
                        parts.set(action.partId, (parts.get(action.partId) ?? "") + action.content);
                        process.stdout.write(action.content);
                        break;
                    case ActionType.ChatToolCallStart:
                        console.log("Native tool call:", action.toolName);
                        if (action.toolName === "demo_label") labelCalls.add(action.toolCallId);
                        break;
                    case ActionType.ChatToolCallComplete:
                        if (labelCalls.has(action.toolCallId)) {
                            if (!action.result.success)
                                throw new Error("Local demo_label tool failed");
                            labelCompleted = true;
                            console.log("Local demo_label result:", action.result);
                        }
                        break;
                    case ActionType.ChatError:
                        throw new Error(`Native model turn failed: ${JSON.stringify(action.part)}`);
                    case ActionType.ChatTurnComplete: {
                        const text = [...parts.values()].join("");
                        console.log("\nCompleted model response:", text);
                        if (!text.includes("SDK demo:") || !labelCompleted) {
                            throw new Error(
                                "Did not observe both the distinctive prompt and demo_label execution"
                            );
                        }
                        return;
                    }
                }
            }
            throw new Error("AHP subscription closed before the model turn completed");
        })();
        const timedOut = new Promise<never>((_, reject) => {
            timeout = setTimeout(
                () => reject(new Error("Model exercise timed out after 120 seconds")),
                120_000
            );
        });
        await Promise.race([completion, timedOut]);
    } finally {
        clearTimeout(timeout);
        await subscription.close();
        await client.unsubscribe(chat);
    }
}

const transport = await WebSocketTransport.connect(process.env.AHP_URL ?? "ws://127.0.0.1:8765");
const client = new AhpClient(transport);
client.connect();
try {
    console.log(
        "initialize:",
        await client.initialize({
            clientId: `sdk-demo-${randomUUID()}`,
            protocolVersions: ["0.9.0"],
            initialSubscriptions: ["ahp-root://"],
        })
    );
    const before = await client.request("listSessions", { channel: "ahp-root://" });
    console.log("Visible sessions (private SDK session is excluded):", before.items);
    const excludedId = process.env.EXCLUDED_SESSION_ID;
    if (excludedId) {
        const excludedUri = `ahp-session:/${excludedId}`;
        if (before.items.some((item) => item.resource === excludedUri)) {
            throw new Error("Native endpoint listed the excluded session");
        }
        try {
            const unexpected = await client.subscribe(excludedUri);
            await unexpected.subscription.close();
            throw new Error("Native endpoint unexpectedly authorized the excluded session URI");
        } catch (error) {
            if (
                !(error instanceof RpcError) ||
                ![
                    AhpErrorCodes.PermissionDenied,
                    AhpErrorCodes.SessionNotFound,
                    AhpErrorCodes.NotFound,
                ].some((code) => code === error.code)
            ) {
                throw error;
            }
            console.log(
                "Direct excluded-URI subscription rejected by native authorization:",
                error.message
            );
        }
    } else {
        console.log("EXCLUDED_SESSION_ID not set; direct excluded-URI probe skipped.");
    }
    const coldId = process.env.COLD_SESSION_ID;
    const existingUri = coldId ? `ahp-session:/${coldId}` : before.items[0]?.resource;
    if (existingUri) {
        const existing = await client.subscribe(existingUri);
        console.log("Authorized persisted-session snapshot:", existing.result);
        console.log(
            "The first subscription after owner disconnect exercises cold resume; a live attachment does not invoke that override. Check the SDK server log."
        );
        if (process.env.AHP_RUN_MODEL === "1") {
            if (!existing.result.snapshot) throw new Error("Missing persisted-session snapshot");
            await exerciseModel(client, existing.result.snapshot);
        }
        await existing.subscription.close();
        await client.unsubscribe(existingUri);
    }
    const channel = `ahp-session:/${randomUUID()}`;
    await client.request("createSession", { channel, provider: "copilot" });
    console.log("Created through the SDK callback:", channel);
    const { result, subscription } = await client.subscribe(channel);
    console.log("New live session snapshot (attachment, not a resume override):", result);
    if (process.env.AHP_RUN_MODEL === "1") {
        if (!result.snapshot) throw new Error("Missing new-session snapshot");
        await exerciseModel(client, result.snapshot);
    }
    console.log(
        "Visible sessions after create:",
        await client.request("listSessions", { channel: "ahp-root://" })
    );
    await subscription.close();
    await client.unsubscribe(channel);
} finally {
    await client.shutdown();
}
