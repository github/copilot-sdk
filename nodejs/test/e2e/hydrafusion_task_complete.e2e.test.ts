/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it, onTestFinished } from "vitest";
import {
    approveAll,
    CopilotRequestHandler,
    type CopilotSession,
    type SessionEvent,
} from "../../src/index.js";
import { createSdkTestContext, DEFAULT_GITHUB_TOKEN } from "./harness/sdkTestContext.js";
import { isByokBackend } from "./harness/testBackend.js";

const MODEL = "gpt-5.6-sol";
const SUMMARY = "SDK_HYDRAFUSION_TASK_COMPLETE_SUMMARY_OK";
const TASK_COMPLETE_CALL_ID = "sdk-hydrafusion-task-complete";
const UNEXPECTED_CONTINUATION = "SDK_HYDRAFUSION_UNEXPECTED_CONTINUATION";
const FUSION_COMPLETED_TIMEOUT_MS = 60_000;

type ChatRequest = {
    model?: string;
    stream?: boolean;
    messages?: Array<{ role?: string }>;
};

/** Routes one Max Single turn whose solver answers only through `task_complete`. */
class SummaryOnlyTaskCompleteHandler extends CopilotRequestHandler {
    readonly inference: ChatRequest[] = [];

    protected override async sendRequest(request: Request): Promise<Response> {
        const path = new URL(request.url).pathname;
        if (path === "/models") {
            return Response.json({
                data: [
                    {
                        id: MODEL,
                        name: MODEL,
                        model_picker_enabled: true,
                        policy: { state: "enabled" },
                        supported_endpoints: ["/chat/completions"],
                        capabilities: {
                            supports: { streaming: true, tool_calls: true, vision: true },
                            limits: {
                                max_prompt_tokens: 128000,
                                max_output_tokens: 16384,
                                max_context_window_tokens: 144384,
                            },
                        },
                    },
                ],
            });
        }
        if (path === "/model/fusion") {
            return Response.json({
                fusion_mode: "hydrafusion-max",
                fusion_pattern: "solo",
                plan_version: "2",
                steps: [{ role: "generation", model_id: MODEL }],
                session: { token: "jwt.fake.sdk.task-complete.plan", expires_at: 0 },
            });
        }
        if (path === "/chat/completions") {
            const body = (await request.json()) as ChatRequest;
            this.inference.push(body);
            if (body.model !== MODEL) {
                throw new Error(`Expected concrete Fusion constituent, received ${body.model}`);
            }
            // task_complete is terminal in Autopilot, so a request carrying its
            // result would be an unexpected continuation; answer with text the
            // test then proves never reached the committed transcript.
            const continuation = body.messages?.some((message) => message.role === "tool");
            const message = continuation
                ? { role: "assistant", content: UNEXPECTED_CONTINUATION }
                : {
                      role: "assistant",
                      content: "",
                      tool_calls: [
                          {
                              id: TASK_COMPLETE_CALL_ID,
                              type: "function",
                              function: {
                                  name: "task_complete",
                                  arguments: JSON.stringify({ summary: SUMMARY }),
                              },
                          },
                      ],
                  };
            const finishReason = "tool_calls" in message ? "tool_calls" : "stop";
            const completion = {
                id: "chatcmpl-sdk-hydrafusion-task-complete",
                object: "chat.completion",
                created: 0,
                model: MODEL,
                choices: [{ index: 0, message, finish_reason: finishReason }],
                usage: { prompt_tokens: 11, completion_tokens: 7, total_tokens: 18 },
            };
            if (!body.stream) return Response.json(completion);
            return new Response(
                `data: ${JSON.stringify({
                    ...completion,
                    object: "chat.completion.chunk",
                    choices: [
                        {
                            index: 0,
                            delta: {
                                ...message,
                                ...("tool_calls" in message
                                    ? {
                                          tool_calls: message.tool_calls.map((call, index) => ({
                                              index,
                                              ...call,
                                          })),
                                      }
                                    : {}),
                            },
                            finish_reason: finishReason,
                        },
                    ],
                })}\n\ndata: [DONE]\n\n`,
                { headers: { "content-type": "text/event-stream" } }
            );
        }
        if (path === "/models/session/intent") {
            return Response.json({ error: "Intent unavailable in this fixture" }, { status: 404 });
        }
        if (path === "/models/session") {
            return Response.json({
                available_models: [MODEL],
                selected_model: MODEL,
                session_token: "jwt.fake.sdk.task-complete.routing",
                expires_at: 0,
            });
        }
        throw new Error(`Unexpected model request: ${request.method} ${path}`);
    }
}

function waitForFusionCompleted(session: CopilotSession): Promise<SessionEvent> {
    return new Promise<SessionEvent>((resolve, reject) => {
        let unsubscribe: () => void = () => {};
        const timer = setTimeout(() => {
            unsubscribe();
            reject(new Error("Timed out waiting for session.fusion_completed"));
        }, FUSION_COMPLETED_TIMEOUT_MS);
        unsubscribe = session.on((event) => {
            if (event.type === "session.fusion_completed") {
                clearTimeout(timer);
                unsubscribe();
                resolve(event);
            } else if (event.type === "session.error") {
                clearTimeout(timer);
                unsubscribe();
                reject(new Error(`${event.data.message}\n${event.data.stack ?? ""}`));
            }
        });
    });
}

function assistantContents(events: SessionEvent[]): string[] {
    return events.flatMap((event) =>
        event.type === "assistant.message" ? [event.data.content] : []
    );
}

describe.skipIf(isByokBackend)("HydraFusion summary-only task_complete", async () => {
    const handler = new SummaryOnlyTaskCompleteHandler();
    const { copilotClient: client, workDir } = await createSdkTestContext({
        copilotClientOptions: {
            gitHubToken: DEFAULT_GITHUB_TOKEN,
            requestHandler: handler,
            env: {
                HYDRAFUSION: "true",
                HYDRAFUSION_ROLLOUT: "true",
                HYDRAFUSION_PLAN_V2: "true",
                COPILOT_EXP_COPILOT_CLI_HYDRAFUSION_PLAN_V2: "true",
            },
        },
    });

    it("commits the summary only as the task completion, not as assistant text", async () => {
        const session = await client.createSession({
            model: "hydrafusion-max",
            enableExperimentalMode: true,
            workingDirectory: workDir,
            onPermissionRequest: approveAll,
        });
        const liveEvents: SessionEvent[] = [];
        const unsubscribe = session.on((event) => liveEvents.push(event));
        onTestFinished(async () => {
            unsubscribe();
            await session.disconnect();
        });

        await session.rpc.mode.set({ mode: "autopilot" });
        const fusionCompleted = waitForFusionCompleted(session);
        await session.send({
            prompt: "Call task_complete and give your answer only in its summary.",
        });
        await fusionCompleted;
        const persistedEvents = await session.getEvents();

        expect(handler.inference.length).toBeGreaterThan(0);
        for (const events of [liveEvents, persistedEvents]) {
            expect(events).toEqual(
                expect.arrayContaining([
                    expect.objectContaining({
                        type: "tool.execution_complete",
                        data: expect.objectContaining({
                            toolCallId: TASK_COMPLETE_CALL_ID,
                            success: true,
                            fusion: expect.objectContaining({ commitId: expect.any(String) }),
                        }),
                    }),
                    expect.objectContaining({
                        type: "session.task_complete",
                        data: expect.objectContaining({ success: true, summary: SUMMARY }),
                    }),
                ])
            );
            // The solver produced no assistant text, so the committed turn must
            // not republish the task_complete summary as an assistant message:
            // hosts already render it from the completion, and a copy shows twice.
            expect(assistantContents(events).filter((content) => content.trim())).toEqual([]);
        }
    });
});
