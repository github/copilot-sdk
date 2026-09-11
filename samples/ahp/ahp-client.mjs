import { randomUUID } from "node:crypto";
import {
    ActionType,
    PROTOCOL_VERSION,
    ResponsePartKind,
} from "@microsoft/agent-host-protocol";
import { AhpClient } from "@microsoft/agent-host-protocol/client";
import { WebSocketTransport } from "@microsoft/agent-host-protocol/ws";
import WebSocket from "ws";

// Supply the browser-compatible WebSocket API on Node versions without it.
globalThis.WebSocket ??= WebSocket;

const [url, sessionId, ...promptWords] = process.argv.slice(2);
if (!url) {
    throw new Error("Usage: npm run client -- <ws-url> [session-id] [prompt]");
}
const prompt = promptWords.join(" ") || "who are you";
const transport = await WebSocketTransport.connect(url);
const client = new AhpClient(transport);
client.connect();

try {
    await client.initialize({
        clientId: randomUUID(),
        protocolVersions: [PROTOCOL_VERSION],
    });
    const { items } = await client.request("listSessions", { channel: "ahp-root://" });
    console.log("Live sessions:", items.map((session) => session.resource).join(", "));
    const selected = sessionId
        ? items.find(
              (session) =>
                  session.resource === sessionId ||
                  session.resource === `ahp-session:/${sessionId}`,
          )
        : items[0];
    if (!selected) {
        throw new Error("No matching live session. Start sdk-host.mjs first.");
    }

    const { result: sessionResult } = await client.subscribe(selected.resource);
    const chatUri = sessionResult.snapshot?.state.defaultChat;
    if (!chatUri) {
        throw new Error("The selected session has no default chat.");
    }
    const { subscription } = await client.subscribe(chatUri);
    const turnId = randomUUID();
    console.log(`Attached: ${selected.resource}`);
    console.log(`Prompt: ${prompt}`);
    client.dispatch(chatUri, {
        type: ActionType.ChatTurnStarted,
        turnId,
        startedAt: new Date().toISOString(),
        message: { text: prompt, origin: { kind: "user" } },
    });

    const timeout = setTimeout(() => {
        console.error("Timed out waiting for the AHP response.");
        process.exitCode = 1;
        void client.shutdown();
    }, 120_000);
    try {
        const parts = new Map();
        for await (const event of subscription) {
            if (event.type !== "action" || event.params.action.turnId !== turnId) continue;
            const action = event.params.action;
            if (
                action.type === ActionType.ChatResponsePart &&
                action.part.kind === ResponsePartKind.Markdown
            ) {
                parts.set(action.part.id, action.part.content);
            } else if (action.type === ActionType.ChatDelta) {
                parts.set(action.partId, (parts.get(action.partId) ?? "") + action.content);
            } else if (action.type === ActionType.ChatTurnComplete) {
                console.log(`AHP response: ${[...parts.values()].join("")}`);
                break;
            } else if (action.type === ActionType.ChatError) {
                throw new Error(action.error.message);
            }
        }
    } finally {
        clearTimeout(timeout);
    }
} finally {
    await client.shutdown();
}
