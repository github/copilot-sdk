/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it, onTestFailed } from "vitest";
import { z } from "zod";
import { approveAll, CopilotRequestHandler } from "../../src/index.js";
import type { SessionEvent, CopilotRequestContext, CopilotSession } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { getNextEventOfType, waitForCondition } from "./harness/sdkTestHelper.js";
import { isByokBackend } from "./harness/testBackend";

const capiIt = it.skipIf(isByokBackend);

class ModelRequestRecorder extends CopilotRequestHandler {
    readonly requests: Array<{
        sessionId?: string;
        agentId?: string;
        body: { model?: string; reasoning_effort?: string };
    }> = [];

    protected override async sendRequest(
        request: Request,
        context: CopilotRequestContext
    ): Promise<Response> {
        const pathname = new URL(request.url).pathname;
        if (["/chat/completions", "/responses", "/v1/messages"].includes(pathname)) {
            const body = z
                .object({
                    model: z.string(),
                    reasoning_effort: z.string().optional(),
                    reasoning: z.object({ effort: z.string().optional() }).optional(),
                    output_config: z.object({ effort: z.string().optional() }).optional(),
                })
                .parse(await request.clone().json());
            this.requests.push({
                sessionId: context.sessionId,
                agentId: context.agentId,
                body: {
                    model: body.model,
                    reasoning_effort:
                        pathname === "/responses"
                            ? body.reasoning?.effort
                            : pathname === "/v1/messages"
                              ? body.output_config?.effort
                              : body.reasoning_effort,
                },
            });
        }
        return super.sendRequest(request, context);
    }
}

describe("Session tasks RPC and pending handlers", async () => {
    const {
        copilotClient: client,
        openAiEndpoint,
        createClient,
    } = await createSdkTestContext({
        modelNames: { "claude-sonnet-5": "Claude Sonnet 5" },
    });

    async function getAgentTask(session: CopilotSession, agentId: string) {
        const task = (await session.rpc.tasks.list()).tasks.find((entry) => entry.id === agentId);
        if (task && task.type !== "agent") {
            throw new Error(`Expected agent task ${agentId}, received ${JSON.stringify(task)}`);
        }
        return task;
    }

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

    // BYOK model IDs are user-defined; only CAPI validates them against its catalog.
    capiIt("should report implemented error for invalid task agent model", async () => {
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
                const findTask = () => getAgentTask(session, agentId);
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
                // Provider-backed subagents carry an explicit model override; CAPI leaves it unset.
                expect(task?.model ?? null).toBe(isByokBackend ? parentModel : null);
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
            const task = await getAgentTask(session, started.agentId);
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
                    const task = await getAgentTask(session, started.agentId);
                    return (
                        !!task &&
                        ["completed", "idle"].includes(task.status) &&
                        (task.latestResponse ?? task.result ?? "").includes("TASK_AGENT_DONE")
                    );
                },
                { timeoutMs: 60_000, timeoutMessage: `Agent ${started.agentId} never settled` }
            );
            const current = await getAgentTask(session, started.agentId);
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

    // Shell IDs count up per runtime process, so each attached-shell test owns its
    // client: the recorded `shellId: 0` then holds regardless of test order.
    async function withRunningAttachedShell(
        run: (
            session: CopilotSession,
            shellId: string,
            eventTypes: SessionEvent["type"][],
            replies: string[]
        ) => Promise<void>
    ): Promise<void> {
        const shellClient = createClient();
        try {
            const session = await shellClient.createSession({ onPermissionRequest: approveAll });
            const eventTypes: SessionEvent["type"][] = [];
            const replies: string[] = [];
            const unsubscribe = session.on((event) => {
                eventTypes.push(event.type);
                if (event.type === "assistant.message") {
                    replies.push(event.data.content ?? "");
                }
            });
            try {
                const assistantIdle = getNextEventOfType(session, "assistant.idle");
                await session.send({
                    prompt: "Use your shell tool to run `sleep 600` in async mode without detaching. Do not read its output or wait for it. Then reply with exactly SHELL_STARTED.",
                });
                await assistantIdle;
                expect(replies.at(-1)).toContain("SHELL_STARTED");

                const shell = (await session.rpc.tasks.list()).tasks.find(
                    (task) => task.type === "shell" && task.status === "running"
                );
                expect(shell).toMatchObject({ attachmentMode: "attached" });
                await run(session, shell!.id, eventTypes, replies);
            } finally {
                unsubscribe();
                await session.disconnect();
            }
        } finally {
            await shellClient.stop();
        }
    }

    it(
        "should emit session idle after cancelling a running attached shell",
        { timeout: 120_000 },
        async () => {
            await withRunningAttachedShell(async (session, shellId, eventTypes) => {
                // The running attached shell holds the turn's session.idle.
                expect(eventTypes).not.toContain("session.idle");

                expect((await session.rpc.tasks.cancel({ id: shellId })).cancelled).toBe(true);
                await waitForCondition(() => eventTypes.includes("session.idle"), {
                    timeoutMessage: `session.idle never followed cancelling shell ${shellId}`,
                });
            });
        }
    );

    it(
        "should run a queued message after cancelling a running attached shell",
        { timeout: 120_000 },
        async () => {
            await withRunningAttachedShell(async (session, shellId, eventTypes, replies) => {
                const started = performance.now();
                let phase = "enqueue";
                let queuePolls = 0;
                let pendingCount = 0;
                let queuedMatch = false;
                onTestFailed(() => {
                    console.error(
                        "Attached shell queue failure boundary:",
                        JSON.stringify({
                            phase,
                            elapsedMs: Math.round(performance.now() - started),
                            queuePolls,
                            pendingCount,
                            queuedMatch,
                            replyCount: replies.length,
                            hasQueuedReply: replies.some((reply) => reply.includes("QUEUED_DONE")),
                            sessionIdleCount: eventTypes.filter((type) => type === "session.idle")
                                .length,
                            assistantIdleCount: eventTypes.filter(
                                (type) => type === "assistant.idle"
                            ).length,
                        })
                    );
                });
                // The running attached shell holds idle, so an enqueued message waits behind it.
                await session.send({ prompt: "Reply with exactly QUEUED_DONE.", mode: "enqueue" });
                phase = "pending-queue";
                await waitForCondition(
                    async () => {
                        const { items } = await session.rpc.queue.pendingItems();
                        queuePolls++;
                        pendingCount = items.length;
                        queuedMatch = items.some((item) =>
                            item.displayText.includes("QUEUED_DONE")
                        );
                        return queuedMatch;
                    },
                    { timeoutMessage: "The enqueued message was not parked behind the shell" }
                );
                phase = "assert-parked";
                expect(replies.some((reply) => reply.includes("QUEUED_DONE"))).toBe(false);
                expect(eventTypes).not.toContain("session.idle");

                phase = "cancel-shell";
                expect((await session.rpc.tasks.cancel({ id: shellId })).cancelled).toBe(true);
                phase = "queued-reply";
                await waitForCondition(
                    () => replies.some((reply) => reply.includes("QUEUED_DONE")),
                    {
                        timeoutMessage: `The queued message never ran after cancelling shell ${shellId}`,
                    }
                );
                phase = "session-idle";
                await waitForCondition(() => eventTypes.includes("session.idle"), {
                    timeoutMessage: "session.idle never followed the queued message",
                });
            });
        }
    );

    it(
        "should apply a custom agent's composed model and effort when started over RPC",
        { timeout: 240_000 },
        async () => {
            const requestRecorder = new ModelRequestRecorder();
            const recordingClient = createClient({ requestHandler: requestRecorder });
            try {
                const session = await recordingClient.createSession({
                    onPermissionRequest: approveAll,
                    customAgents: [
                        {
                            name: "sdk-composed-model-agent",
                            description: "SDK composed model coverage",
                            prompt: "Reply with SDK_COMPOSED_AGENT_DONE exactly.",
                            model: "claude-sonnet-5:defaultReasoningEffort=high",
                        },
                    ],
                });
                const configurations: Array<{
                    agentId?: string;
                    model: string;
                    reasoningEffort?: string;
                }> = [];
                const lifecycleEvents: SessionEvent[] = [];
                const unsubscribe = session.on((event) => {
                    if (event.type === "subagent.configured") {
                        configurations.push({ agentId: event.agentId, ...event.data });
                    }
                    if (event.type.startsWith("subagent.") || event.type === "session.shutdown") {
                        lifecycleEvents.push(event);
                    }
                });
                try {
                    const started = await session.rpc.tasks.startAgent({
                        agentType: "sdk-composed-model-agent",
                        prompt: "Reply with SDK_COMPOSED_AGENT_DONE exactly.",
                        name: "sdk-composed-model-task",
                    });
                    await waitForCondition(
                        () => configurations.some((event) => event.agentId === started.agentId),
                        {
                            timeoutMs: 60_000,
                            timeoutMessage: `Agent ${started.agentId} was never configured: ${JSON.stringify(configurations)}`,
                        }
                    );
                    expect(
                        configurations.find((event) => event.agentId === started.agentId)
                    ).toMatchObject({
                        model: "claude-sonnet-5",
                        reasoningEffort: "high",
                    });

                    await waitForCondition(
                        async () => {
                            const task = await getAgentTask(session, started.agentId);
                            return (
                                !!task &&
                                ["completed", "idle"].includes(task.status) &&
                                (task.latestResponse ?? task.result ?? "").includes(
                                    "SDK_COMPOSED_AGENT_DONE"
                                )
                            );
                        },
                        {
                            timeoutMs: 60_000,
                            timeoutMessage: `Agent ${started.agentId} never completed`,
                        }
                    );
                    const requests = requestRecorder.requests
                        .filter((request) => request.agentId === started.agentId)
                        .map((request) => request.body);
                    expect(requests.length).toBeGreaterThan(0);
                    for (const request of requests) {
                        expect(request).toMatchObject({
                            model: "claude-sonnet-5",
                            reasoning_effort: "high",
                        });
                    }
                    await waitForCondition(
                        () =>
                            lifecycleEvents.some(
                                (event) =>
                                    event.type === "subagent.completed" &&
                                    event.agentId === started.agentId
                            ),
                        { timeoutMessage: `Agent ${started.agentId} never emitted its completion` }
                    );
                    const completions = lifecycleEvents.filter(
                        (event) => event.type === "subagent.completed"
                    );
                    expect(completions).toHaveLength(1);
                    expect(completions[0].data.cancelled).not.toBe(true);
                    const beforeStop = [...lifecycleEvents];
                    expect(await recordingClient.stop()).toEqual([]);
                    expect(
                        lifecycleEvents.filter((event) => event.type.startsWith("subagent."))
                    ).toEqual(beforeStop);
                    expect(lifecycleEvents.at(-1)?.type).toBe("session.shutdown");
                } finally {
                    unsubscribe();
                }
            } finally {
                await recordingClient.stop();
            }
        }
    );

    it("should preserve a composed display-name effort in a model-issued task", async () => {
        const taskArguments: unknown[] = [];
        const requestRecorder = new ModelRequestRecorder();
        const recordingClient = createClient({ requestHandler: requestRecorder });
        try {
            const session = await recordingClient.createSession({
                model: "claude-sonnet-5",
                reasoningEffort: "medium",
                onPermissionRequest: approveAll,
                hooks: {
                    onPreToolUse: async (input) => {
                        if (input.toolName === "task") {
                            taskArguments.push(input.toolArgs);
                        }
                        return { permissionDecision: "allow" };
                    },
                },
                customAgents: [
                    {
                        name: "sdk-display-model-agent",
                        description: "SDK display-name model coverage",
                        prompt: "Reply with SDK_DISPLAY_AGENT_DONE exactly.",
                        // BYOK requires explicit IDs; CAPI also resolves its catalog's display names.
                        model: `${isByokBackend ? "claude-sonnet-5" : "Claude Sonnet 5"}:defaultReasoningEffort=high`,
                    },
                ],
            });
            if (!isByokBackend) {
                const modelList = await session.rpc.model.list();
                expect(modelList.list).toEqual(
                    expect.arrayContaining([
                        expect.objectContaining({ id: "claude-sonnet-5", name: "Claude Sonnet 5" }),
                    ])
                );
            }
            const configurations: Array<{
                agentId?: string;
                model: string;
                reasoningEffort?: string;
            }> = [];
            const lifecycleEvents: SessionEvent[] = [];
            const unsubscribe = session.on((event) => {
                if (event.type === "subagent.configured") {
                    configurations.push({ agentId: event.agentId, ...event.data });
                }
                if (event.type.startsWith("subagent.") || event.type === "session.shutdown") {
                    lifecycleEvents.push(event);
                }
            });
            try {
                const reply = await session.sendAndWait(
                    {
                        prompt:
                            'Use the task tool with agent_type "sdk-display-model-agent", mode "sync", and prompt "Reply with SDK_DISPLAY_AGENT_DONE exactly.". ' +
                            "Do not specify model or reasoning_effort in the task arguments. " +
                            "After it completes, reply with SDK_DISPLAY_PARENT_DONE exactly.",
                    },
                    120_000
                );
                expect(reply?.data.content).toContain("SDK_DISPLAY_PARENT_DONE");
                expect(taskArguments).toHaveLength(1);
                expect(taskArguments[0]).toMatchObject({
                    agent_type: "sdk-display-model-agent",
                    mode: "sync",
                });
                expect(taskArguments[0]).not.toHaveProperty("model");
                expect(taskArguments[0]).not.toHaveProperty("reasoning_effort");
                expect(configurations).toHaveLength(1);
                const configuration = configurations[0];
                expect(configuration).toMatchObject({
                    model: "claude-sonnet-5",
                    reasoningEffort: "high",
                });
                expect(configuration.agentId).toBeTruthy();
                const requests = requestRecorder.requests
                    .filter((request) => request.agentId === configuration.agentId)
                    .map((request) => request.body);
                expect(requests.length).toBeGreaterThan(0);
                for (const request of requests) {
                    expect(request).toMatchObject({
                        model: "claude-sonnet-5",
                        reasoning_effort: "high",
                    });
                }
                const parentRequests = requestRecorder.requests.filter(
                    (request) =>
                        request.sessionId === session.sessionId &&
                        request.agentId !== configuration.agentId
                );
                expect(parentRequests.length).toBeGreaterThan(0);
                for (const request of parentRequests) {
                    expect(request.body.reasoning_effort).toBe("medium");
                }
                const completions = lifecycleEvents.filter(
                    (event) => event.type === "subagent.completed"
                );
                expect(completions).toHaveLength(1);
                expect(completions[0].agentId).toBe(configuration.agentId);
                expect(completions[0].data.cancelled).not.toBe(true);
                const beforeDisconnect = [...lifecycleEvents];
                await session.disconnect();
                expect(
                    lifecycleEvents.filter((event) => event.type.startsWith("subagent."))
                ).toEqual(beforeDisconnect);
                expect(lifecycleEvents.at(-1)?.type).toBe("session.shutdown");
            } finally {
                unsubscribe();
            }
        } finally {
            await recordingClient.stop();
        }
    }, 240_000);

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
