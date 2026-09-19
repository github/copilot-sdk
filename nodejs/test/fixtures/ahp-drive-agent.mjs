/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// Cross-language E2E peer: use the standard AHP client, not the SDK session API.
import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import { ActionType, MessageKind, ResponsePartKind } from "@microsoft/agent-host-protocol";
import { AhpClient, AhpStateMirror, RpcError } from "@microsoft/agent-host-protocol/client";
import { WebSocketTransport } from "@microsoft/agent-host-protocol/ws";

const [url, existingId, excludedId] = process.argv.slice(2);
const channel = `ahp-session:/${existingId || randomUUID()}`;
const deadline = setTimeout(() => {
    console.error("AHP agent turn timed out");
    process.exit(1);
}, 60_000);

async function connect() {
    const client = new AhpClient(await WebSocketTransport.connect(url));
    client.connect();
    await client.initialize({ clientId: randomUUID(), protocolVersions: ["0.9.0"] });
    return client;
}

try {
    const client = await connect();
    try {
        const listed = await client.request("listSessions", { channel: "ahp-root://" });
        assert(!listed.items.some((item) => item.resource === `ahp-session:/${excludedId}`));
        await assert.rejects(client.subscribe(`ahp-session:/${excludedId}`), RpcError);
        if (!existingId) {
            await client.request("createSession", { channel, provider: "copilot" });
        }
        const { result } = await client.subscribe(channel);
        const mirror = new AhpStateMirror();
        mirror.applySnapshot(result.snapshot);
        const chat = mirror.getSession(channel).defaultChat;
        assert(chat);
        const { subscription } = await client.subscribe(chat);
        const turnId = randomUUID();
        const parts = new Map();
        let completedTool = false;
        const tools = new Set();
        client.dispatch(chat, {
            type: ActionType.ChatTurnStarted,
            turnId,
            startedAt: new Date().toISOString(),
            message: {
                text: "Use encrypt_string to encrypt this string: Hello",
                origin: { kind: MessageKind.User },
            },
        });
        let completed = false;
        for await (const event of subscription) {
            if (event.type !== "action") continue;
            const action = event.params.action;
            if (action.turnId !== turnId) continue;
            if (
                action.type === ActionType.ChatResponsePart &&
                action.part.kind === ResponsePartKind.Markdown
            ) {
                parts.set(action.part.id, action.part.content);
            } else if (action.type === ActionType.ChatDelta) {
                parts.set(action.partId, (parts.get(action.partId) ?? "") + action.content);
            } else if (
                action.type === ActionType.ChatToolCallStart &&
                action.toolName === "encrypt_string"
            ) {
                tools.add(action.toolCallId);
            } else if (
                action.type === ActionType.ChatToolCallComplete &&
                tools.has(action.toolCallId)
            ) {
                assert(action.result.success, JSON.stringify(action.result));
                completedTool = true;
            } else if (action.type === ActionType.ChatError) {
                assert.fail(JSON.stringify(action.part));
            } else if (action.type === ActionType.ChatTurnComplete) {
                completed = true;
                break;
            }
        }
        assert(completed, "AHP stream closed before turn completion");
        assert(completedTool, "Rust application tool did not complete over AHP");
        assert.match([...parts.values()].join(""), /HELLO/);
    } finally {
        await client.shutdown();
    }

    const reconnected = await connect();
    try {
        const { result } = await reconnected.subscribe(channel);
        const mirror = new AhpStateMirror();
        mirror.applySnapshot(result.snapshot);
        const chat = await reconnected.subscribe(mirror.getSession(channel).defaultChat);
        assert.match(JSON.stringify(chat.result.snapshot), /HELLO/);
    } finally {
        await reconnected.shutdown();
    }
    console.log("AHP_TOOL_TURN_AND_RECONNECT_OK");
} finally {
    clearTimeout(deadline);
}
