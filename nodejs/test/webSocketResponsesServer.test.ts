/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import http from "node:http";
import { describe, expect, it } from "vitest";
import { WebSocket } from "ws";
import { startWebSocketResponsesServer } from "./e2e/harness/webSocketResponsesServer.js";
import { withTestCleanup } from "./helpers/withTestCleanup.js";

async function listen(server: http.Server) {
    await new Promise<void>((resolve, reject) => {
        server.once("error", reject);
        server.listen(0, "127.0.0.1", resolve);
    });
    const address = server.address();
    if (address === null || typeof address === "string") {
        throw new Error("Expected a bound TCP address for the test upstream.");
    }
    return `http://127.0.0.1:${address.port}`;
}

async function close(server: http.Server) {
    if (!server.listening) return;
    server.closeAllConnections();
    await new Promise<void>((resolve, reject) =>
        server.close((error) => (error ? reject(error) : resolve()))
    );
}

describe("WebSocket provider HTTP forwarding", () => {
    it.each([false, true])(
        "closes the upstream with response headers sent: %s",
        async (sendHeaders) => {
            const accepted = Promise.withResolvers<void>();
            const receivedBody = Promise.withResolvers<void>();
            const openSockets = new Set<http.IncomingMessage["socket"]>();
            const configured = http.createServer((req, res) => {
                openSockets.add(req.socket);
                req.socket.once("close", () => openSockets.delete(req.socket));
                accepted.resolve();
                if (sendHeaders) res.write("held upstream body");
            });
            let provider: Awaited<ReturnType<typeof startWebSocketResponsesServer>> | undefined;
            let request: http.ClientRequest | undefined;
            await withTestCleanup(
                async () => {
                    const configuredUrl = await listen(configured);
                    provider = await startWebSocketResponsesServer();
                    provider.forwardHttpTo(configuredUrl);
                    request = http.get(`${provider.baseUrl}/held`, (response) => {
                        response.on("error", () => response.destroy());
                        response.once("data", () => receivedBody.resolve());
                        response.resume();
                    });
                    request.on("error", () => request?.destroy());
                    await accepted.promise;
                    if (sendHeaders) await receivedBody.promise;
                    const stopping = provider.stop();
                    provider = undefined;
                    await stopping;
                    await expect.poll(() => openSockets.size).toBe(0);
                },
                () => {
                    request?.destroy();
                },
                () => provider?.stop(),
                () => close(configured)
            );
        }
    );

    it("reports an upstream response reset and closes the downstream", async () => {
        const receivedBody = Promise.withResolvers<void>();
        const downstreamClosed = Promise.withResolvers<void>();
        let upstreamSocket: http.IncomingMessage["socket"] | undefined;
        const configured = http.createServer((req, res) => {
            upstreamSocket = req.socket;
            res.write("partial body");
        });
        let provider: Awaited<ReturnType<typeof startWebSocketResponsesServer>> | undefined;
        let request: http.ClientRequest | undefined;
        await withTestCleanup(
            async () => {
                const configuredUrl = await listen(configured);
                provider = await startWebSocketResponsesServer();
                provider.forwardHttpTo(configuredUrl);
                request = http.get(`${provider.baseUrl}/reset`, (response) => {
                    response.on("error", () => response.destroy());
                    response.once("data", () => receivedBody.resolve());
                    response.once("close", () => downstreamClosed.resolve());
                    response.resume();
                });
                request.on("error", () => request?.destroy());
                await receivedBody.promise;
                upstreamSocket?.destroy();
                await downstreamClosed.promise;
                const stopping = provider.stop();
                provider = undefined;
                await expect(stopping).rejects.toMatchObject({
                    message: "WebSocket provider fixture failed.",
                    errors: [expect.objectContaining({ code: "ECONNRESET" })],
                });
            },
            () => {
                request?.destroy();
            },
            () => provider?.stop(),
            () => close(configured)
        );
    });

    it("keeps the destination fixed while forwarding request paths, methods, and bodies", async () => {
        const configured = http.createServer((req, res) => {
            req.setEncoding("utf8");
            let body = "";
            req.on("data", (chunk: string) => (body += chunk));
            req.on("end", () => {
                res.writeHead(200, {
                    "content-type": "application/json; charset=utf-8",
                    "x-content-type-options": "nosniff",
                });
                res.end(
                    JSON.stringify({
                        server: "configured",
                        path: req.url,
                        method: req.method,
                        host: req.headers.host,
                        body,
                    })
                );
            });
        });
        let alternateRequests = 0;
        const alternate = http.createServer((_req, res) => {
            alternateRequests++;
            res.end(JSON.stringify({ server: "alternate" }));
        });
        let provider: Awaited<ReturnType<typeof startWebSocketResponsesServer>> | undefined;
        await withTestCleanup(
            async () => {
                const configuredUrl = await listen(configured);
                const alternateUrl = await listen(alternate);
                const upstream = await startWebSocketResponsesServer();
                provider = upstream;
                upstream.forwardHttpTo(configuredUrl);
                const paths = [
                    "/copilot_internal/user?include_quota=true",
                    `${alternateUrl}/absolute-target`,
                    `//${new URL(alternateUrl).host}/network-target`,
                ];
                for (const path of paths) {
                    const body = JSON.stringify({
                        forwarded: path,
                        content: "<span>fixture input</span>",
                    });
                    const response = await new Promise<{
                        body: string;
                        headers: http.IncomingHttpHeaders;
                    }>((resolve, reject) => {
                        const request = http.request(
                            upstream.baseUrl,
                            {
                                path,
                                method: "POST",
                                headers: { "content-type": "application/json" },
                            },
                            (res) => {
                                res.setEncoding("utf8");
                                let text = "";
                                res.on("data", (chunk: string) => (text += chunk));
                                res.on("error", reject);
                                res.on("end", () => resolve({ body: text, headers: res.headers }));
                            }
                        );
                        request.on("error", reject);
                        request.end(body);
                    });
                    expect(response.headers["content-type"]).toBe(
                        "application/json; charset=utf-8"
                    );
                    expect(response.headers["x-content-type-options"]).toBe("nosniff");
                    expect(JSON.parse(response.body)).toEqual({
                        server: "configured",
                        path,
                        method: "POST",
                        host: new URL(configuredUrl).host,
                        body,
                    });
                }
                expect(alternateRequests).toBe(0);
            },
            () => provider?.stop(),
            () => close(configured),
            () => close(alternate)
        );
    });
});

describe("WebSocket provider message failures", () => {
    it.each(["malformed JSON", "synchronous reply failure"])(
        "reports %s through cleanup",
        async (failure) => {
            const provider = await startWebSocketResponsesServer();
            let stopped = false;
            const socket = new WebSocket(provider.baseUrl.replace("http:", "ws:"));
            await withTestCleanup(
                async () => {
                    await new Promise<void>((resolve, reject) => {
                        socket.once("open", resolve);
                        socket.once("error", reject);
                    });
                    const replyError = new Error("reply failed synchronously");
                    provider.state.responses.push(() => {
                        throw replyError;
                    });
                    socket.send(
                        failure === "malformed JSON"
                            ? "{"
                            : JSON.stringify({ type: "response.create" })
                    );
                    await expect.poll(() => socket.readyState).toBe(WebSocket.CLOSED);
                    const stopping = provider.stop();
                    stopped = true;
                    await expect(stopping).rejects.toMatchObject({
                        message: "WebSocket provider fixture failed.",
                        errors: [
                            failure === "malformed JSON" ? expect.any(SyntaxError) : replyError,
                        ],
                    });
                },
                () => socket.terminate(),
                async () => {
                    if (!stopped) await provider.stop();
                }
            );
        }
    );
});
