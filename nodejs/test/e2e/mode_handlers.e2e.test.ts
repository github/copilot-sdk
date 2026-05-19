/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import type {
    AutoModeSwitchRequest,
    CopilotSession,
    ExitPlanModeRequest,
    ExitPlanModeResult,
    SessionEvent,
} from "../../src/index.js";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

const EVENT_TIMEOUT_MS = 30_000;
const MODE_HANDLER_TOKEN = "mode-handler-token";
const PLAN_SUMMARY = "Greeting file implementation plan";
const PLAN_PROMPT =
    "Create a brief implementation plan for adding a greeting.txt file, then request approval with exit_plan_mode.";
const AUTO_MODE_PROMPT =
    "Explain that auto mode recovered from a rate limit in one short sentence.";

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
    const { copilotClient: client, openAiEndpoint, env } = await createSdkTestContext();

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
            onExitPlanMode: async (request, invocation): Promise<ExitPlanModeResult> => {
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
                mode: "plan" as unknown as NonNullable<Parameters<typeof session.send>[0]["mode"]>,
            });

            expect(exitPlanModeRequests).toHaveLength(1);
            expect(exitPlanModeRequests[0]).toMatchObject({
                summary: PLAN_SUMMARY,
                actions: ["interactive", "autopilot", "exit_only"],
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

    it("should invoke auto mode switch handler when rate limited", async () => {
        const autoModeSwitchRequests: AutoModeSwitchRequest[] = [];
        let session: CopilotSession | undefined;

        session = await client.createSession({
            gitHubToken: MODE_HANDLER_TOKEN,
            onPermissionRequest: approveAll,
            onAutoModeSwitch: (request, invocation) => {
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
