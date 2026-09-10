/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { PassThrough } from "node:stream";
import { describe, expect, it, onTestFinished } from "vitest";
import {
    createMessageConnection,
    ResponseError,
    StreamMessageReader,
    StreamMessageWriter,
} from "vscode-jsonrpc/node.js";
import type { MessageOptions, MessageSource, SessionEvent } from "../src/index.js";
import { CopilotSession } from "../src/session.js";

function sessionPair(traceContextProvider?: ConstructorParameters<typeof CopilotSession>[3]) {
    const clientToServer = new PassThrough();
    const serverToClient = new PassThrough();
    const client = createMessageConnection(
        new StreamMessageReader(serverToClient),
        new StreamMessageWriter(clientToServer)
    );
    const server = createMessageConnection(
        new StreamMessageReader(clientToServer),
        new StreamMessageWriter(serverToClient)
    );
    onTestFinished(() => {
        client.dispose();
        server.dispose();
        clientToServer.destroy();
        serverToClient.destroy();
    });
    client.listen();
    server.listen();
    return {
        session: new CopilotSession("session-1", client, undefined, traceContextProvider),
        server,
    };
}

const sources: (MessageSource | undefined)[] = [undefined, "user", "system", "agent-sender-id"];
const modes: MessageOptions["mode"][] = [undefined, "enqueue", "immediate"];

it("omits source when sending a plain human prompt", async () => {
    const { session, server } = sessionPair();
    server.onRequest("session.send", (params: unknown) => {
        expect(params).toEqual({ sessionId: "session-1", prompt: "hello" });
        return { messageId: "message-1" };
    });
    await expect(session.send("hello")).resolves.toBe("message-1");
});

describe.each(sources)("message source %s", (source) => {
    it.each(modes)("preserves the wire payload with delivery mode %s", async (mode) => {
        const { session, server } = sessionPair();
        const expected = {
            sessionId: "session-1",
            prompt: "hello",
            ...(source === undefined ? {} : { source }),
            ...(mode === undefined ? {} : { mode }),
        };
        server.onRequest("session.send", (params: unknown) => {
            expect(params).toEqual(expected);
            return { messageId: "message-1" };
        });
        await expect(session.send({ prompt: "hello", source, mode })).resolves.toBe("message-1");
    });

    it("preserves attachments, display text, headers, agent mode and tracing", async () => {
        const trace = {
            traceparent: "00-fedcba0987654321fedcba0987654321-abcdef1234567890-01",
            tracestate: "vendor=source",
        };
        const { session, server } = sessionPair(() => trace);
        const options: MessageOptions = {
            prompt: "context updated",
            source,
            mode: "immediate",
            agentMode: "plan",
            attachments: [{ type: "blob", data: "aGk=", mimeType: "text/plain" }],
            displayPrompt: "Context updated",
            requestHeaders: { "X-Tag": "context" },
        };
        const expected = { sessionId: "session-1", ...trace, ...options };
        if (source === undefined) {
            delete expected.source;
        }
        server.onRequest("session.send", (params: unknown) => {
            expect(params).toEqual(expected);
            return { messageId: "message-1" };
        });
        await expect(session.send(options)).resolves.toBe("message-1");
    });

    it.each(modes)(
        "allows sendAndWait to finish without assistant output in mode %s",
        async (mode) => {
            const { session, server } = sessionPair();
            server.onRequest("session.send", (params: unknown) => {
                expect(params).toEqual({
                    sessionId: "session-1",
                    prompt: "context updated",
                    ...(source === undefined ? {} : { source }),
                    ...(mode === undefined ? {} : { mode }),
                });
                session._dispatchEvent({
                    type: "session.idle",
                    id: "idle-1",
                    timestamp: new Date().toISOString(),
                    parentId: null,
                    ephemeral: true,
                    data: {},
                });
                return { messageId: "message-1" };
            });
            await expect(
                session.sendAndWait({ prompt: "context updated", source, mode }, 1000)
            ).resolves.toBeUndefined();
        }
    );
});

describe.each(["system", "agent-sender-id"] as const)("%s message errors", (source) => {
    it("propagates an RPC failure from sendAndWait", async () => {
        const { session, server } = sessionPair();
        server.onRequest("session.send", () => new ResponseError(-32603, "send failed"));
        await expect(
            session.sendAndWait({ prompt: "context updated", source }, 1000)
        ).rejects.toThrow("send failed");
    });

    it("propagates a session error from sendAndWait", async () => {
        const { session, server } = sessionPair();
        server.onRequest("session.send", () => {
            const event: SessionEvent = {
                type: "session.error",
                id: "error-1",
                timestamp: new Date().toISOString(),
                parentId: null,
                data: { errorType: "notification", message: "agent failed" },
            };
            session._dispatchEvent(event);
            return { messageId: "message-1" };
        });
        await expect(
            session.sendAndWait({ prompt: "context updated", source }, 1000)
        ).rejects.toThrow("agent failed");
    });
});
