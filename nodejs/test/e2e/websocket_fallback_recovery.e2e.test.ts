/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import http from "node:http";
import { afterAll, describe, expect, it } from "vitest";
import { WebSocketServer } from "ws";
import { approveAll, defineTool, type SessionEvent } from "../../src/index.js";
import { withTestCleanup } from "../helpers/withTestCleanup.js";
import { createSdkTestContext, DEFAULT_GITHUB_TOKEN } from "./harness/sdkTestContext.js";

const FINAL_TEXT = "WS_FALLBACK_RECOVERY_DONE";
const TOOL_NAME = "fallback_recovery_step";

type Transport = "ws" | "http";
type ProviderRequest = { transport: Transport; connectionId?: number; body: ProviderBody };
type ProviderBody = {
    input?: Array<{ type?: string; call_id?: string; output?: unknown }>;
    previous_response_id?: string;
    stream?: boolean;
};

/**
 * Scripts one agent loop: two tool rounds, then a final answer. The reply depends
 * only on how many tool results the request carries, so either transport can
 * serve any step and the assertions observe which transport the runtime chose.
 */
function responseEvents(body: ProviderBody, id: string): Array<Record<string, unknown>> {
    const toolResults = (body.input ?? []).filter((item) => item.type === "function_call_output");
    const created = {
        type: "response.created",
        response: { id, object: "response", status: "in_progress", output: [] },
    };
    if (toolResults.length < 2) {
        const call = {
            type: "function_call",
            id: `fc-${toolResults.length + 1}`,
            call_id: `call-${toolResults.length + 1}`,
            name: TOOL_NAME,
            arguments: JSON.stringify({ step: toolResults.length + 1 }),
            status: "completed",
        };
        return [
            created,
            { type: "response.output_item.added", output_index: 0, item: call },
            { type: "response.output_item.done", output_index: 0, item: call },
            {
                type: "response.completed",
                response: { id, object: "response", status: "completed", output: [call] },
            },
        ];
    }
    const message = {
        id: "msg-final",
        type: "message",
        role: "assistant",
        content: [{ type: "output_text", text: FINAL_TEXT, annotations: [] }],
    };
    return [
        created,
        {
            type: "response.output_item.added",
            output_index: 0,
            item: { ...message, content: [] },
        },
        {
            type: "response.content_part.added",
            output_index: 0,
            content_index: 0,
            part: { type: "output_text", text: "" },
        },
        {
            type: "response.output_text.delta",
            output_index: 0,
            content_index: 0,
            delta: FINAL_TEXT,
        },
        { type: "response.output_text.done", output_index: 0, content_index: 0, text: FINAL_TEXT },
        { type: "response.output_item.done", output_index: 0, item: message },
        {
            type: "response.completed",
            response: { id, object: "response", status: "completed", output: [message] },
        },
    ];
}

/** An OpenAI-compatible provider whose first WebSocket request fails before any output. */
async function startProvider() {
    const requests: ProviderRequest[] = [];
    const errors: unknown[] = [];
    let connectionCount = 0;
    let wsRequestCount = 0;
    const server = http.createServer((req, res) => {
        void (async () => {
            if (req.method !== "POST" || req.url !== "/v1/responses") {
                throw new Error(`Unexpected provider request: ${req.method} ${req.url}`);
            }
            req.setEncoding("utf8");
            let raw = "";
            for await (const chunk of req) raw += chunk;
            const body = JSON.parse(raw) as ProviderBody;
            if (body.stream !== true) {
                // Session title generation is a one-shot request outside the agent loop.
                res.writeHead(200, { "content-type": "application/json" });
                res.end(
                    JSON.stringify({
                        id: "resp-title",
                        object: "response",
                        status: "completed",
                        output: [
                            {
                                id: "msg-title",
                                type: "message",
                                role: "assistant",
                                content: [{ type: "output_text", text: "Title", annotations: [] }],
                            },
                        ],
                    })
                );
                return;
            }
            requests.push({ transport: "http", body });
            res.writeHead(200, { "content-type": "text/event-stream" });
            for (const event of responseEvents(body, `resp-http-${requests.length}`)) {
                res.write(`data: ${JSON.stringify(event)}\n\n`);
            }
            res.end();
        })().catch((error: unknown) => {
            errors.push(error);
            res.destroy(error instanceof Error ? error : new Error(String(error)));
        });
    });
    const sockets = new WebSocketServer({ server, path: "/v1/responses" });
    sockets.on("connection", (socket) => {
        const connectionId = ++connectionCount;
        socket.on("error", (error) => errors.push(error));
        socket.on("message", (raw) => {
            const body = JSON.parse(raw.toString()) as ProviderBody;
            requests.push({ transport: "ws", connectionId, body });
            const events =
                ++wsRequestCount === 1
                    ? [
                          {
                              type: "error",
                              error: {
                                  code: "service_unavailable",
                                  type: "websocket_error",
                                  message: "Controlled transient WebSocket failure",
                              },
                          },
                      ]
                    : responseEvents(body, `resp-ws-${wsRequestCount}`);
            for (const event of events) socket.send(JSON.stringify(event));
        });
    });
    await new Promise<void>((resolve, reject) => {
        server.once("error", reject);
        server.listen(0, "127.0.0.1", resolve);
    });
    const address = server.address();
    if (address === null || typeof address === "string") {
        throw new Error("Expected a bound TCP address for the provider.");
    }
    return {
        baseUrl: `http://127.0.0.1:${address.port}/v1`,
        requests,
        connectionCount: () => connectionCount,
        async stop() {
            for (const socket of sockets.clients) socket.terminate();
            await new Promise<void>((resolve) => sockets.close(() => resolve()));
            server.closeAllConnections();
            await new Promise<void>((resolve, reject) =>
                server.close((error) => (error ? reject(error) : resolve()))
            );
            if (errors.length > 0) throw new AggregateError(errors, "Provider fixture failed.");
        },
    };
}

// The dispatcher's fallback state lives for one run (one user message's agent
// loop), so recovery is only observable across the model calls of one send.
describe.each([
    { recovery: true, transports: ["ws", "http", "http", "ws"] },
    { recovery: false, transports: ["ws", "http", "http", "http"] },
])("WebSocket fallback recovery=$recovery", async ({ recovery, transports }) => {
    const provider = await startProvider();
    // Register first so the SDK context stops its client before this provider.
    afterAll(() => provider.stop());
    const flag = String(recovery);
    const { copilotClient: client } = await createSdkTestContext({
        useStdio: true,
        copilotClientOptions: {
            gitHubToken: DEFAULT_GITHUB_TOKEN,
            env: {
                COPILOT_CLI_DISABLE_WEBSOCKET_RESPONSES: "false",
                WEBSOCKET_FALLBACK_RECOVERY: flag,
                COPILOT_EXP_COPILOT_CLI_WEBSOCKET_FALLBACK_RECOVERY: flag,
            },
        },
    });

    it(`${recovery ? "returns to" : "stays off"} the socket after a transient failure within one run`, async () => {
        const toolCalls: number[] = [];
        const session = await client.createSession({
            model: "gpt-5-responses",
            streaming: true,
            provider: {
                type: "openai",
                baseUrl: provider.baseUrl,
                apiKey: "test-provider-key",
                wireApi: "responses",
                transport: "websockets",
            },
            tools: [
                defineTool<{ step: number }>(TOOL_NAME, {
                    description: "Records one step of the controlled agent loop",
                    parameters: {
                        type: "object",
                        properties: { step: { type: "number" } },
                        required: ["step"],
                    },
                    defer: "never",
                    handler: ({ step }) => {
                        toolCalls.push(step);
                        return `STEP_${step}_RESULT`;
                    },
                }),
            ],
            onPermissionRequest: approveAll,
        });
        const events: SessionEvent[] = [];
        const unsubscribe = session.on((event) => events.push(event));
        await withTestCleanup(
            async () => {
                const answer = await session.sendAndWait({ prompt: "Run the two steps." });
                // Validate the final assistant response arrived (guards against truncated captures)
                expect(answer?.data.content).toBe(FINAL_TEXT);

                const { requests } = provider;
                expect(requests.map((request) => request.transport)).toEqual(transports);
                // The failed socket request is replayed once over HTTP, then each
                // tool runs exactly once whichever transport carried its turn.
                expect(toolCalls).toEqual([1, 2]);
                expect(events.filter((event) => event.type === "session.error")).toEqual([]);
                const deltas = events.flatMap((event) =>
                    event.type === "assistant.message_delta" ? [event.data.deltaContent] : []
                );
                expect(deltas.join("")).toBe(FINAL_TEXT);

                const last = requests.at(-1)!;
                const results = (last.body.input ?? []).filter(
                    (item) => item.type === "function_call_output"
                );
                expect(results.map((item) => item.call_id)).toEqual(["call-1", "call-2"]);
                expect(JSON.stringify(results)).toContain("STEP_1_RESULT");
                expect(JSON.stringify(results)).toContain("STEP_2_RESULT");
                if (recovery) {
                    // A fresh socket carries the full history, not a continuation
                    // of the response it never produced.
                    expect(last.connectionId).toBe(2);
                    expect(last.body.previous_response_id).toBeUndefined();
                    expect(provider.connectionCount()).toBe(2);
                } else {
                    expect(provider.connectionCount()).toBe(1);
                }
            },
            unsubscribe,
            () => session.disconnect(),
            async () => {
                const errors = await client.stop();
                if (errors.length > 0) {
                    throw new AggregateError(errors, "Client cleanup failed");
                }
            }
        );
    });
});
