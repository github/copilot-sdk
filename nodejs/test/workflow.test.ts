/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { afterEach, describe, expect, it, onTestFinished, vi } from "vitest";
import { ResponseError } from "vscode-jsonrpc/node.js";
import { CopilotClient } from "../src/client.js";
import { joinSession } from "../src/extension.js";
import { CopilotSession } from "../src/session.js";
import {
    defineWorkflow,
    WorkflowRunError,
    type WorkflowAgentOptions,
    type WorkflowDefinition,
} from "../src/workflow.js";

async function stopClient(client: CopilotClient): Promise<void> {
    await client.stop();
}

describe("workflows", () => {
    const originalSessionId = process.env.SESSION_ID;

    afterEach(() => {
        if (originalSessionId === undefined) {
            delete process.env.SESSION_ID;
        } else {
            process.env.SESSION_ID = originalSessionId;
        }
        vi.restoreAllMocks();
    });

    it("defines a stable handle and accepts omitted limits", async () => {
        const meta = {
            name: "no-limits",
            description: "A workflow without resource limits",
            phases: [],
        };
        const run = vi.fn(async ({ args }: { args: unknown }) => args);
        const handle = defineWorkflow({ meta, run });

        expect(handle.meta).toBe(meta);
        expect(Object.isFrozen(handle)).toBe(true);

        const session = new CopilotSession("session-1", {} as never);
        session.registerWorkflows([handle]);
        const result = await session.clientSessionApis.workflow!.execute({
            sessionId: session.sessionId,
            name: meta.name,
            runId: "run-1",
            args: { value: 42 },
        });

        expect(run).toHaveBeenCalledOnce();
        expect(result).toEqual({ result: { value: 42 } });
    });

    it.each([
        ["maxConcurrentSubagents", 0],
        ["maxConcurrentSubagents", 1.5],
        ["maxTotalSubagents", -1],
        ["maxTotalSubagents", Number.POSITIVE_INFINITY],
        ["timeout", 0],
        ["timeout", Number.NaN],
    ] as const)("rejects invalid %s limit %s", (field, value) => {
        const definition = {
            meta: {
                name: `invalid-${field}-${String(value)}`,
                description: "Invalid workflow",
                phases: [],
                limits: { [field]: value },
            },
            run: async () => null,
        } as WorkflowDefinition;

        expect(() => defineWorkflow(definition)).toThrow(/must be a positive/);
    });

    it("serializes only workflow metadata in the extension resume payload", async () => {
        const client = new CopilotClient();
        await client.start();
        onTestFinished(() => stopClient(client));

        const run = vi.fn(async () => ({ ok: true }));
        const workflow = defineWorkflow({
            meta: {
                name: "registered",
                description: "Registration test",
                phases: [{ title: "Run" }],
                limits: { maxTotalSubagents: 2 },
            },
            run,
        });
        const sendRequest = vi
            .spyOn(
                (client as never as { connection: { sendRequest: Function } }).connection,
                "sendRequest"
            )
            .mockImplementation(async (method: string, params: Record<string, unknown>) => {
                if (method === "session.resume") {
                    const sessions = (client as never as { sessions: Map<string, CopilotSession> })
                        .sessions;
                    expect(
                        sessions.get(params.sessionId as string)?.clientSessionApis.workflow
                    ).toBeDefined();
                    return { sessionId: params.sessionId };
                }
                throw new Error(`Unexpected method: ${method}`);
            });

        await client.resumeSessionForExtension(
            "session-registration",
            { onPermissionRequest: () => ({ kind: "approved" }) },
            [workflow]
        );

        const payload = sendRequest.mock.calls.find(
            ([method]) => method === "session.resume"
        )![1] as {
            workflows: unknown[];
        };
        expect(payload.workflows).toEqual([workflow.meta]);
        expect(payload.workflows[0]).not.toHaveProperty("run");
        expect(JSON.stringify(payload.workflows)).not.toContain("async");
    });

    it("passes workflows only through the extension join path", async () => {
        process.env.SESSION_ID = "session-extension";
        const workflow = defineWorkflow({
            meta: {
                name: "extension-only",
                description: "Extension-only registration",
                phases: [],
            },
            run: async () => ({ ok: true }),
        });
        const resumeSessionForExtension = vi
            .spyOn(CopilotClient.prototype, "resumeSessionForExtension")
            .mockResolvedValue({} as CopilotSession);

        await joinSession({ workflows: [workflow] });

        expect(resumeSessionForExtension).toHaveBeenCalledWith(
            "session-extension",
            expect.objectContaining({ suppressResumeEvent: true }),
            [workflow]
        );
    });

    it("builds the workflow context with the unrestricted joined session identity", async () => {
        process.env.SESSION_ID = "session-context";
        const sendRequest = vi.fn(async (method: string) => {
            if (method === "session.workflow.log") {
                return {};
            }
            if (method === "session.tasks.list") {
                return { tasks: [] };
            }
            throw new Error(`Unexpected method: ${method}`);
        });
        const joinedSession = new CopilotSession("session-context", { sendRequest } as never);
        const contextSeen = Promise.withResolvers<{
            args: unknown;
            session: CopilotSession;
            signal: AbortSignal;
        }>();
        const workflow = defineWorkflow({
            meta: {
                name: "context",
                description: "Context test",
                phases: [],
            },
            run: async (context) => {
                contextSeen.resolve(context);
                context.phase("A");
                context.log("hi");
                const tasks = await context.session.rpc.tasks.list();
                return { ok: true, taskCount: tasks.tasks.length };
            },
        });
        vi.spyOn(CopilotClient.prototype, "resumeSessionForExtension").mockImplementation(
            async (_sessionId, _config, workflows) => {
                joinedSession.registerWorkflows(workflows);
                return joinedSession;
            }
        );

        const joinSessionResult = await joinSession({ workflows: [workflow] });
        const executeResult = await joinSessionResult.clientSessionApis.workflow!.execute({
            sessionId: joinSessionResult.sessionId,
            name: "context",
            runId: "run-context",
            args: { value: 42 },
        });
        const context = await contextSeen.promise;

        expect(context.args).toEqual({ value: 42 });
        expect(context.session).toBe(joinSessionResult);
        expect(context.session.rpc).toBe(joinSessionResult.rpc);
        expect(context.signal).toBeInstanceOf(AbortSignal);
        expect(executeResult).toEqual({ result: { ok: true, taskCount: 0 } });
        expect(sendRequest).toHaveBeenCalledWith("session.tasks.list", {
            sessionId: joinSessionResult.sessionId,
        });
        expect(sendRequest).toHaveBeenCalledWith("session.workflow.log", {
            sessionId: joinSessionResult.sessionId,
            runId: "run-context",
            lines: [
                { seq: 0, kind: "phase", text: "A" },
                { seq: 1, kind: "log", text: "hi" },
            ],
        });
    });

    it("flushes progress incrementally while a workflow body is awaiting", async () => {
        const sendRequest = vi.fn(async () => ({}));
        const session = new CopilotSession("session-live-progress", { sendRequest } as never);
        const body = Promise.withResolvers<void>();
        const workflow = defineWorkflow({
            meta: {
                name: "live-progress",
                description: "Incremental progress test",
                phases: [],
            },
            run: async ({ log }) => {
                log("before await");
                await body.promise;
                return "done";
            },
        });
        session.registerWorkflows([workflow]);

        const execution = session.clientSessionApis.workflow!.execute({
            sessionId: session.sessionId,
            name: "live-progress",
            runId: "run-live-progress",
            args: {},
        });
        await vi.waitFor(() => {
            expect(sendRequest).toHaveBeenCalledWith("session.workflow.log", {
                sessionId: session.sessionId,
                runId: "run-live-progress",
                lines: [{ seq: 0, kind: "log", text: "before await" }],
            });
        });

        body.resolve();
        await expect(execution).resolves.toEqual({ result: "done" });
    });

    it("calls workflow.agent with the current run id and returns its text", async () => {
        const sendRequest = vi.fn(async (method: string) => {
            if (method === "session.workflow.agent") {
                return { result: "pong" };
            }
            throw new Error(`Unexpected method: ${method}`);
        });
        const session = new CopilotSession("session-agent", { sendRequest } as never);
        const workflow = defineWorkflow({
            meta: {
                name: "agent",
                description: "Agent context test",
                phases: [],
            },
            run: async ({ agent }) =>
                agent("Reply with pong", {
                    label: "Pong helper",
                    model: "gpt-test",
                    schema: { type: "string" },
                    effort: "high",
                } as WorkflowAgentOptions),
        });
        session.registerWorkflows([workflow]);

        await expect(
            session.clientSessionApis.workflow!.execute({
                sessionId: session.sessionId,
                name: "agent",
                runId: "run-agent",
                args: {},
            })
        ).resolves.toEqual({ result: "pong" });
        expect(sendRequest).toHaveBeenCalledWith("session.workflow.agent", {
            sessionId: session.sessionId,
            workflowRunId: "run-agent",
            prompt: "Reply with pong",
            opts: {
                label: "Pong helper",
                model: "gpt-test",
                schema: { type: "string" },
            },
        });
    });

    it("runs a durable step once, serves cached null, and does not cache failures", async () => {
        const journal = new Map<string, unknown>();
        const sendRequest = vi.fn(
            async (method: string, params: { key?: string; resultJson?: unknown }) => {
                if (method === "session.workflow.journal.get") {
                    return journal.has(params.key!)
                        ? { hit: true, resultJson: journal.get(params.key!) }
                        : { hit: false };
                }
                if (method === "session.workflow.journal.put") {
                    journal.set(params.key!, params.resultJson);
                    return {};
                }
                throw new Error(`Unexpected method: ${method}`);
            }
        );
        const session = new CopilotSession("session-step", { sendRequest } as never);
        let cachedProducerCalls = 0;
        let failingProducerCalls = 0;
        const workflow = defineWorkflow({
            meta: {
                name: "step",
                description: "Durable step context test",
                phases: [],
            },
            run: async ({ step }) => {
                const first = await step("cached-null", async () => {
                    cachedProducerCalls++;
                    return null;
                });
                const second = await step("cached-null", async () => {
                    cachedProducerCalls++;
                    return "wrong";
                });
                const failed = await step("retry", async () => {
                    failingProducerCalls++;
                    throw new Error("transient");
                }).catch(() => "failed");
                const retried = await step("retry", async () => {
                    failingProducerCalls++;
                    return "recovered";
                });
                return { first, second, failed, retried };
            },
        });
        session.registerWorkflows([workflow]);

        await expect(
            session.clientSessionApis.workflow!.execute({
                sessionId: session.sessionId,
                name: "step",
                runId: "run-step",
                args: {},
            })
        ).resolves.toEqual({
            result: { first: null, second: null, failed: "failed", retried: "recovered" },
        });
        expect(cachedProducerCalls).toBe(1);
        expect(failingProducerCalls).toBe(2);
        expect(
            sendRequest.mock.calls.filter(([method]) => method === "session.workflow.journal.put")
        ).toHaveLength(2);
    });

    it("exposes workflow getRun and forwards the run id", async () => {
        const envelope = { runId: "run-read", status: "error", error: "failed" };
        const sendRequest = vi.fn(async () => envelope);
        const session = new CopilotSession("session-read", { sendRequest } as never);

        await expect(session.workflow.getRun("run-read")).resolves.toEqual(envelope);
        expect(sendRequest).toHaveBeenCalledWith("session.workflow.getRun", {
            sessionId: session.sessionId,
            runId: "run-read",
        });
    });

    it("exposes workflow cancel and forwards the run id", async () => {
        const envelope = { runId: "run-cancel", status: "cancelled", reason: "cancelled" };
        const sendRequest = vi.fn(async () => envelope);
        const session = new CopilotSession("session-cancel", { sendRequest } as never);

        await expect(session.workflow.cancel("run-cancel")).resolves.toEqual(envelope);
        expect(sendRequest).toHaveBeenCalledWith("session.workflow.cancel", {
            sessionId: session.sessionId,
            runId: "run-cancel",
        });
    });

    it("runs parallel as a barrier and maps a throwing thunk to null", async () => {
        const first = Promise.withResolvers<string>();
        const second = Promise.withResolvers<string>();
        const started: string[] = [];
        const session = new CopilotSession("session-parallel", {} as never);
        const workflow = defineWorkflow({
            meta: {
                name: "parallel",
                description: "Parallel combinator test",
                phases: [],
            },
            run: async ({ parallel }) =>
                parallel([
                    async () => {
                        started.push("first");
                        return first.promise;
                    },
                    async () => {
                        started.push("second");
                        return second.promise;
                    },
                    async () => {
                        started.push("throwing");
                        throw new Error("expected");
                    },
                ]),
        });
        session.registerWorkflows([workflow]);

        let settled = false;
        const execution = session.clientSessionApis
            .workflow!.execute({
                sessionId: session.sessionId,
                name: "parallel",
                runId: "run-parallel",
                args: {},
            })
            .finally(() => {
                settled = true;
            });
        await vi.waitFor(() => expect(started).toEqual(["first", "second", "throwing"]));

        second.resolve("second");
        await Promise.resolve();
        expect(settled).toBe(false);

        first.resolve("first");
        await expect(execution).resolves.toEqual({ result: ["first", "second", null] });
    });

    it("rejects already-invoked promises passed to parallel with a clear diagnostic", async () => {
        const session = new CopilotSession("session-parallel-promises", {} as never);
        const workflow = defineWorkflow({
            meta: {
                name: "parallel-promises",
                description: "Parallel misuse diagnostic",
                phases: [],
            },
            run: async ({ parallel }) =>
                parallel([Promise.resolve("already running")] as unknown as Array<
                    () => Promise<string>
                >),
        });
        session.registerWorkflows([workflow]);

        await expect(
            session.clientSessionApis.workflow!.execute({
                sessionId: session.sessionId,
                name: "parallel-promises",
                runId: "run-parallel-promises",
                args: {},
            })
        ).rejects.toThrow(
            "parallel() expects an array of functions, not promises. Wrap each call: () => agent(...)"
        );
    });

    it("flows pipeline items independently and drops only the item whose stage throws", async () => {
        const releaseFirstItem = Promise.withResolvers<void>();
        const secondStageStarted = Promise.withResolvers<void>();
        const finalStageItems: string[] = [];
        const session = new CopilotSession("session-pipeline", {} as never);
        const workflow = defineWorkflow({
            meta: {
                name: "pipeline",
                description: "Pipeline combinator test",
                phases: [],
            },
            run: async ({ pipeline }) =>
                pipeline(
                    ["slow", "fast", "throw"],
                    async (_previous, item) => {
                        if (item === "slow") {
                            await releaseFirstItem.promise;
                        }
                        if (item === "throw") {
                            throw new Error("expected");
                        }
                        return `${item}-stage-1`;
                    },
                    async (previous, item) => {
                        if (item === "fast") {
                            secondStageStarted.resolve();
                        }
                        finalStageItems.push(item as string);
                        return `${previous}-stage-2`;
                    }
                ),
        });
        session.registerWorkflows([workflow]);

        const execution = session.clientSessionApis.workflow!.execute({
            sessionId: session.sessionId,
            name: "pipeline",
            runId: "run-pipeline",
            args: {},
        });
        await secondStageStarted.promise;
        expect(finalStageItems).toEqual(["fast"]);

        releaseFirstItem.resolve();
        await expect(execution).resolves.toEqual({
            result: ["slow-stage-1-stage-2", "fast-stage-1-stage-2", null],
        });
        expect(finalStageItems).toEqual(["fast", "slow"]);
    });

    it("enforces the 4096-item cap for parallel and pipeline", async () => {
        const session = new CopilotSession("session-fanout-cap", {} as never);
        const workflow = defineWorkflow({
            meta: {
                name: "fanout-cap",
                description: "Fan-out cap test",
                phases: [],
            },
            run: async ({ parallel, pipeline }) => {
                const tooManyItems = Array.from({ length: 4097 }, () => null);
                const parallelError = await parallel(
                    tooManyItems.map(() => async () => null)
                ).catch((error: unknown) => error);
                const pipelineError = await pipeline(tooManyItems).catch((error: unknown) => error);
                return {
                    parallel: (parallelError as Error).message,
                    pipeline: (pipelineError as Error).message,
                };
            },
        });
        session.registerWorkflows([workflow]);

        await expect(
            session.clientSessionApis.workflow!.execute({
                sessionId: session.sessionId,
                name: "fanout-cap",
                runId: "run-fanout-cap",
                args: {},
            })
        ).resolves.toEqual({
            result: {
                parallel: "parallel() accepts at most 4096 items; got 4097.",
                pipeline: "pipeline() accepts at most 4096 items; got 4097.",
            },
        });
    });

    it("does not deadlock nested combinators when only leaf agents use a one-slot limiter", async () => {
        let active = 0;
        let maxActive = 0;
        let tail = Promise.resolve();
        const sendRequest = vi.fn(
            async (method: string, params: { prompt: string }): Promise<{ result: string }> => {
                if (method !== "session.workflow.agent") {
                    throw new Error(`Unexpected method: ${method}`);
                }
                const previous = tail;
                const done = Promise.withResolvers<void>();
                tail = done.promise;
                await previous;
                active++;
                maxActive = Math.max(maxActive, active);
                await Promise.resolve();
                active--;
                done.resolve();
                return { result: params.prompt };
            }
        );
        const session = new CopilotSession("session-nested-combinators", {
            sendRequest,
        } as never);
        const workflow = defineWorkflow({
            meta: {
                name: "nested-combinators",
                description: "Nested combinator deadlock regression",
                phases: [],
            },
            run: async ({ agent, parallel, pipeline }) =>
                parallel([
                    () => parallel([() => agent("a"), () => agent("b")]),
                    () => pipeline(["c"], (_previous, item) => agent(item as string)),
                ]),
        });
        session.registerWorkflows([workflow]);

        await expect(
            session.clientSessionApis.workflow!.execute({
                sessionId: session.sessionId,
                name: "nested-combinators",
                runId: "run-nested-combinators",
                args: {},
            })
        ).resolves.toEqual({ result: [["a", "b"], ["c"]] });
        expect(maxActive).toBe(1);
        expect(sendRequest).toHaveBeenCalledTimes(3);
    });

    it("flushes buffered progress in finally when the workflow body throws", async () => {
        const sendRequest = vi.fn(async () => ({}));
        const session = new CopilotSession("session-throw-progress", { sendRequest } as never);
        const workflow = defineWorkflow({
            meta: {
                name: "throw-progress",
                description: "Throwing progress test",
                phases: [],
            },
            run: async ({ log }) => {
                log("before throw");
                throw new Error("body failed");
            },
        });
        session.registerWorkflows([workflow]);

        await expect(
            session.clientSessionApis.workflow!.execute({
                sessionId: session.sessionId,
                name: "throw-progress",
                runId: "run-throw-progress",
                args: {},
            })
        ).rejects.toThrow("body failed");
        expect(sendRequest).toHaveBeenCalledWith("session.workflow.log", {
            sessionId: session.sessionId,
            runId: "run-throw-progress",
            lines: [{ seq: 0, kind: "log", text: "before throw" }],
        });
    });

    it("surfaces the per-run abort signal on the workflow context", async () => {
        const session = new CopilotSession("session-abort-signal", {} as never);
        const signalSeen = Promise.withResolvers<AbortSignal>();
        const workflow = defineWorkflow({
            meta: {
                name: "abort-signal",
                description: "Abort signal test",
                phases: [],
            },
            run: async ({ signal }) => {
                signalSeen.resolve(signal);
                await new Promise<void>((resolve) =>
                    signal.addEventListener("abort", () => resolve(), { once: true })
                );
                return signal.aborted;
            },
        });
        session.registerWorkflows([workflow]);

        const execution = session.clientSessionApis.workflow!.execute({
            sessionId: session.sessionId,
            name: "abort-signal",
            runId: "run-abort-signal",
            args: {},
        });
        const signal = await signalSeen.promise;
        expect(signal.aborted).toBe(false);

        await session.clientSessionApis.workflow!.abort({
            sessionId: session.sessionId,
            runId: "run-abort-signal",
        });

        expect(signal.aborted).toBe(true);
        await expect(execution).resolves.toEqual({ result: true });
    });

    it("rejects an in-flight runtime-backed await when workflow.abort trips the signal", async () => {
        const agentResponse = Promise.withResolvers<{ result: string }>();
        const sendRequest = vi.fn(async (method: string) => {
            if (method === "session.workflow.agent") {
                return agentResponse.promise;
            }
            return {};
        });
        const session = new CopilotSession("session-abort-await", { sendRequest } as never);
        const workflow = defineWorkflow({
            meta: {
                name: "abort-await",
                description: "Abort an in-flight workflow await",
                phases: [],
            },
            run: async ({ agent }) => agent("wait forever"),
        });
        session.registerWorkflows([workflow]);

        const execution = session.clientSessionApis.workflow!.execute({
            sessionId: session.sessionId,
            name: "abort-await",
            runId: "run-abort-await",
            args: {},
        });
        await vi.waitFor(() => expect(sendRequest).toHaveBeenCalledWith("session.workflow.agent", expect.anything()));

        await session.clientSessionApis.workflow!.abort({
            sessionId: session.sessionId,
            runId: "run-abort-await",
        });

        await expect(execution).rejects.toMatchObject({ name: "AbortError" });
        agentResponse.resolve({ result: "late" });
    });

    it("dispatches workflow.execute by name and returns a structured unknown-name error", async () => {
        const run = vi.fn(async ({ args, log }) => {
            log("executing");
            return { echoed: args };
        });
        const workflow = defineWorkflow({
            meta: {
                name: "echo",
                description: "Echo arguments",
                phases: [],
            },
            run,
        });
        const session = new CopilotSession("session-execute", {
            sendRequest: vi.fn(async () => ({})),
        } as never);
        session.registerWorkflows([workflow]);

        await expect(
            session.clientSessionApis.workflow!.execute({
                sessionId: session.sessionId,
                name: "echo",
                runId: "run-echo",
                args: { message: "hello" },
            })
        ).resolves.toEqual({ result: { echoed: { message: "hello" } } });

        const error = await session.clientSessionApis
            .workflow!.execute({
                sessionId: session.sessionId,
                name: "missing",
                runId: "run-missing",
                args: {},
            })
            .catch((caught: unknown) => caught);
        expect(error).toBeInstanceOf(ResponseError);
        expect((error as ResponseError<{ code: string; name: string }>).data).toEqual({
            code: "workflow_not_found",
            name: "missing",
        });
    });

    it("runs workflows by name or handle and unwraps only foreground results", async () => {
        const workflow = defineWorkflow({
            meta: {
                name: "friendly-run",
                description: "Friendly run wrapper",
                phases: [],
            },
            run: async () => ({ unused: true }),
        });
        const sendRequest = vi.fn(
            async (
                _method: string,
                params: { name: string; options?: { background?: boolean } }
            ) =>
                params.options?.background
                    ? { runId: "run-background", status: "running" }
                    : {
                          runId: "run-foreground",
                          status: "completed",
                          result: { name: params.name },
                      }
        );
        const session = new CopilotSession("session-run", { sendRequest } as never);

        await expect(session.workflow.run("by-name", { args: { value: 1 } })).resolves.toEqual({
            name: "by-name",
        });
        await expect(session.workflow.run(workflow)).resolves.toEqual({ name: "friendly-run" });
        await expect(session.workflow.run("background", { background: true })).resolves.toEqual({
            runId: "run-background",
            status: "running",
        });

        expect(sendRequest).toHaveBeenNthCalledWith(1, "session.workflow.run", {
            sessionId: session.sessionId,
            name: "by-name",
            args: { value: 1 },
            options: { background: undefined, resumeFromRunId: undefined },
        });
        expect(sendRequest).toHaveBeenNthCalledWith(2, "session.workflow.run", {
            sessionId: session.sessionId,
            name: "friendly-run",
            args: {},
            options: { background: undefined, resumeFromRunId: undefined },
        });
    });

    it("throws WorkflowRunError with the full foreground envelope", async () => {
        const envelope = {
            runId: "run-error",
            status: "error" as const,
            error: "workflow failed",
            snapshot: { completed: 1 },
        };
        const session = new CopilotSession("session-error", {
            sendRequest: vi.fn(async () => envelope),
        } as never);

        const error = await session.workflow.run("failing").catch((caught: unknown) => caught);
        expect(error).toBeInstanceOf(WorkflowRunError);
        expect((error as WorkflowRunError).envelope).toBe(envelope);
    });
});
