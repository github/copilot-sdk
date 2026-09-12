import { CopilotClient, RuntimeConnection } from "../../nodejs/dist/index.js";
import { randomBytes } from "node:crypto";
import { createServer } from "node:http";
import express from "express";
import { WebSocketServer } from "ws";

const path = process.argv[2] ?? process.env.COPILOT_CLI_PATH;
if (!path) {
    throw new Error("Pass the local copilot-runtime binary path or set COPILOT_CLI_PATH.");
}

const client = new CopilotClient({
    connection: RuntimeConnection.forStdio({ path }),
    gitHubToken: process.env.GITHUB_TOKEN ?? process.env.GH_TOKEN,
});
let endpoint;
const server = createServer(express());
const sockets = new WebSocketServer({ noServer: true, maxPayload: 8 * 1024 * 1024 });

try {
    await client.start();
    endpoint = await client.createAhpEndpoint();
    // A short-lived capability authenticates this local demo; production apps own auth.
    const token = randomBytes(24).toString("hex");
    server.on("upgrade", (request, socket, head) => {
        const target = new URL(request.url, "http://localhost");
        if (target.pathname !== "/ahp" || target.searchParams.get("token") !== token) {
            socket.end("HTTP/1.1 403 Forbidden\r\nConnection: close\r\n\r\n");
            return;
        }
        sockets.handleUpgrade(request, socket, head, (ws) => {
            const connection = endpoint.acceptConnection({
                send: (text, signal) => new Promise((resolve, reject) => {
                    if (signal.aborted) return reject(signal.reason);
                    ws.send(text, (error) => error ? reject(error) : resolve());
                }),
                close: (error) => error ? ws.terminate() : ws.close(),
            });
            ws.on("message", (data, isBinary) => {
                if (isBinary) return ws.terminate();
                ws.pause();
                void connection.receive(data.toString()).then(
                    () => ws.resume(),
                    () => ws.terminate(),
                );
            });
            ws.on("close", () => void connection.end().catch(() => ws.terminate()));
            ws.on("error", () => ws.terminate());
            void connection.closed.catch((error) => {
                console.error(`AHP connection failed: ${error.message}`);
                ws.terminate();
            });
        });
    });
    await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
    const url = `ws://127.0.0.1:${server.address().port}/ahp?token=${token}`;
    const session = await client.createSession({
        model: process.env.COPILOT_MODEL ?? "gpt-4.1",
        streaming: true,
        systemMessage: {
            mode: "replace",
            content: "You are Bert. When asked who you are, reply exactly: I am Bert.",
        },
        onPermissionRequest: async () => ({ kind: "denied-interactively-by-user" }),
    });
    let streamedDeltas = 0;
    session.on("assistant.message_delta", () => streamedDeltas++);
    session.on("assistant.message", (event) => {
        console.log(`[SDK observed ${session.sessionId}] ${event.data.content}`);
        console.log(`[SDK streaming] ${streamedDeltas} message deltas`);
        streamedDeltas = 0;
    });
    session.on("session.error", (event) => {
        console.error(`[SDK session error] ${event.data.message}`);
    });

    console.log(`AHP URL: ${url}`);
    console.log(`SDK session ID: ${session.sessionId}`);
    console.log(`In another terminal: npm run client -- '${url}' '${session.sessionId}'`);
    console.log("Waiting for AHP prompts. Press Ctrl+C to stop.");
    await new Promise((resolve) => {
        process.once("SIGINT", resolve);
        process.once("SIGTERM", resolve);
    });
} finally {
    try {
        await endpoint?.dispose();
    } finally {
        for (const ws of sockets.clients) ws.terminate();
        sockets.close();
        await new Promise((resolve) => server.close(resolve));
        await client.stop();
    }
}
