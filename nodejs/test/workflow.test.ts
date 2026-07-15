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

    it("builds the workflow context with args, progress, signal, and the joined session identity", async () => {
        process.env.SESSION_ID = "session-context";
        const sendRequest = vi.fn(async (method: string) => {
            if (method === "session.workflow.log") {
                return {};
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
                return { ok: true };
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
        expect(context.signal).toBeInstanceOf(AbortSignal);
        expect(executeResult).toEqual({ result: { ok: true } });
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
