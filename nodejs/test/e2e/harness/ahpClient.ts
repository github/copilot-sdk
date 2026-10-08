/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import { once } from "node:events";
import { pathToFileURL } from "node:url";
import {
    ActionType,
    MessageKind,
    PROTOCOL_VERSION,
    ResponsePartKind,
    ToolCallConfirmationReason,
    ToolCallContributorKind,
    ToolResultContentType,
    type ClientCapabilities,
    type RootState,
    type SessionState,
    type SessionActiveClient,
    type StateAction,
} from "@microsoft/agent-host-protocol-v09";
import { AhpClient, type Subscription } from "@microsoft/agent-host-protocol-v09/client";
import { WebSocketTransport } from "@microsoft/agent-host-protocol-v09/ws";
import WebSocket from "ws";
import { sealAhpAuthToken } from "./ahpSealedAuth.js";

export async function withDeadline<T>(promise: Promise<T>, label: string, ms = 30_000): Promise<T> {
    let timer: ReturnType<typeof setTimeout> | undefined;
    try {
        return await Promise.race([
            promise,
            new Promise<never>((_, reject) => {
                timer = setTimeout(() => reject(new Error(`Timed out: ${label}`)), ms);
            }),
        ]);
    } finally {
        clearTimeout(timer);
    }
}

export async function connectAhp(
    host: { url?: string; token?: string },
    capabilities?: ClientCapabilities,
    clientId: string = randomUUID()
) {
    assert(host.url, "A local listener URL is required for a WebSocket connection");
    const url = new URL(host.url);
    if (host.token !== undefined) url.searchParams.set("tkn", host.token);
    const socket = new WebSocket(url, { handshakeTimeout: 10_000 });
    try {
        await once(socket, "open");
        const transport = WebSocketTransport.fromSocket(socket as unknown as globalThis.WebSocket);
        const client = new AhpClient(transport, { requestTimeoutMs: 15_000 });
        client.connect();
        // The initialize() convenience method does not forward capabilities.
        const initialized = await client.request("initialize", {
            channel: "ahp-root://",
            clientId,
            protocolVersions: [PROTOCOL_VERSION],
            capabilities,
        });
        assert.equal(initialized.protocolVersion, PROTOCOL_VERSION);
        return { client, clientId, transport, initialized };
    } catch (error) {
        socket.terminate();
        throw error;
    }
}

export async function authenticateAhp(
    ahp: Awaited<ReturnType<typeof connectAhp>>,
    githubToken: string
) {
    assert(githubToken, "Session creation requires a GitHub credential");
    const { result: root } = await ahp.client.subscribe("ahp-root://");
    const agent = (root.snapshot?.state as RootState | undefined)?.agents.find(
        (entry) => entry.provider === "copilot"
    );
    const resource = agent?.protectedResources?.find(
        (entry) => entry.resource_name === "GitHub API"
    );
    assert(resource, "The Copilot agent must advertise its GitHub protected resource");
    await ahp.client.request("authenticate", {
        channel: "ahp-root://",
        resource: resource.resource,
        token: await sealAhpAuthToken(
            ahp.initialized._meta,
            (root.snapshot?.state as RootState)._meta,
            resource.resource,
            githubToken
        ),
    });
}

export async function createAhpSession(
    ahp: Awaited<ReturnType<typeof connectAhp>>,
    workDir: string,
    githubToken: string,
    tools: SessionActiveClient["tools"] = []
) {
    await authenticateAhp(ahp, githubToken);
    // This token names the public AHP URI, not the independently allocated SDK session.
    const sessionId = randomUUID();
    const sessionUri = `ahp-session:/${sessionId}`;
    await ahp.client.request("createSession", {
        channel: sessionUri,
        provider: "copilot",
        workingDirectories: [pathToFileURL(workDir).href],
        activeClient: { clientId: ahp.clientId, displayName: "Runtime host E2E", tools },
    });
    const { result } = await ahp.client.subscribe(sessionUri);
    const chatUri = (result.snapshot?.state as SessionState | undefined)?.defaultChat;
    assert(chatUri, "AHP createSession must create a default chat");
    const { subscription } = await ahp.client.subscribe(chatUri);
    return { sessionId, sessionUri, chatUri, subscription };
}

export async function streamedTurn(
    client: AhpClient,
    chatUri: string,
    subscription: Subscription,
    prompt: string,
    model = "claude-sonnet-5",
    onAction?: (action: StateAction) => void,
    clientTools?: { clientId: string; handlers: Record<string, (input: unknown) => string> }
) {
    const turnId = randomUUID();
    const pendingClientCalls = new Map<string, string>();
    const completedClientCalls = new Set<string>();
    const parts = new Map<string, string>();
    let deltas = 0;
    client.dispatch(chatUri, {
        type: ActionType.ChatTurnStarted,
        turnId,
        startedAt: new Date().toISOString(),
        message: { text: prompt, origin: { kind: MessageKind.User }, model: { id: model } },
    });
    return withDeadline(
        (async () => {
            for await (const event of subscription) {
                if (event.type !== "action") continue;
                if (event.params.rejectionReason) throw new Error(event.params.rejectionReason);
                const action = event.params.action;
                if (!("turnId" in action) || action.turnId !== turnId) continue;
                onAction?.(action);
                if (
                    clientTools &&
                    action.type === ActionType.ChatToolCallStart &&
                    action.contributor?.kind === ToolCallContributorKind.Client
                ) {
                    assert.equal(action.contributor.clientId, clientTools.clientId);
                    assert(
                        clientTools.handlers[action.toolName],
                        `Unexpected client tool ${action.toolName}`
                    );
                    assert(!pendingClientCalls.has(action.toolCallId));
                    assert(!completedClientCalls.has(action.toolCallId));
                    pendingClientCalls.set(action.toolCallId, action.toolName);
                } else if (
                    clientTools &&
                    action.type === ActionType.ChatToolCallReady &&
                    pendingClientCalls.has(action.toolCallId)
                ) {
                    assert.equal(action.confirmed, ToolCallConfirmationReason.NotNeeded);
                    assert.equal(typeof action.toolInput, "string");
                    const name = pendingClientCalls.get(action.toolCallId)!;
                    pendingClientCalls.delete(action.toolCallId);
                    completedClientCalls.add(action.toolCallId);
                    const text = clientTools.handlers[name](JSON.parse(action.toolInput as string));
                    client.dispatch(chatUri, {
                        type: ActionType.ChatToolCallComplete,
                        turnId,
                        toolCallId: action.toolCallId,
                        result: {
                            success: true,
                            pastTenseMessage: `Ran ${name}`,
                            content: [{ type: ToolResultContentType.Text, text }],
                        },
                    });
                }
                if (
                    action.type === ActionType.ChatResponsePart &&
                    action.part.kind === ResponsePartKind.Markdown
                ) {
                    parts.set(action.part.id, action.part.content);
                } else if (action.type === ActionType.ChatDelta) {
                    deltas++;
                    parts.set(action.partId, (parts.get(action.partId) ?? "") + action.content);
                } else if (action.type === ActionType.ChatError) {
                    throw new Error(action.part.error.message);
                } else if (action.type === ActionType.ChatTurnComplete) {
                    return { text: [...parts.values()].join("").trim(), deltas };
                }
            }
            throw new Error("AHP subscription closed before chat/turnComplete");
        })(),
        "streamed AHP turn"
    );
}
