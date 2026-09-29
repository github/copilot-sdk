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
import type {
    ExtensionContextAttachment,
    MessageOptions,
    MessageSource,
    SessionEvent,
} from "../src/index.js";
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

describe("RPC admission correlation", () => {
    it.each([
        undefined,
        "01234567-89ab-4cde-8f01-23456789abcd",
        "01234567-89AB-4CDE-8F01-23456789ABCD",
        "not-a-uuid",
        "",
    ])(
        "forwards only explicitly supplied strings without changing other options: %s",
        async (clientCorrelationId) => {
            const trace = {
                traceparent: "00-11111111111111111111111111111111-2222222222222222-01",
                tracestate: "vendor=preserved",
            };
            const { session, server } = sessionPair(() => trace);
            const options = {
                prompt: "hello",
                source: "system",
                mode: "enqueue",
                displayPrompt: "display",
                requestHeaders: { "X-Test": "preserved" },
                clientCorrelationId,
            } satisfies MessageOptions;
            server.onRequest("session.send", (params: unknown) => {
                expect(params).toEqual(
                    JSON.parse(JSON.stringify({ sessionId: "session-1", ...trace, ...options }))
                );
                return { messageId: "canonical-1", futureField: true };
            });
            await expect(session.send(options)).resolves.toBe("canonical-1");
            await expect(session.rpc.send({ ...trace, ...options })).resolves.toMatchObject({
                messageId: "canonical-1",
            });
        }
    );

    it("omits null metadata at the untyped convenience boundary", async () => {
        const { session, server } = sessionPair();
        server.onRequest("session.send", (params: unknown) => {
            expect(params).toEqual({ sessionId: "session-1", prompt: "hello" });
            return { messageId: "canonical-1" };
        });
        await expect(
            session.send({
                prompt: "hello",
                clientCorrelationId: null,
            } as unknown as MessageOptions)
        ).resolves.toBe("canonical-1");
    });

    it("retains admission metadata through sendAndWait", async () => {
        const clientCorrelationId = "01234567-89ab-4cde-8f01-23456789abcd";
        const { session, server } = sessionPair();
        server.onRequest("session.send", (params: unknown) => {
            expect(params).toEqual({
                sessionId: "session-1",
                prompt: "hello",
                clientCorrelationId,
            });
            session._dispatchEvent({
                type: "session.idle",
                id: "idle-1",
                parentId: null,
                timestamp: "2026-09-24T18:00:00Z",
                ephemeral: true,
                data: {},
            });
            return { messageId: "canonical-1" };
        });
        await expect(
            session.sendAndWait({ prompt: "hello", clientCorrelationId }, 1000)
        ).resolves.toBeUndefined();
    });

    it("keeps concurrent and repeated values per input, not per batch", async () => {
        const first = "01234567-89ab-4cde-8f01-23456789abcd";
        const second = "abcdef01-2345-4678-9abc-def012345678";
        const { session, server } = sessionPair();
        const messages = [
            { prompt: "context", clientCorrelationId: second },
            { prompt: "plain" },
            { prompt: "reused", clientCorrelationId: first },
        ];
        let sends = 0;
        server.onRequest(
            "session.send",
            (params: { prompt: string; clientCorrelationId: string }) => {
                sends++;
                expect(params.clientCorrelationId).toBe(params.prompt === "first" ? first : second);
                return { messageId: `${params.prompt}-id` };
            }
        );
        server.onRequest("session.sendMessages", (params: unknown) => {
            expect(params).toEqual({ sessionId: "session-1", messages });
            return { messageIds: ["context-id", "plain-id", "reused-id"] };
        });
        await expect(
            Promise.all([
                session.send({ prompt: "first", clientCorrelationId: first }),
                session.send({ prompt: "second", clientCorrelationId: second }),
                session.rpc.sendMessages({ messages }),
            ])
        ).resolves.toEqual([
            "first-id",
            "second-id",
            { messageIds: ["context-id", "plain-id", "reused-id"] },
        ]);
        expect(sends).toBe(2);
    });
});

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
        const extensionContext = {
            type: "extension_context",
            capturedAt: "2026-09-18T20:00:00Z",
            extensionId: "scenario-extension",
            title: "Selected change",
            canvasId: "diff",
            instanceId: "diff-17",
            payload: { selection: "active" },
        } satisfies ExtensionContextAttachment;
        const options: MessageOptions = {
            prompt: "context updated",
            source,
            mode: "immediate",
            agentMode: "plan",
            attachments: [{ type: "blob", data: "aGk=", mimeType: "text/plain" }, extensionContext],
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
