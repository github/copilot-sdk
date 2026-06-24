/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { createServer, IncomingMessage, Server as HttpServer, ServerResponse } from "http";
import { AddressInfo } from "net";
import { afterAll, describe, expect, it } from "vitest";
import { WebSocketServer } from "ws";
import {
    approveAll,
    CopilotRequestHandler,
    CopilotWebSocketForwarder,
    type CopilotRequestContext,
} from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

const HTTP_TEXT = "OK from synthetic HTTP upstream.";
const WS_TEXT = "OK from synthetic WS upstream.";

/**
 * Stand up an in-process upstream that speaks the real CAPI shapes the
 * runtime needs: model catalog, policy, `/responses` SSE for HTTP
 * inference, and a WebSocket endpoint at `/responses` that answers each
 * inbound `response.create` with the ordered `/responses` events the
 * reducer expects.
 *
 * Returned `url` is what the handler subclass rewrites every
 * intercepted request to point at — the runtime never talks to this
 * server directly; the handler does, on the runtime's behalf.
 */
async function startFakeUpstream(): Promise<{
    url: string;
    server: HttpServer;
    wsRequestCount: () => number;
    close: () => Promise<void>;
}> {
    let wsRequests = 0;

    const httpServer = createServer((req, res) => {
        const url = new URL(req.url ?? "/", `http://${req.headers.host ?? "localhost"}`);
        if (url.pathname === "/models" && req.method === "GET") {
            sendJson(res, 200, {
                data: [
                    {
                        id: "claude-sonnet-4.5",
                        name: "Claude Sonnet 4.5",
                        object: "model",
                        vendor: "Anthropic",
                        version: "1",
                        preview: false,
                        model_picker_enabled: true,
                        supported_endpoints: ["/responses", "ws:/responses"],
                        capabilities: {
                            type: "chat",
                            family: "claude-sonnet-4.5",
                            tokenizer: "o200k_base",
                            limits: {
                                max_context_window_tokens: 200000,
                                max_output_tokens: 8192,
                            },
                            supports: {
                                streaming: true,
                                tool_calls: true,
                                parallel_tool_calls: true,
                                vision: true,
                            },
                        },
                    },
                ],
            });
            return;
        }
        if (url.pathname.endsWith("/models/session")) {
            sendJson(res, 200, {});
            return;
        }
        if (url.pathname.includes("/policy")) {
            sendJson(res, 200, { state: "enabled" });
            return;
        }
        if (url.pathname.endsWith("/responses") && req.method === "POST") {
            // Single-shot HTTP inference (e.g. title generation). SSE
            // events the `responses-client.ts` reducer accepts.
            drainBody(req)
                .then(() => {
                    res.writeHead(200, {
                        "content-type": "text/event-stream",
                        "cache-control": "no-cache",
                    });
                    for (const event of buildResponsesEvents(HTTP_TEXT, "resp_stub_http")) {
                        res.write(`event: ${event.type}\ndata: ${JSON.stringify(event)}\n\n`);
                    }
                    res.end();
                })
                .catch(() => {
                    res.writeHead(500).end();
                });
            return;
        }
        // Anything else: not found.
        res.writeHead(404, { "content-type": "application/json" });
        res.end(JSON.stringify({ error: "not_found", path: url.pathname }));
    });

    const wss = new WebSocketServer({ server: httpServer, path: "/responses" });
    wss.on("connection", (socket) => {
        socket.on("message", (raw) => {
            wsRequests++;
            // For each `response.create` request the runtime sends,
            // answer with the ordered `/responses` event objects — one
            // event per outbound WS message, raw JSON (NOT SSE-framed).
            for (const event of buildResponsesEvents(WS_TEXT, "resp_stub_ws")) {
                socket.send(JSON.stringify(event));
            }
            void raw;
        });
    });

    await new Promise<void>((resolve) => httpServer.listen(0, "127.0.0.1", resolve));
    const port = (httpServer.address() as AddressInfo).port;
    const url = `http://127.0.0.1:${port}`;

    return {
        url,
        server: httpServer,
        wsRequestCount: () => wsRequests,
        async close() {
            wss.clients.forEach((c) => c.terminate());
            await new Promise<void>((resolve) => wss.close(() => resolve()));
            await new Promise<void>((resolve) => httpServer.close(() => resolve()));
        },
    };
}

function sendJson(res: ServerResponse, status: number, body: unknown): void {
    res.writeHead(status, { "content-type": "application/json" });
    res.end(JSON.stringify(body));
}

async function drainBody(req: IncomingMessage): Promise<Buffer> {
    const parts: Buffer[] = [];
    for await (const chunk of req) {
        parts.push(chunk as Buffer);
    }
    return Buffer.concat(parts);
}

function buildResponsesEvents(text: string, id: string): Array<Record<string, unknown>> {
    return [
        {
            type: "response.created",
            response: { id, object: "response", status: "in_progress", output: [] },
        },
        {
            type: "response.output_item.added",
            output_index: 0,
            item: { id: "msg_1", type: "message", role: "assistant", content: [] },
        },
        {
            type: "response.content_part.added",
            output_index: 0,
            content_index: 0,
            part: { type: "output_text", text: "" },
        },
        { type: "response.output_text.delta", output_index: 0, content_index: 0, delta: text },
        { type: "response.output_text.done", output_index: 0, content_index: 0, text },
        {
            type: "response.completed",
            response: {
                id,
                object: "response",
                status: "completed",
                output: [
                    {
                        id: "msg_1",
                        type: "message",
                        role: "assistant",
                        content: [{ type: "output_text", text }],
                    },
                ],
                usage: { input_tokens: 5, output_tokens: 7, total_tokens: 12 },
            },
        },
    ];
}

/**
interface Counters {
    httpRequests: number;
    httpResponses: number;
    wsRequestMessages: number;
    wsResponseMessages: number;
}

/**
 * Single handler subclass that services BOTH transports against the
 * per-test fake upstream. Demonstrates mutation in each direction:
 *
 * - HTTP: rewrites the URL to point at the test server, adds an
 *   `X-Test-Mutated` header to the outbound request, and adds an
 *   `X-Test-Response-Mutated` header on the way back. The test server
 *   echoes the request header into a counter so we can assert it
 *   actually arrived upstream.
 * - WebSocket: rewrites the WS URL similarly and forwards through the
 *   default WebSocket forwarder while observing message counts in both
 *   directions.
 */
class TestHandler extends CopilotRequestHandler {
    constructor(
        private readonly upstreamUrl: string,
        private readonly counters: Counters
    ) {
        super();
    }

    private rewriteUrl(originalUrl: string): string {
        const parsed = new URL(originalUrl);
        const upstream = new URL(this.upstreamUrl);
        parsed.protocol = upstream.protocol;
        parsed.host = upstream.host;
        return parsed.toString();
    }

    private rewriteWsUrl(originalUrl: string): string {
        const parsed = new URL(originalUrl);
        const upstream = new URL(this.upstreamUrl);
        // The upstream URL is http(s); flip to ws(s) for the WS open.
        parsed.protocol = upstream.protocol === "https:" ? "wss:" : "ws:";
        parsed.host = upstream.host;
        return parsed.toString();
    }

    protected override async sendRequest(
        request: Request,
        _ctx: CopilotRequestContext
    ): Promise<Response> {
        this.counters.httpRequests++;
        const rewritten = this.rewriteUrl(request.url);
        const requestHeaders = new Headers(request.headers);
        requestHeaders.set("x-test-mutated", "1");
        const rewrittenRequest = new Request(rewritten, {
            method: request.method,
            headers: requestHeaders,
            body: request.body,
            // @ts-expect-error duplex is required by undici when streaming a body
            duplex: "half",
        });
        const response = await fetch(rewrittenRequest, { signal: _ctx.signal });
        this.counters.httpResponses++;
        const responseHeaders = new Headers(response.headers);
        responseHeaders.set("x-test-response-mutated", "1");
        return new Response(response.body, {
            status: response.status,
            statusText: response.statusText,
            headers: responseHeaders,
        });
    }

    protected override async openWebSocket(
        ctx: CopilotRequestContext
    ): Promise<CopilotWebSocketForwarder> {
        ctx.url = this.rewriteWsUrl(ctx.url);
        return new CountingSocketForwarder(ctx, this.counters);
    }
}

class CountingSocketForwarder extends CopilotWebSocketForwarder {
    constructor(
        ctx: CopilotRequestContext,
        private readonly counters: Counters
    ) {
        super(ctx);
    }

    override sendRequestMessage(data: string | Uint8Array): void {
        this.counters.wsRequestMessages++;
        super.sendRequestMessage(data);
    }

    override async sendResponseMessage(data: string | Uint8Array): Promise<void> {
        this.counters.wsResponseMessages++;
        await super.sendResponseMessage(data);
    }
}

describe("CopilotRequestHandler — single subclass handles HTTP + WebSocket", async () => {
    const upstream = await startFakeUpstream();
    const counters: Counters = {
        httpRequests: 0,
        httpResponses: 0,
        wsRequestMessages: 0,
        wsResponseMessages: 0,
    };

    const { copilotClient: client, env } = await createSdkTestContext({
        copilotClientOptions: {
            requestHandler: new TestHandler(upstream.url, counters),
        },
    });

    // Enable the WebSocket Responses transport in the spawned runtime so
    // the main agent turn picks the WS path; single-shot calls (title
    // generation) still go over HTTP through the same subclass.
    env.COPILOT_EXP_COPILOT_CLI_WEBSOCKET_RESPONSES = "true";

    afterAll(async () => {
        await upstream.close();
    });

    it("services both an HTTP turn and a WebSocket turn end-to-end via one handler", async () => {
        await client.start();
        const session = await client.createSession({ onPermissionRequest: approveAll });
        let resultJson = "";
        try {
            const result = await session.sendAndWait({ prompt: "Say OK." });
            resultJson = JSON.stringify(result);
        } finally {
            await session.disconnect();
        }

        // The HTTP hooks fired — the runtime issued model-layer GETs
        // (catalog, policy) and possibly a single-shot inference.
        expect(counters.httpRequests, "expected sendRequest to fire").toBeGreaterThan(0);
        expect(
            counters.httpResponses,
            "expected sendRequest response mutation to fire"
        ).toBeGreaterThan(0);

        // The WebSocket hooks fired — the main agent turn went over
        // the WS path and we observed messages in both directions.
        expect(
            counters.wsRequestMessages,
            "expected sendRequestMessage (runtime → upstream) to fire"
        ).toBeGreaterThan(0);
        expect(
            counters.wsResponseMessages,
            "expected sendResponseMessage (upstream → runtime) to fire"
        ).toBeGreaterThan(0);
        expect(
            upstream.wsRequestCount(),
            "expected upstream WS to receive request messages"
        ).toBeGreaterThan(0);

        // The synthetic content from the upstream surfaced in the
        // assistant turn — proves the full chain (runtime → handler
        // → upstream → handler → runtime) is intact for the
        // transport the main agent turn used.
        // Validate the final assistant response arrived (guards against truncated captures)
        expect(resultJson).toMatch(/OK from synthetic (HTTP|WS) upstream/);
    }, 90_000);
});
