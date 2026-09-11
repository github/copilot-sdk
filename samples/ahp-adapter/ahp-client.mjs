import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import { join, resolve } from "node:path";
import { pathToFileURL } from "node:url";
import { ActionType, PROTOCOL_VERSION, ResponsePartKind } from "@microsoft/agent-host-protocol";
import { AhpClient } from "@microsoft/agent-host-protocol/client";
import { WebSocketTransport } from "@microsoft/agent-host-protocol/ws";
import WebSocket from "ws";

globalThis.WebSocket ??= WebSocket;
const [url, workspace] = process.argv.slice(2);
if (!url || !workspace) throw new Error("Usage: node ahp-client.mjs <ahp-url> <workspace-path>");
const client = new AhpClient(await WebSocketTransport.connect(url));
client.connect();
const deadline = setTimeout(() => {
    console.error("Timed out waiting for the AHP demonstration.");
    process.exit(1);
}, 180_000);

try {
    const clientId = randomUUID();
    await client.initialize({ clientId, protocolVersions: [PROTOCOL_VERSION] });

    // These are ordinary AHP commands. No SDK filesystem adapter is involved.
    const directory = pathToFileURL(workspace).href;
    const { entries } = await client.request("resourceList", { channel: "ahp-root://", uri: directory });
    assert(entries.some(entry => entry.name === "hello.txt"));
    console.log(`AHP filesystem list: ${entries.map(entry => entry.name).join(", ")}`);
    const file = await client.request("resourceRead", {
        channel: "ahp-root://", uri: pathToFileURL(join(workspace, "hello.txt")).href, encoding: "utf-8",
    });
    assert.equal(file.encoding, "utf-8");
    assert.equal(file.data.trim(), "Hello from the existing copilotd filesystem implementation.");
    console.log(`AHP filesystem read: ${file.data.trim()}`);
    await assert.rejects(
        client.request("resourceRead", {
            channel: "ahp-root://", uri: pathToFileURL(resolve(workspace, "../package.json")).href,
        }),
        error => error.code === -32009,
    );
    console.log("AHP filesystem boundary: outside-root read rejected");

    const sessionId = randomUUID();
    const sessionUri = `ahp-session:/${sessionId}`;
    await client.request("createSession", {
        channel: sessionUri,
        provider: "copilot",
        workingDirectories: [directory],
        activeClient: { clientId, displayName: "SDK child PoC", tools: [] },
    });
    const { result } = await client.subscribe(sessionUri);
    const chatUri = result.snapshot?.state.defaultChat;
    assert(chatUri, "Created session must have a default chat");
    const { items } = await client.request("listSessions", { channel: "ahp-root://" });
    assert(items.some(item => item.resource === sessionUri));
    console.log(`AHP session: ${sessionUri}`);
    const { subscription } = await client.subscribe(chatUri);
    const events = subscription[Symbol.asyncIterator]();

    async function turn(prompt) {
        const turnId = randomUUID();
        const parts = new Map();
        let deltas = 0;
        client.dispatch(chatUri, {
            type: ActionType.ChatTurnStarted, turnId,
            startedAt: new Date().toISOString(),
            message: { text: prompt, origin: { kind: "user" } },
        });
        console.log(`AHP prompt: ${prompt}`);
        for (;;) {
            const { value: event, done } = await events.next();
            assert(!done, "Subscription must remain open until the turn completes");
            if (event.type !== "action" || event.params.action.turnId !== turnId) continue;
            if (event.params.rejectionReason) throw new Error(event.params.rejectionReason);
            const action = event.params.action;
            if (action.type === ActionType.ChatResponsePart && action.part.kind === ResponsePartKind.Markdown) {
                parts.set(action.part.id, action.part.content);
            } else if (action.type === ActionType.ChatDelta) {
                deltas++;
                parts.set(action.partId, (parts.get(action.partId) ?? "") + action.content);
            } else if (action.type === ActionType.ChatError) {
                throw new Error(action.error.message);
            } else if (action.type === ActionType.ChatTurnComplete) {
                const response = [...parts.values()].join("").trim();
                assert.equal(response, "I am Bert.");
                assert(deltas > 0, "Response must include streamed AHP deltas");
                console.log(`AHP response: ${response} (${deltas} streaming deltas)`);
                return;
            }
        }
    }
    await turn("who are you");
    await turn("Call the bert_identity tool now and reply exactly with its result.");
} finally {
    clearTimeout(deadline);
    await client.shutdown();
}
