/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { afterAll, describe, expect, it } from "vitest";
import { z } from "zod";
import { approveAll, defineTool, type SessionEvent } from "../../src/index.js";
import { withTestCleanup } from "../helpers/withTestCleanup.js";
import { createSdkTestContext, DEFAULT_GITHUB_TOKEN } from "./harness/sdkTestContext.js";
import {
    createCompletedResponse,
    startWebSocketResponsesServer,
} from "./harness/webSocketResponsesServer.js";

const TOOL_NAME = "ws_fallback_marker";
const TOOL_RESULT = "WS_FALLBACK_TOOL_RESULT";
const FINAL = "WS_FALLBACK_FINAL_ANSWER";
const usage = {
    input_tokens: 10,
    output_tokens: 5,
    total_tokens: 15,
    input_tokens_details: { cached_tokens: 0 },
    output_tokens_details: { reasoning_tokens: 0 },
};

describe("WebSocket to HTTP fallback telemetry", async () => {
    const upstream = await startWebSocketResponsesServer();
    // Register first so the SDK context stops its client and proxy before this provider.
    afterAll(() => upstream.stop());
    const { copilotClient: client, openAiEndpoint } = await createSdkTestContext({
        useStdio: true,
        copilotClientOptions: {
            gitHubToken: DEFAULT_GITHUB_TOKEN,
            env: {
                COPILOT_API_URL: upstream.baseUrl,
                COPILOT_CLI_DISABLE_WEBSOCKET_RESPONSES: "false",
            },
        },
    });
    upstream.forwardHttpTo(openAiEndpoint.url);

    it("describes the WebSocket failure on the downgrading call and keeps it sticky", async () => {
        const { state } = upstream;
        state.responses.push({
            type: "error",
            error: {
                code: "service_unavailable",
                type: "websocket_error",
                message: "controlled WebSocket outage",
            },
        });
        state.httpResponses.push(
            [
                {
                    type: "response.completed",
                    response: {
                        ...createCompletedResponse("", "resp-http-tool"),
                        output: [
                            {
                                type: "function_call",
                                id: "fc-1",
                                call_id: "call-1",
                                name: TOOL_NAME,
                                arguments: JSON.stringify({ input: "ping" }),
                                status: "completed",
                            },
                        ],
                        output_text: "",
                        usage,
                    },
                },
            ],
            [
                {
                    type: "response.completed",
                    response: { ...createCompletedResponse(FINAL, "resp-http-final"), usage },
                },
            ]
        );
        const session = await client.createSession({
            model: "gpt-5-responses",
            onPermissionRequest: approveAll,
            tools: [
                defineTool(TOOL_NAME, {
                    description: "Returns a fixed marker",
                    parameters: z.object({ input: z.string() }),
                    handler: () => TOOL_RESULT,
                }),
            ],
        });
        const events: SessionEvent[] = [];
        const unsubscribe = session.on((event) => events.push(event));
        await withTestCleanup(
            async () => {
                const answer = await session.sendAndWait({ prompt: "Use the marker tool." });
                // Validate the final assistant response arrived (guards against truncated captures)
                expect(answer?.data.content).toBe(FINAL);

                expect(state.messages).toHaveLength(1);
                expect(state.httpResponseRequestPaths).toHaveLength(2);
                expect(state.httpResponses).toHaveLength(0);
                expect(
                    events.some(
                        (event) =>
                            event.type === "tool.execution_complete" &&
                            JSON.stringify(event.data).includes(TOOL_RESULT)
                    )
                ).toBe(true);

                const usages = events.flatMap((event) =>
                    event.type === "assistant.usage" ? [event.data] : []
                );
                expect(usages).toHaveLength(2);
                const descriptors = {
                    websocketFallbackReason: "api_error",
                    websocketFallbackErrorKind: "http_status",
                    websocketFallbackStatusCode: 503,
                    websocketFallbackReconnectAttempted: false,
                };
                expect(usages[0]).toMatchObject({
                    ...descriptors,
                    websocketFallbackStartedThisCall: true,
                });
                expect(usages[0].websocketFallbackCloseCode).toBeUndefined();
                expect(usages[1]).toMatchObject({
                    ...descriptors,
                    websocketFallbackStartedThisCall: false,
                });
                expect(
                    usages.every((data) => typeof data.websocketFallbackAfterMs === "number")
                ).toBe(true);
            },
            unsubscribe,
            () => session.disconnect(),
            async () => {
                const errors = await client.stop();
                if (errors.length > 0) {
                    throw new AggregateError(errors, "WebSocket fallback client cleanup failed");
                }
            }
        );
    });
});
