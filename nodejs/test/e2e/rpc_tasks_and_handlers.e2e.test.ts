/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import type { SessionEvent } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { waitForCondition } from "./harness/sdkTestHelper.js";

describe("Session tasks RPC and pending handlers", async () => {
    const { copilotClient: client, openAiEndpoint } = await createSdkTestContext();

    async function assertImplementedFailure(
        action: () => Promise<unknown>,
        method: string
    ): Promise<void> {
        await expect(action()).rejects.toSatisfy((err: unknown) => {
            const text = err instanceof Error ? `${err.message}\n${err.stack ?? ""}` : String(err);
            expect(text.toLowerCase()).not.toContain(`unhandled method ${method.toLowerCase()}`);
            return true;
        });
    }

    it("should list task state and return false for missing task operations", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        const tasks = await session.rpc.tasks.list();
        expect(tasks.tasks).toBeDefined();
        expect(tasks.tasks).toEqual([]);

        await expect(session.rpc.tasks.refresh()).resolves.toBeDefined();
        await expect(session.rpc.tasks.waitForPending()).resolves.toBeDefined();

        const progress = await session.rpc.tasks.getProgress({ id: "missing-task" });
        expect(progress.progress).toBeNull();

        const currentPromotable = await session.rpc.tasks.getCurrentPromotable();
        expect(currentPromotable.task).toBeUndefined();

        const promote = await session.rpc.tasks.promoteToBackground({ id: "missing-task" });
        expect(promote.promoted).toBe(false);

        const promoteCurrent = await session.rpc.tasks.promoteCurrentToBackground();
        expect(promoteCurrent.task).toBeUndefined();

        const cancel = await session.rpc.tasks.cancel({ id: "missing-task" });
        expect(cancel.cancelled).toBe(false);

        const remove = await session.rpc.tasks.remove({ id: "missing-task" });
        expect(remove.removed).toBe(false);

        const sendMessage = await session.rpc.tasks.sendMessage({
            id: "missing-task",
            message: "hello from the SDK E2E test",
        });
        expect(sendMessage.sent).toBe(false);
        expect(sendMessage.error?.trim()).toBeTruthy();

        await session.disconnect();
    }, 60_000);

    it("should report implemented error for missing task agent type", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        await assertImplementedFailure(
            () =>
                session.rpc.tasks.startAgent({
                    agentType: "missing-agent-type",
                    prompt: "Say hi",
                    name: "sdk-test-task",
                }),
            "session.tasks.startAgent"
        );

        await session.disconnect();
    });

    it("should report implemented error for invalid task agent model", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        await assertImplementedFailure(
            () =>
                session.rpc.tasks.startAgent({
                    agentType: "general-purpose",
                    prompt: "Say hi",
                    name: "sdk-test-task",
                    description: "SDK task agent validation",
                    model: "not-a-real-model",
                }),
            "session.tasks.startAgent"
        );
        expect((await session.rpc.tasks.list()).tasks).toEqual([]);

        await session.disconnect();
    });

    it(
        "should start a general-purpose agent on the parent's model",
        { timeout: 240_000 },
        async () => {
            const parentModel = "claude-sonnet-5";
            const session = await client.createSession({
                onPermissionRequest: approveAll,
                model: parentModel,
            });
            const started: Extract<SessionEvent, { type: "subagent.started" }>[] = [];
            const parentReplies: string[] = [];
            const unsubscribe = session.on((event) => {
                if (event.type === "subagent.started") {
                    started.push(event);
                } else if (event.type === "assistant.message" && !event.agentId) {
                    parentReplies.push(event.data.content ?? "");
                }
            });
            try {
                // The token gives the parent's turn for the completion notification a reply to wait for.
                expect(
                    (
                        await session.sendAndWait({
                            prompt:
                                "Reply with TASK_MODEL_READY exactly. When a background agent notification " +
                                "arrives later, reply with TASK_MODEL_NOTIFIED exactly.",
                        })
                    )?.data.content
                ).toContain("TASK_MODEL_READY");

                const prompt = "Reply with TASK_MODEL_CHILD_DONE exactly.";
                const { agentId } = await session.rpc.tasks.startAgent({
                    agentType: "general-purpose",
                    prompt,
                    name: "sdk-inherited-model-agent",
                    description: "SDK inherited model coverage",
                });
                const findTask = async () =>
                    (await session.rpc.tasks.list()).tasks.find((entry) => entry.id === agentId);
                let lastTask: Awaited<ReturnType<typeof findTask>>;
                await waitForCondition(
                    async () => {
                        lastTask = await findTask();
                        return (
                            !!lastTask &&
                            ["completed", "idle"].includes(lastTask.status) &&
                            (lastTask.latestResponse ?? lastTask.result ?? "").includes(
                                "TASK_MODEL_CHILD_DONE"
                            )
                        );
                    },
                    { timeoutMs: 60_000 }
                ).catch((error: unknown) => {
                    throw new Error(`Agent ${agentId} never settled: ${JSON.stringify(lastTask)}`, {
                        cause: error,
                    });
                });
                // The parent's reply to the notification ends the last turn this test records.
                await waitForCondition(
                    () => parentReplies.some((reply) => reply.includes("TASK_MODEL_NOTIFIED")),
                    { timeoutMs: 60_000 }
                ).catch((error: unknown) => {
                    throw new Error(
                        `Parent never answered the completion notification: ${JSON.stringify(parentReplies)}`,
                        { cause: error }
                    );
                });

                const task = await findTask();
                // No model was requested; the wire reports that as `null`.
                expect(task?.model ?? null).toBeNull();
                expect(task?.resolvedModel).toBe(parentModel);
                expect(started.map((event) => event.data.model)).toEqual([parentModel]);
                // Raw requests, scoped by this test's prompts: an earlier test's late
                // request can land here, and its error response is not parsable.
                const requestModelsMentioning = async (text: string) =>
                    (await openAiEndpoint.getRequests())
                        .filter((request) => request.body.includes(text))
                        .map((request) => (JSON.parse(request.body) as { model?: string }).model);
                const childModels = await requestModelsMentioning(prompt);
                const parentModels = await requestModelsMentioning("TASK_MODEL_READY");
                expect(childModels.length).toBeGreaterThan(0);
                expect(parentModels.length).toBeGreaterThan(0);
                expect(new Set([...childModels, ...parentModels])).toEqual(new Set([parentModel]));
            } finally {
                unsubscribe();
                await session.disconnect();
            }
        }
    );

    it("should start background agent and report task details", { timeout: 240_000 }, async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        const replies: string[] = [];
        const unsubscribe = session.on((event) => {
            if (event.type === "assistant.message") {
                replies.push(event.data.content ?? "");
            }
        });
        try {
            expect(
                (await session.sendAndWait({ prompt: "Reply with TASK_AGENT_READY exactly." }))
                    ?.data.content
            ).toContain("TASK_AGENT_READY");
            const prompt = "Reply with TASK_AGENT_DONE exactly.";
            const started = await session.rpc.tasks.startAgent({
                agentType: "general-purpose",
                prompt,
                name: "sdk-background-agent",
                description: "SDK background agent coverage",
            });
            expect(started.agentId).toBeTruthy();

            await waitForCondition(
                async () =>
                    (await session.rpc.tasks.list()).tasks.some(
                        (task) => task.id === started.agentId
                    ),
                { timeoutMessage: `Background agent ${started.agentId} never appeared` }
            );
            const task = (await session.rpc.tasks.list()).tasks.find(
                (entry) => entry.id === started.agentId
            );
            expect(task).toMatchObject({
                id: started.agentId,
                agentType: "general-purpose",
                prompt,
                description: "SDK background agent coverage",
                executionMode: "background",
                canPromoteToBackground: false,
            });
            expect(Number.isNaN(Date.parse(task!.startedAt))).toBe(false);
            expect(
                (await session.rpc.tasks.promoteToBackground({ id: started.agentId })).promoted
            ).toBe(false);

            await waitForCondition(
                () => replies.some((message) => message.includes("TASK_AGENT_DONE")),
                {
                    timeoutMs: 60_000,
                    timeoutMessage: `Agent ${started.agentId} did not complete: ${JSON.stringify(replies)}`,
                }
            );
            await waitForCondition(
                async () => {
                    const task = (await session.rpc.tasks.list()).tasks.find(
                        (entry) => entry.id === started.agentId
                    );
                    return (
                        !!task &&
                        ["completed", "idle"].includes(task.status) &&
                        (task.latestResponse ?? task.result ?? "").includes("TASK_AGENT_DONE")
                    );
                },
                { timeoutMs: 60_000, timeoutMessage: `Agent ${started.agentId} never settled` }
            );
            const current = (await session.rpc.tasks.list()).tasks.find(
                (entry) => entry.id === started.agentId
            );
            expect(current?.latestResponse ?? current?.result).toContain("TASK_AGENT_DONE");
            if (current?.status === "idle") {
                expect((await session.rpc.tasks.cancel({ id: started.agentId })).cancelled).toBe(
                    true
                );
            }
            const removed = await session.rpc.tasks.remove({ id: started.agentId });
            expect(removed.removed || current === undefined).toBe(true);
            await waitForCondition(
                async () =>
                    !(await session.rpc.tasks.list()).tasks.some(
                        (entry) => entry.id === started.agentId
                    ),
                { timeoutMessage: `Completed agent ${started.agentId} remains listed` }
            );
        } finally {
            unsubscribe();
            await session.disconnect();
        }
    });

    it("should return expected results for missing pending handler requestIds", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        const tool = await session.rpc.tools.handlePendingToolCall({
            requestId: "missing-tool-request",
            result: "tool result",
        });
        expect(tool.success).toBe(false);

        const command = await session.rpc.commands.handlePendingCommand({
            requestId: "missing-command-request",
            error: "command error",
        });
        expect(command.success).toBe(true);

        const elicitation = await session.rpc.ui.handlePendingElicitation({
            requestId: "missing-elicitation-request",
            result: { action: "cancel" },
        });
        expect(elicitation.success).toBe(false);

        const userInput = await session.rpc.ui.handlePendingUserInput({
            requestId: "missing-user-input-request",
            response: { answer: "typed answer", wasFreeform: true },
        });
        expect(userInput.success).toBe(false);

        const sampling = await session.rpc.ui.handlePendingSampling({
            requestId: "missing-sampling-request",
            response: {},
        });
        expect(sampling.success).toBe(false);

        const autoModeSwitch = await session.rpc.ui.handlePendingAutoModeSwitch({
            requestId: "missing-auto-mode-switch-request",
            response: "no",
        });
        expect(autoModeSwitch.success).toBe(false);

        const exitPlanMode = await session.rpc.ui.handlePendingExitPlanMode({
            requestId: "missing-exit-plan-mode-request",
            response: {
                approved: false,
                feedback: "No pending plan approval",
                selectedAction: "exit_only",
            },
        });
        expect(exitPlanMode.success).toBe(false);

        const permission = await session.rpc.permissions.handlePendingPermissionRequest({
            requestId: "missing-permission-request",
            result: { kind: "reject", feedback: "not approved" },
        });
        expect(permission.success).toBe(false);

        const permanent = await session.rpc.permissions.handlePendingPermissionRequest({
            requestId: "missing-permanent-permission-request",
            result: { kind: "approve-permanently", domain: "example.com" },
        });
        expect(permanent.success).toBe(false);

        const sessionApproval = await session.rpc.permissions.handlePendingPermissionRequest({
            requestId: "missing-session-approval-request",
            result: {
                kind: "approve-for-session",
                approval: { kind: "custom-tool", toolName: "missing-tool" },
            },
        });
        expect(sessionApproval.success).toBe(false);

        const locationApproval = await session.rpc.permissions.handlePendingPermissionRequest({
            requestId: "missing-location-approval-request",
            result: {
                kind: "approve-for-location",
                approval: { kind: "custom-tool", toolName: "missing-tool" },
                locationKey: "missing-location",
            },
        });
        expect(locationApproval.success).toBe(false);

        const sessionLimits = await session.rpc.ui.handlePendingSessionLimitsExhausted({
            requestId: "missing-session-limits-request",
            response: { action: "cancel" },
        });
        expect(sessionLimits.success).toBe(false);

        const headers = await session.rpc.mcp.headers.handlePendingHeadersRefreshRequest({
            requestId: "missing-headers-refresh-request",
            result: {
                kind: "headers",
                headers: { "X-SDK-Test": "missing" },
            },
        });
        expect(headers.success).toBe(false);

        const noHeaders = await session.rpc.mcp.headers.handlePendingHeadersRefreshRequest({
            requestId: "missing-headers-refresh-none-request",
            result: { kind: "none" },
        });
        expect(noHeaders.success).toBe(false);

        await session.disconnect();
    });

    it("should round trip rpc elicitation through config handler", async () => {
        let resolveContext!: (value: unknown) => void;
        const handlerContext = new Promise<unknown>((resolve) => {
            resolveContext = resolve;
        });
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            onElicitationRequest: (context) => {
                resolveContext(context);
                return {
                    action: "accept",
                    content: {
                        answer: "from handler",
                        confirmed: true,
                    },
                };
            },
        });

        const schema = {
            type: "object" as const,
            properties: {
                answer: { type: "string" as const },
                confirmed: { type: "boolean" as const },
            },
            required: ["answer"],
        };

        const response = await session.rpc.ui.elicitation({
            message: "Need details",
            requestedSchema: schema,
        });
        const context = (await handlerContext) as {
            sessionId: string;
            message: string;
            requestedSchema?: typeof schema;
        };

        expect(context.sessionId).toBe(session.sessionId);
        expect(context.message).toBe("Need details");
        expect(context.requestedSchema?.type).toBe("object");
        expect(Object.keys(context.requestedSchema?.properties ?? {})).toEqual([
            "answer",
            "confirmed",
        ]);
        expect(context.requestedSchema?.required).toEqual(["answer"]);
        expect(response.action).toBe("accept");
        expect(response.content?.answer).toBe("from handler");
        expect(response.content?.confirmed).toBe(true);

        await session.disconnect();
    });

    it("should register and unregister direct auto mode switch handler", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        const missing = await session.rpc.ui.unregisterDirectAutoModeSwitchHandler({
            handle: "missing-direct-auto-mode-handle",
        });
        expect(missing.unregistered).toBe(false);

        const registration = await session.rpc.ui.registerDirectAutoModeSwitchHandler();
        expect(registration.handle.trim()).toBeTruthy();

        const unregister = await session.rpc.ui.unregisterDirectAutoModeSwitchHandler({
            handle: registration.handle,
        });
        expect(unregister.unregistered).toBe(true);

        const unregisterAgain = await session.rpc.ui.unregisterDirectAutoModeSwitchHandler({
            handle: registration.handle,
        });
        expect(unregisterAgain.unregistered).toBe(false);

        await session.disconnect();
    });
});
