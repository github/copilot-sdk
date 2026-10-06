/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import http from "node:http";
import { WebSocketServer } from "ws";

export type WebSocketEnvelope = Record<string, unknown>;
type WebSocketReply = WebSocketEnvelope | (() => Promise<WebSocketEnvelope>);

/** Controlled provider terminals cannot be reproduced by ordinary successful-conversation replay. */
export async function startWebSocketResponsesServer() {
    const state = {
        messages: [] as Array<Record<string, unknown>>,
        messageConnectionIds: [] as number[],
        responses: [] as WebSocketReply[],
        openConnectionIds: new Set<number>(),
        connectionCount: 0,
        closedConnectionCount: 0,
    };
    const errors: unknown[] = [];
    const forwardedRequests = new Set<http.ClientRequest>();
    const forwardedResponses = new Set<http.IncomingMessage>();
    const forwardingAgent = new http.Agent();
    let stopping = false;
    let httpTargetUrl: string | undefined;
    const server = http.createServer((req, res) => {
        if (stopping) {
            res.destroy();
            return;
        }
        if (req.url === "/models") {
            res.writeHead(200, { "content-type": "application/json" });
            res.end(
                JSON.stringify({
                    data: [
                        {
                            id: "gpt-5-responses",
                            name: "GPT-5 Responses",
                            supported_endpoints: ["/responses", "ws:/responses"],
                            capabilities: {
                                supports: { streaming: true, tool_calls: true },
                                limits: {
                                    max_context_window_tokens: 128000,
                                    max_output_tokens: 16384,
                                },
                            },
                        },
                    ],
                })
            );
            return;
        }
        if (!httpTargetUrl) {
            res.writeHead(503);
            res.end("The test CAPI proxy has not been attached.");
            return;
        }
        // Absolute-form request paths must not replace the configured proxy origin.
        const target = new URL(httpTargetUrl);
        const upstream = http.request(
            {
                protocol: target.protocol,
                hostname: target.hostname,
                port: target.port,
                path: req.url ?? "/",
                method: req.method,
                headers: { ...req.headers, host: target.host },
                agent: forwardingAgent,
            },
            (response) => {
                forwardedResponses.add(response);
                response.once("close", () => forwardedResponses.delete(response));
                response.on("error", (error) => {
                    if (!stopping) errors.push(error);
                    res.destroy();
                });
                if (stopping) {
                    response.destroy();
                    return;
                }
                res.writeHead(response.statusCode ?? 500, response.headers);
                response.pipe(res);
            }
        );
        forwardedRequests.add(upstream);
        upstream.once("close", () => forwardedRequests.delete(upstream));
        upstream.on("error", (error) => {
            if (stopping) return;
            errors.push(error);
            if (!res.headersSent) res.writeHead(502);
            res.end(String(error));
        });
        req.pipe(upstream);
    });
    const sockets = new WebSocketServer({ server });
    sockets.on("connection", (socket) => {
        const connectionId = ++state.connectionCount;
        state.openConnectionIds.add(connectionId);
        socket.on("close", () => {
            state.closedConnectionCount++;
            state.openConnectionIds.delete(connectionId);
        });
        socket.on("error", (error) => errors.push(error));
        socket.on("message", (raw) => {
            void (async () => {
                state.messages.push(JSON.parse(raw.toString()) as Record<string, unknown>);
                state.messageConnectionIds.push(connectionId);
                const reply = state.responses.shift();
                if (!reply) {
                    throw new Error("Unexpected WebSocket response.create request.");
                }
                const envelope = typeof reply === "function" ? await reply() : reply;
                if (stopping) return;
                await new Promise<void>((resolve, reject) => {
                    socket.send(JSON.stringify(envelope), (error) =>
                        error ? reject(error) : resolve()
                    );
                });
            })().catch((error: unknown) => {
                errors.push(error);
                socket.terminate();
            });
        });
    });
    await new Promise<void>((resolve, reject) => {
        server.once("error", reject);
        server.listen(0, "127.0.0.1", resolve);
    });
    const address = server.address();
    if (address === null || typeof address === "string") {
        throw new Error("Expected a bound TCP address for the WebSocket provider.");
    }
    return {
        baseUrl: `http://127.0.0.1:${address.port}`,
        state,
        forwardHttpTo(url: string) {
            httpTargetUrl = url;
        },
        async stop() {
            stopping = true;
            const forwarded = [...forwardedRequests, ...forwardedResponses];
            const forwardedClosed = Promise.all(
                forwarded.map(
                    (stream) => new Promise<void>((resolve) => stream.once("close", resolve))
                )
            );
            for (const stream of forwarded) stream.destroy();
            forwardingAgent.destroy();
            for (const socket of sockets.clients) socket.terminate();
            await new Promise<void>((resolve, reject) =>
                sockets.close((error) => (error ? reject(error) : resolve()))
            );
            server.closeAllConnections();
            await new Promise<void>((resolve, reject) =>
                server.close((error) => (error ? reject(error) : resolve()))
            );
            await forwardedClosed;
            if (errors.length > 0)
                throw new AggregateError(errors, "WebSocket provider fixture failed.");
        },
    };
}

export function createCompletedResponse(text: string, id: string) {
    return {
        id,
        object: "response",
        model: "gpt-5-responses",
        status: "completed",
        output: [
            {
                type: "message",
                id: "msg-1",
                role: "assistant",
                content: [{ type: "output_text", text, annotations: [] }],
                status: "completed",
            },
        ],
        output_text: text,
        usage: null,
    };
}
