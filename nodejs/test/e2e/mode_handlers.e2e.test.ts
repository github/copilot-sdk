/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { createServer } from "node:http";
import { text } from "node:stream/consumers";
import { describe, expect, it, onTestFinished } from "vitest";
import type {
    AutoModeSwitchRequest,
    CopilotSession,
    ExitPlanModeRequest,
    ExitPlanModeResult,
    NamedProviderConfig,
    ProviderModelConfig,
    SessionEvent,
} from "../../src/index.js";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { waitForCondition } from "./harness/sdkTestHelper.js";

const EVENT_TIMEOUT_MS = 30_000;
const MODE_HANDLER_TOKEN = "mode-handler-token";
const PLAN_SUMMARY = "Greeting file implementation plan";
const PLAN_PROMPT =
    "Create a brief implementation plan for adding a greeting.txt file, then request approval with exit_plan_mode.";
const AUTO_MODE_PROMPT =
    "Explain that auto mode recovered from a rate limit in one short sentence.";
const PLAN_OVERRIDE_PROMPT =
    "Write a plan, then call task_complete with summary PLAN_OVERRIDE_TOOL_CALLED.";
const INTERACTIVE_AFTER_PLAN_PROMPT = "Reply with exactly INTERACTIVE_AFTER_PLAN_OVERRIDE.";
const AUTOPILOT_READY_PROMPT = "Reply with exactly AUTOPILOT_MODE_READY.";
const AUTOPILOT_TO_INTERACTIVE_PROMPT =
    "Call task_complete after the test switches this in-flight turn from Autopilot to Interactive.";
const INTERACTIVE_TO_AUTOPILOT_PROMPT =
    "Call task_complete after the test switches this in-flight turn from Interactive to Autopilot.";

function waitForEvent<T extends SessionEvent>(
    session: CopilotSession,
    predicate: (event: SessionEvent) => event is T,
    description: string,
    timeoutMs = EVENT_TIMEOUT_MS,
    allowRateLimitError = false
): Promise<T> {
    return new Promise<T>((resolve, reject) => {
        let unsubscribe: () => void = () => {};
        const timer = setTimeout(() => {
            unsubscribe();
            reject(new Error(`Timed out waiting for ${description}`));
        }, timeoutMs);

        unsubscribe = session.on((event) => {
            if (predicate(event)) {
                clearTimeout(timer);
                unsubscribe();
                resolve(event);
            } else if (
                event.type === "session.error" &&
                !(allowRateLimitError && event.data.errorType === "rate_limit")
            ) {
                clearTimeout(timer);
                unsubscribe();
                reject(new Error(`${event.data.message}\n${event.data.stack ?? ""}`));
            }
        });
    });
}

describe("Mode handlers", async () => {
    const { copilotClient: client, openAiEndpoint, env, workDir } = await createSdkTestContext();

    env.COPILOT_DEBUG_GITHUB_API_URL = env.COPILOT_API_URL;
    await openAiEndpoint.setCopilotUserByToken(MODE_HANDLER_TOKEN, {
        login: "mode-handler-user",
        copilot_plan: "individual_pro",
        endpoints: {
            api: env.COPILOT_API_URL,
            telemetry: "https://localhost:1/telemetry",
        },
        analytics_tracking_id: "mode-handler-tracking-id",
    });

    it("should invoke exit plan mode handler when model uses tool", async () => {
        const exitPlanModeRequests: ExitPlanModeRequest[] = [];
        let session: CopilotSession | undefined;

        session = await client.createSession({
            gitHubToken: MODE_HANDLER_TOKEN,
            onPermissionRequest: approveAll,
            onExitPlanModeRequest: async (request, invocation): Promise<ExitPlanModeResult> => {
                exitPlanModeRequests.push(request);
                expect(invocation.sessionId).toBe(session?.sessionId);

                return {
                    approved: true,
                    selectedAction: "interactive",
                    feedback: "Approved by the TypeScript E2E test",
                };
            },
        });

        try {
            const requestedEvent = waitForEvent(
                session,
                (event): event is Extract<SessionEvent, { type: "exit_plan_mode.requested" }> =>
                    event.type === "exit_plan_mode.requested" &&
                    event.data.summary === PLAN_SUMMARY,
                "exit_plan_mode.requested event"
            );
            const completedEvent = waitForEvent(
                session,
                (event): event is Extract<SessionEvent, { type: "exit_plan_mode.completed" }> =>
                    event.type === "exit_plan_mode.completed" &&
                    event.data.approved === true &&
                    event.data.selectedAction === "interactive",
                "exit_plan_mode.completed event"
            );

            const response = await session.sendAndWait({
                prompt: PLAN_PROMPT,
                agentMode: "plan",
            });

            expect(exitPlanModeRequests).toHaveLength(1);
            expect(exitPlanModeRequests[0]).toMatchObject({
                summary: PLAN_SUMMARY,
                actions: ["autopilot", "interactive", "exit_only"],
                recommendedAction: "interactive",
            });
            expect(exitPlanModeRequests[0].planContent).toBeDefined();

            expect((await requestedEvent).data.summary).toBe(PLAN_SUMMARY);
            const completed = await completedEvent;
            expect(completed.data.approved).toBe(true);
            expect(completed.data.selectedAction).toBe("interactive");
            expect(completed.data.feedback).toBe("Approved by the TypeScript E2E test");
            expect(response).toBeDefined();
        } finally {
            await session.disconnect();
        }
    });

    it("handles Plan overrides and in-flight mode changes", { timeout: 180_000 }, async () => {
        const modelRequests: Array<{
            messages?: Array<{
                role?: string;
                content?: unknown;
                tool_calls?: Array<{ function?: { name?: string } }>;
            }>;
            tools?: Array<{ function?: { name?: string } }>;
        }> = [];
        const autopilotToInteractiveGate = Promise.withResolvers<void>();
        const interactiveToAutopilotGate = Promise.withResolvers<void>();
        let modelFailure: Error | undefined;
        const modelServer = createServer((request, response) => {
            void (async () => {
                const modelRequest = JSON.parse(await text(request)) as {
                    messages?: Array<{
                        role?: string;
                        content?: unknown;
                        tool_calls?: Array<{ function?: { name?: string } }>;
                    }>;
                    tools?: Array<{ function?: { name?: string } }>;
                };
                modelRequests.push(modelRequest);
                const messages = modelRequest.messages ?? [];
                const latestUserIndex = messages.findLastIndex(
                    (message) => message.role === "user"
                );
                const activeMessages = messages.slice(Math.max(0, latestUserIndex));
                const latestUserContent = JSON.stringify(messages[latestUserIndex]?.content ?? "");
                const calledTools =
                    activeMessages.flatMap(
                        (message) =>
                            message.tool_calls
                                ?.map((toolCall) => toolCall.function?.name)
                                .filter((name): name is string => name !== undefined) ?? []
                    ) ?? [];
                let message;
                if (latestUserContent.includes(INTERACTIVE_AFTER_PLAN_PROMPT)) {
                    message = { role: "assistant", content: "INTERACTIVE_AFTER_PLAN_OVERRIDE" };
                } else if (latestUserContent.includes(AUTOPILOT_READY_PROMPT)) {
                    if (calledTools.includes("task_complete")) {
                        message = {
                            role: "assistant",
                            content: "UNEXPECTED_AUTOPILOT_READY_CONTINUATION",
                        };
                    } else {
                        message = {
                            role: "assistant",
                            content: null,
                            tool_calls: [
                                {
                                    id: "autopilot-ready-task-complete",
                                    type: "function",
                                    function: {
                                        name: "task_complete",
                                        arguments: '{"summary":"AUTOPILOT_MODE_READY"}',
                                    },
                                },
                            ],
                        };
                    }
                } else if (latestUserContent.includes(AUTOPILOT_TO_INTERACTIVE_PROMPT)) {
                    if (calledTools.includes("task_complete")) {
                        message = {
                            role: "assistant",
                            content: "AUTOPILOT_TO_INTERACTIVE_CONTINUED",
                        };
                    } else {
                        await autopilotToInteractiveGate.promise;
                        message = {
                            role: "assistant",
                            content: null,
                            tool_calls: [
                                {
                                    id: "autopilot-to-interactive-task-complete",
                                    type: "function",
                                    function: {
                                        name: "task_complete",
                                        arguments: '{"summary":"MODE_SWITCHED_TO_INTERACTIVE"}',
                                    },
                                },
                            ],
                        };
                    }
                } else if (latestUserContent.includes(INTERACTIVE_TO_AUTOPILOT_PROMPT)) {
                    if (calledTools.includes("task_complete")) {
                        message = {
                            role: "assistant",
                            content: "UNEXPECTED_INTERACTIVE_TO_AUTOPILOT_CONTINUATION",
                        };
                    } else {
                        await interactiveToAutopilotGate.promise;
                        message = {
                            role: "assistant",
                            content: null,
                            tool_calls: [
                                {
                                    id: "interactive-to-autopilot-task-complete",
                                    type: "function",
                                    function: {
                                        name: "task_complete",
                                        arguments: '{"summary":"MODE_SWITCHED_TO_AUTOPILOT"}',
                                    },
                                },
                            ],
                        };
                    }
                } else if (calledTools.includes("task_complete")) {
                    message = { role: "assistant", content: "PLAN_OVERRIDE_CONTINUED" };
                } else {
                    message = {
                        role: "assistant",
                        content: null,
                        tool_calls: [
                            {
                                id: "plan-override-task-complete",
                                type: "function",
                                function: {
                                    name: "task_complete",
                                    arguments: '{"summary":"PLAN_OVERRIDE_TOOL_CALLED"}',
                                },
                            },
                        ],
                    };
                }
                response.writeHead(200, { "content-type": "application/json" });
                response.end(
                    JSON.stringify({
                        id: `plan-override-${modelRequests.length}`,
                        object: "chat.completion",
                        created: 0,
                        model: "test-model",
                        choices: [
                            {
                                index: 0,
                                message,
                                finish_reason: "tool_calls" in message ? "tool_calls" : "stop",
                            },
                        ],
                        usage: { prompt_tokens: 10, completion_tokens: 10, total_tokens: 20 },
                    })
                );
            })().catch((error: unknown) => {
                modelFailure = error instanceof Error ? error : new Error(String(error));
                response.writeHead(500).end();
            });
        });
        let session: CopilotSession | undefined;
        const sessions: CopilotSession[] = [];
        let serverStart: Promise<void> | undefined;
        onTestFinished(async () => {
            const errors: unknown[] = [];
            for (const activeSession of sessions.toReversed()) {
                try {
                    await activeSession.disconnect();
                } catch (error) {
                    errors.push(error);
                }
            }
            if (serverStart) {
                const [startup] = await Promise.allSettled([serverStart]);
                if (startup.status === "rejected") {
                    errors.push(startup.reason);
                }
            }
            modelServer.closeAllConnections();
            if (modelServer.listening) {
                try {
                    await new Promise<void>((resolve, reject) => {
                        modelServer.close((error) => (error ? reject(error) : resolve()));
                    });
                } catch (error) {
                    errors.push(error);
                }
            }
            if (errors.length) {
                throw new AggregateError(
                    errors,
                    "Per-message Plan override fixture cleanup failed"
                );
            }
        });
        serverStart = new Promise<void>((resolve, reject) => {
            modelServer.once("error", reject);
            modelServer.listen(0, "127.0.0.1", resolve);
        });
        await serverStart;
        const address = modelServer.address();
        if (!address || typeof address === "string") {
            throw new Error("Missing local model server address");
        }

        const providers: NamedProviderConfig[] = [
            {
                name: "local",
                type: "openai",
                baseUrl: `http://127.0.0.1:${address.port}`,
                apiKey: "test",
                wireApi: "completions",
            },
        ];
        const models: ProviderModelConfig[] = [
            { id: "model", provider: "local", modelId: "test-model", wireModel: "test-model" },
        ];
        session = await client.createSession({
            onPermissionRequest: approveAll,
            workingDirectory: workDir,
            providers,
            models,
            model: "local/model",
        });
        sessions.push(session);

        const planResponse = await session.sendAndWait({
            prompt: PLAN_OVERRIDE_PROMPT,
            agentMode: "plan",
        });
        const interactiveResponse = await session.sendAndWait({
            prompt: INTERACTIVE_AFTER_PLAN_PROMPT,
        });
        await waitForCondition(() => modelRequests.length >= 3, {
            timeoutMs: EVENT_TIMEOUT_MS,
            timeoutMessage:
                "The local model did not receive the Plan, continuation, and Interactive requests",
        });

        if (modelFailure) {
            throw modelFailure;
        }
        expect(modelRequests).toHaveLength(3);
        const firstSystemMessages = modelRequests[0].messages?.filter(
            (message) => message.role === "system"
        );
        const firstUserMessages = modelRequests[0].messages?.filter(
            (message) => message.role === "user"
        );
        expect(JSON.stringify(firstSystemMessages)).not.toContain("<plan_mode>");
        expect(JSON.stringify(firstUserMessages)).toContain("<mode_changed_notice>");
        expect(JSON.stringify(firstUserMessages)).toContain("<plan_mode>");
        expect(
            modelRequests[0].tools?.some((tool) => tool.function?.name === "task_complete")
        ).toBe(true);
        expect(JSON.stringify(modelRequests[1].messages)).toContain("Continue working");
        expect(JSON.stringify(modelRequests[2].messages)).toContain(
            "Plan mode is no longer active."
        );
        expect(planResponse?.data.content).toBe("PLAN_OVERRIDE_CONTINUED");
        expect(interactiveResponse?.data.content).toBe("INTERACTIVE_AFTER_PLAN_OVERRIDE");
        await session.disconnect();
        sessions.splice(sessions.indexOf(session), 1);

        const approvalSession = await client.createSession({
            onPermissionRequest: approveAll,
            workingDirectory: workDir,
            providers,
            models,
            model: "local/model",
        });
        sessions.push(approvalSession);
        const enteredAutopilot = waitForEvent(
            approvalSession,
            (event): event is Extract<SessionEvent, { type: "session.mode_changed" }> =>
                event.type === "session.mode_changed" && event.data.newMode === "autopilot",
            "session.mode_changed event for Autopilot warm-up"
        );
        await approvalSession.rpc.mode.set({ mode: "autopilot" });
        await enteredAutopilot;
        let autopilotReadyState: "pending" | "idle" | "error" = "pending";
        let autopilotReadyError: Error | undefined;
        const unsubscribeAutopilotReady = approvalSession.on((event) => {
            if (event.type === "session.idle") {
                autopilotReadyState = "idle";
            } else if (event.type === "session.error") {
                autopilotReadyState = "error";
                autopilotReadyError = new Error(`${event.data.message}\n${event.data.stack ?? ""}`);
            }
        });
        try {
            await approvalSession.send({ prompt: AUTOPILOT_READY_PROMPT });
            await waitForCondition(
                () => autopilotReadyState !== "pending" || modelRequests.length >= 5,
                {
                    timeoutMs: EVENT_TIMEOUT_MS,
                    timeoutMessage: "The Autopilot warm-up turn did not finish or continue",
                }
            );
        } finally {
            unsubscribeAutopilotReady();
        }
        if (autopilotReadyError) {
            throw autopilotReadyError;
        }
        expect(autopilotReadyState).toBe("idle");
        expect(modelRequests).toHaveLength(4);
        expect(JSON.stringify(modelRequests)).not.toContain(
            "UNEXPECTED_AUTOPILOT_READY_CONTINUATION"
        );

        let exitAutopilotState: "pending" | "idle" | "error" = "pending";
        let exitAutopilotError: Error | undefined;
        const unsubscribeExitAutopilot = approvalSession.on((event) => {
            if (event.type === "session.idle") {
                exitAutopilotState = "idle";
            } else if (event.type === "session.error") {
                exitAutopilotState = "error";
                exitAutopilotError = new Error(`${event.data.message}\n${event.data.stack ?? ""}`);
            }
        });
        try {
            await approvalSession.send({ prompt: AUTOPILOT_TO_INTERACTIVE_PROMPT });
            await waitForCondition(() => modelRequests.length >= 5, {
                timeoutMs: EVENT_TIMEOUT_MS,
                timeoutMessage: "The Autopilot request did not reach the local model",
            });
            const enteredInteractive = waitForEvent(
                approvalSession,
                (event): event is Extract<SessionEvent, { type: "session.mode_changed" }> =>
                    event.type === "session.mode_changed" && event.data.newMode === "interactive",
                "session.mode_changed event for Autopilot to Interactive"
            );
            await approvalSession.rpc.mode.set({ mode: "interactive" });
            await enteredInteractive;
            autopilotToInteractiveGate.resolve();
            await waitForCondition(
                () => exitAutopilotState !== "pending" || modelRequests.length >= 6,
                {
                    timeoutMs: EVENT_TIMEOUT_MS,
                    timeoutMessage: "The Autopilot-to-Interactive turn did not finish or continue",
                }
            );
            if (exitAutopilotState === "pending") {
                await waitForCondition(() => exitAutopilotState !== "pending", {
                    timeoutMs: EVENT_TIMEOUT_MS,
                    timeoutMessage: "The Autopilot-to-Interactive continuation did not reach idle",
                });
            }
        } finally {
            unsubscribeExitAutopilot();
        }
        if (exitAutopilotError) {
            throw exitAutopilotError;
        }
        expect(exitAutopilotState).toBe("idle");
        expect(modelRequests.length).toBeGreaterThanOrEqual(6);
        expect(
            modelRequests
                .slice(5)
                .some((request) => JSON.stringify(request.messages).includes("Continue working"))
        ).toBe(true);

        const beforeEnterAutopilot = modelRequests.length;
        let enterAutopilotState: "pending" | "idle" | "error" = "pending";
        let enterAutopilotError: Error | undefined;
        const unsubscribeEnterAutopilot = approvalSession.on((event) => {
            if (event.type === "session.idle") {
                enterAutopilotState = "idle";
            } else if (event.type === "session.error") {
                enterAutopilotState = "error";
                enterAutopilotError = new Error(`${event.data.message}\n${event.data.stack ?? ""}`);
            }
        });
        try {
            await approvalSession.send({ prompt: INTERACTIVE_TO_AUTOPILOT_PROMPT });
            await waitForCondition(() => modelRequests.length >= beforeEnterAutopilot + 1, {
                timeoutMs: EVENT_TIMEOUT_MS,
                timeoutMessage: "The Interactive request did not reach the local model",
            });
            const reenteredAutopilot = waitForEvent(
                approvalSession,
                (event): event is Extract<SessionEvent, { type: "session.mode_changed" }> =>
                    event.type === "session.mode_changed" && event.data.newMode === "autopilot",
                "session.mode_changed event for Interactive to Autopilot"
            );
            await approvalSession.rpc.mode.set({ mode: "autopilot" });
            await reenteredAutopilot;
            interactiveToAutopilotGate.resolve();
            await waitForCondition(
                () =>
                    enterAutopilotState !== "pending" ||
                    modelRequests.length >= beforeEnterAutopilot + 2,
                {
                    timeoutMs: EVENT_TIMEOUT_MS,
                    timeoutMessage: "The Interactive-to-Autopilot turn did not finish or continue",
                }
            );
        } finally {
            unsubscribeEnterAutopilot();
        }
        if (enterAutopilotError) {
            throw enterAutopilotError;
        }
        expect(enterAutopilotState).toBe("idle");
        expect(modelRequests).toHaveLength(beforeEnterAutopilot + 1);
        expect(JSON.stringify(modelRequests)).not.toContain(
            "UNEXPECTED_INTERACTIVE_TO_AUTOPILOT_CONTINUATION"
        );
    });

    it("should invoke auto mode switch handler when rate limited", async () => {
        const autoModeSwitchRequests: AutoModeSwitchRequest[] = [];
        let session: CopilotSession | undefined;

        session = await client.createSession({
            gitHubToken: MODE_HANDLER_TOKEN,
            onPermissionRequest: approveAll,
            onAutoModeSwitchRequest: (request, invocation) => {
                autoModeSwitchRequests.push(request);
                expect(invocation.sessionId).toBe(session?.sessionId);
                return "yes";
            },
        });

        try {
            const requestedEvent = waitForEvent(
                session,
                (event): event is Extract<SessionEvent, { type: "auto_mode_switch.requested" }> =>
                    event.type === "auto_mode_switch.requested" &&
                    event.data.errorCode === "user_weekly_rate_limited" &&
                    event.data.retryAfterSeconds === 1,
                "auto_mode_switch.requested event",
                EVENT_TIMEOUT_MS,
                true
            );
            const completedEvent = waitForEvent(
                session,
                (event): event is Extract<SessionEvent, { type: "auto_mode_switch.completed" }> =>
                    event.type === "auto_mode_switch.completed" && event.data.response === "yes",
                "auto_mode_switch.completed event",
                EVENT_TIMEOUT_MS,
                true
            );
            const modelChangeEvent = waitForEvent(
                session,
                (event): event is Extract<SessionEvent, { type: "session.model_change" }> =>
                    event.type === "session.model_change" &&
                    event.data.cause === "rate_limit_auto_switch",
                "rate-limit auto-mode model change",
                EVENT_TIMEOUT_MS,
                true
            );
            const idleEvent = waitForEvent(
                session,
                (event): event is Extract<SessionEvent, { type: "session.idle" }> =>
                    event.type === "session.idle",
                "session.idle after auto-mode switch",
                EVENT_TIMEOUT_MS,
                true
            );

            const messageId = await session.send({ prompt: AUTO_MODE_PROMPT });
            expect(messageId).toBeTruthy();

            expect((await requestedEvent).data.errorCode).toBe("user_weekly_rate_limited");
            const completed = await completedEvent;
            expect(completed.data.response).toBe("yes");
            expect((await modelChangeEvent).data.cause).toBe("rate_limit_auto_switch");
            await idleEvent;

            expect(autoModeSwitchRequests).toHaveLength(1);
            expect(autoModeSwitchRequests[0]).toMatchObject({
                errorCode: "user_weekly_rate_limited",
                retryAfterSeconds: 1,
            });
        } finally {
            await session.disconnect();
        }
    });
});
