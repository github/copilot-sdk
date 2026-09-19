/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { randomUUID } from "node:crypto";
import { createInterface } from "node:readline/promises";
import { WebSocket, WebSocketServer } from "ws";
import {
    CopilotClient,
    RuntimeConnection,
    type AhpConnection,
    type SessionConfig,
} from "../src/index.js";

const runtimePath = process.env.COPILOT_RUNTIME_PATH;
if (!runtimePath) {
    throw new Error("Set COPILOT_RUNTIME_PATH to the matching locally built runtime executable");
}

const terminal = createInterface({ input: process.stdin, output: process.stdout });
const client = new CopilotClient({
    connection: RuntimeConnection.forStdio({ path: runtimePath }),
});
const authorizedSessionIds = new Set<string>();
let questions = Promise.resolve();
function question(prompt: string): Promise<string> {
    const answer = questions.then(() => terminal.question(prompt));
    questions = answer.then(
        () => undefined,
        () => undefined
    );
    return answer;
}

// All executable configuration stays in this SDK process, not in ahp.registerEndpoint.
const config: SessionConfig = {
    systemMessage: {
        mode: "append",
        content:
            "You are the SDK-owned AHP demo. Begin replies with 'SDK demo:'. Use demo_label when asked for your label.",
    },
    tools: [
        {
            name: "demo_label",
            description:
                "Return this application's harmless demo label without accessing files or network.",
            parameters: { type: "object", properties: {}, additionalProperties: false },
            handler: () => "SDK-owned native AHP example",
        },
    ],
    onPermissionRequest: async (request, invocation) => {
        console.log("Permission request:", invocation.sessionId, request);
        const answer = await question("Allow once? [y/N] ");
        return answer.trim().toLowerCase() === "y"
            ? { kind: "approve-once" }
            : { kind: "denied-interactively-by-user" };
    },
    onUserInputRequest: async (request) => {
        console.log("Agent question:", request.question, request.choices ?? []);
        return { answer: await question("Your answer: "), wasFreeform: true };
    },
};

try {
    const shared = await client.createSession({
        ...config,
        sessionId: randomUUID(),
        name: "SDK-owned AHP cold-resume example",
    });
    const privateSession = await client.createSession({ ...config, sessionId: randomUUID() });
    authorizedSessionIds.add(shared.sessionId);
    // Naming opts into persistence before the first turn. Drop its ordinary SDK
    // owner before registering the endpoint, retaining authorization to resume it.
    await shared.rpc.name.set({ name: "SDK-owned AHP cold-resume example" });
    await client.rpc.sessions.save({ sessionId: shared.sessionId });
    await shared.disconnect();
    const endpoint = await client.createAhpEndpoint({
        onListSessions: () => [...authorizedSessionIds].map((sessionId) => ({ sessionId })),
        onCreateSession: async (request, { signal }) => {
            signal.throwIfAborted();
            console.log("AHP create override:", request.requestedSessionId);
            const session = await client.createSession({
                ...config,
                sessionId: request.requestedSessionId,
            });
            signal.throwIfAborted();
            authorizedSessionIds.add(session.sessionId);
            return session;
        },
        onResumeSession: async ({ sessionId }, { signal }) => {
            signal.throwIfAborted();
            console.log("AHP cold-resume override:", sessionId);
            const session = await client.resumeSession(sessionId, config);
            signal.throwIfAborted();
            return session;
        },
    });
    const server = new WebSocketServer({
        host: "127.0.0.1",
        port: Number(process.env.PORT ?? 8765),
        maxPayload: 1024 * 1024,
    });
    server.on("connection", (socket) => {
        // Do not accumulate application messages while openConnection is pending.
        socket.pause();
        let logical: AhpConnection | undefined;
        const fail = (error: unknown) => {
            console.error("AHP transport:", error);
            socket.close(1011, "AHP transport failure");
        };
        socket.on("error", fail);
        socket.on("close", () => {
            void logical?.close().catch(fail);
        });
        void endpoint
            .openConnection({
                onMessage: (message) =>
                    new Promise<void>((resolve, reject) => {
                        if (socket.readyState !== WebSocket.OPEN) {
                            reject(new Error("WebSocket is not open"));
                            return;
                        }
                        socket.send(message, (error) => (error ? reject(error) : resolve()));
                    }),
                onClose: (error) => {
                    if (error) console.error("Native AHP connection:", error);
                    socket.close(
                        error ? 1011 : 1000,
                        error ? "AHP connection failed" : "AHP connection closed"
                    );
                },
            })
            .then(async (connection) => {
                logical = connection;
                if (socket.readyState !== WebSocket.OPEN) {
                    await connection.close();
                    return;
                }
                socket.on("message", (data, binary) => {
                    if (binary) {
                        socket.close(1003, "Text messages required");
                        void connection.close().catch(fail);
                        return;
                    }
                    void connection.send(data.toString()).catch(fail);
                });
                socket.resume();
            })
            .catch(fail);
    });
    await new Promise<void>((resolve, reject) => {
        server.once("listening", resolve);
        server.once("error", reject);
    });
    console.log("SDK-owned WebSocket listener:", server.address());
    console.log(
        "Authorized persisted session (ordinary SDK owner disconnected):",
        shared.sessionId
    );
    console.log("Excluded by native onListSessions authorization:", privateSession.sessionId);
    console.log("Copy these variables into the AHP client command:");
    console.log(
        `COLD_SESSION_ID=${shared.sessionId} EXCLUDED_SESSION_ID=${privateSession.sessionId}`
    );
    console.log(
        "Permission and ask-user requests are answered in this terminal, not auto-approved."
    );
    await new Promise<void>((resolve) => {
        process.once("SIGINT", resolve);
        process.once("SIGTERM", resolve);
    });
    try {
        await endpoint.dispose();
    } finally {
        for (const socket of server.clients) socket.terminate();
        await new Promise<void>((resolve, reject) =>
            server.close((error) => (error ? reject(error) : resolve()))
        );
    }
} finally {
    terminal.close();
    const errors = await client.stop();
    if (errors.length) throw new AggregateError(errors, "SDK shutdown failed");
}
