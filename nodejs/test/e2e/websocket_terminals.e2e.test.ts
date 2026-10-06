/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { afterAll, describe, expect, it } from "vitest";
import { approveAll, type SessionEvent } from "../../src/index.js";
import { withTestCleanup } from "../helpers/withTestCleanup.js";
import { createSdkTestContext, DEFAULT_GITHUB_TOKEN } from "./harness/sdkTestContext.js";
import {
    createCompletedResponse,
    startWebSocketResponsesServer,
    type WebSocketEnvelope,
} from "./harness/webSocketResponsesServer.js";

const WARMUP = "WS_TERMINAL_WARMUP_DONE";
const RECOVERED = "WS_TERMINAL_RECOVERY_DONE";
const QUEUED = "WS_TERMINAL_QUEUED_WORK_DONE";
const FAILURE = "Controlled terminal provider failure";
const REJECTED_OUTPUT = "THIS_FAILED_RESPONSE_IS_NOT_SUCCESS";
const shellToolName = process.platform === "win32" ? "powershell" : "bash";
const pollOptions = { timeout: 10_000 };

describe.each(["failed", "incomplete"] as const)(
    "WebSocket response.%s terminals",
    async (terminalType) => {
        const upstream = await startWebSocketResponsesServer();
        // Register first so the SDK context stops its client and proxy before this provider.
        afterAll(() => upstream.stop());
        const { copilotClient: client, openAiEndpoint } = await createSdkTestContext({
            // This regression exercises the supported subprocess SDK boundary.
            useStdio: true,
            copilotClientOptions: {
                gitHubToken: DEFAULT_GITHUB_TOKEN,
                env: {
                    COPILOT_API_URL: upstream.baseUrl,
                    COPILOT_CLI_DISABLE_WEBSOCKET_RESPONSES: "false",
                    WEBSOCKET_RESPONSES_PERSISTENT: "true",
                    COPILOT_EXP_COPILOT_CLI_WEBSOCKET_RESPONSES_PERSISTENT: "true",
                },
            },
        });
        upstream.forwardHttpTo(openAiEndpoint.url);

        it("retires a reused socket without peer EOF and resumes subsequent or queued work", async () => {
            const { state } = upstream;
            const terminal = Promise.withResolvers<WebSocketEnvelope>();
            state.responses.push(
                {
                    type: "response.completed",
                    response: createCompletedResponse(WARMUP, "resp-warmup"),
                },
                () => terminal.promise,
                {
                    type: "response.completed",
                    response: createCompletedResponse(RECOVERED, "resp-recovered"),
                }
            );
            if (terminalType === "incomplete") {
                state.responses.push({
                    type: "response.completed",
                    response: createCompletedResponse(QUEUED, "resp-queued"),
                });
            }
            const session = await client.createSession({
                model: "gpt-5-responses",
                onPermissionRequest: approveAll,
            });
            const events: SessionEvent[] = [];
            const unsubscribe = session.on((event) => events.push(event));
            await withTestCleanup(
                async () => {
                    const warmup = await session.sendAndWait({
                        prompt: "Return the warmup response.",
                    });
                    expect(warmup?.data.content).toBe(WARMUP);
                    await expect
                        .poll(
                            () => events.filter((event) => event.type === "session.idle").length,
                            pollOptions
                        )
                        .toBe(1);
                    const interruptedMessageId = await session.send({
                        prompt: "Exercise the controlled terminal response.",
                    });
                    let recoveryMessageId = interruptedMessageId;
                    let queuedMessageId: string | undefined;
                    const queuedPrompt = "Run the queued follow-up.";
                    await expect.poll(() => state.messages.length, pollOptions).toBe(2);
                    expect(state.messageConnectionIds).toEqual([1, 1]);
                    expect(state.messages[1].previous_response_id).toBe("resp-warmup");
                    expect(state.openConnectionIds.has(1)).toBe(true);
                    await session.rpc.mode.get();
                    if (terminalType === "incomplete") {
                        queuedMessageId = await session.send({
                            prompt: queuedPrompt,
                            mode: "enqueue",
                        });
                        expect(state.messages).toHaveLength(2);
                    }

                    // Release only the terminal envelope; the peer stays open until the runtime retires it.
                    terminal.resolve(
                        terminalType === "failed"
                            ? {
                                  type: "response.failed",
                                  response: {
                                      ...createCompletedResponse(REJECTED_OUTPUT, "resp-failed"),
                                      status: "failed",
                                      error: FAILURE,
                                      code: "invalid_prompt",
                                      param: "input",
                                      request_id: "terminal-request",
                                  },
                              }
                            : {
                                  type: "response.incomplete",
                                  response: {
                                      ...createCompletedResponse("", "resp-incomplete"),
                                      status: "incomplete",
                                      incomplete_details: { reason: "max_output_tokens" },
                                      output: [
                                          {
                                              type: "function_call",
                                              id: "fc-partial",
                                              call_id: "call-partial",
                                              name: shellToolName,
                                              arguments: JSON.stringify({
                                                  command: "echo WS_PARTIAL_TOOL_MUST_NOT_EXECUTE",
                                                  description: "Print a controlled test marker",
                                              }),
                                              status: "incomplete",
                                          },
                                      ],
                                  },
                              }
                    );

                    if (terminalType === "failed") {
                        await expect
                            .poll(
                                () =>
                                    events.some(
                                        (event) =>
                                            event.type === "session.error" &&
                                            event.data.message.includes(FAILURE)
                                    ),
                                pollOptions
                            )
                            .toBe(true);
                        await expect
                            .poll(
                                () =>
                                    events.filter((event) => event.type === "session.idle").length,
                                pollOptions
                            )
                            .toBe(2);
                        expect(state.messages).toHaveLength(2);
                        recoveryMessageId = await session.send({
                            prompt: "Recover after the provider error.",
                        });
                    }
                    // Validate the final assistant response arrived (guards against truncated captures)
                    if (terminalType === "incomplete") {
                        await expect
                            .poll(
                                () =>
                                    events
                                        .filter((event) => event.type === "assistant.message")
                                        .at(-1)?.data.content,
                                pollOptions
                            )
                            .toBe(QUEUED);
                    }
                    await expect
                        .poll(
                            () =>
                                events.some(
                                    (event) =>
                                        event.type === "assistant.message" &&
                                        event.data.content === RECOVERED &&
                                        event.data.originatingMessageId === recoveryMessageId
                                ),
                            pollOptions
                        )
                        .toBe(true);
                    await expect
                        .poll(() => {
                            const finalMessage = events.findLastIndex(
                                (event) =>
                                    event.type === "assistant.message" &&
                                    event.data.content ===
                                        (terminalType === "failed" ? RECOVERED : QUEUED)
                            );
                            return (
                                finalMessage !== -1 &&
                                events
                                    .slice(finalMessage + 1)
                                    .some((event) => event.type === "session.idle")
                            );
                        }, pollOptions)
                        .toBe(true);
                    expect(state.messageConnectionIds).toEqual(
                        terminalType === "failed" ? [1, 1, 2] : [1, 1, 2, 2]
                    );
                    await expect.poll(() => state.closedConnectionCount, pollOptions).toBe(1);
                    expect(state.openConnectionIds.has(1)).toBe(false);
                    expect(state.messages[2].previous_response_id).toBeUndefined();
                    expect(state.responses).toHaveLength(0);
                    expect(events.filter((event) => event.type === "tool.execution_start")).toEqual(
                        []
                    );
                    expect(
                        events.some(
                            (event) =>
                                event.type === "assistant.message" &&
                                event.data.content === REJECTED_OUTPUT
                        )
                    ).toBe(false);
                    expect(JSON.stringify(state.messages[2].input)).not.toContain(REJECTED_OUTPUT);
                    const recovered = events.find(
                        (event) =>
                            event.type === "assistant.message" && event.data.content === RECOVERED
                    );
                    expect(recovered).toMatchObject({
                        data: { originatingMessageId: recoveryMessageId },
                    });
                    if (terminalType === "failed") {
                        expect(
                            events.filter((event) => event.type === "model.call_failure")
                        ).toHaveLength(1);
                    } else {
                        expect(events.filter((event) => event.type === "session.error")).toEqual(
                            []
                        );
                        expect(JSON.stringify(state.messages[2].input)).not.toContain(
                            "call-partial"
                        );
                        expect(JSON.stringify(state.messages[2].input)).not.toContain(queuedPrompt);
                        expect(
                            JSON.stringify(state.messages[3].input).split(queuedPrompt)
                        ).toHaveLength(2);
                        expect(
                            events.find(
                                (event) =>
                                    event.type === "assistant.message" &&
                                    event.data.content === QUEUED
                            )
                        ).toMatchObject({ data: { originatingMessageId: queuedMessageId } });
                    }
                },
                unsubscribe,
                () => session.disconnect(),
                async () => {
                    const errors = await client.stop();
                    if (errors.length > 0) {
                        throw new AggregateError(errors, "WebSocket client cleanup failed");
                    }
                }
            );
        });
    }
);
