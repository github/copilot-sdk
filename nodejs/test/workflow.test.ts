/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it, vi } from "vitest";
import { ErrorCodes, ResponseError } from "vscode-jsonrpc/node.js";
import { CopilotClient } from "../src/client.js";
import { CopilotSession } from "../src/session.js";
import { defineFactory } from "../src/factory.js";
import { defineWorkflow } from "../src/workflow.js";

function runUpdatedEvent(runId: string, revision: number): Record<string, unknown> {
    return {
        type: "factory.run_updated",
        id: `event-${runId}-${revision}`,
        parentId: null,
        timestamp: new Date().toISOString(),
        ephemeral: true,
        data: { runId, revision },
    };
}

describe("dynamic workflows", () => {
    it("defines an independent stable handle and executes through workflow callbacks", async () => {
        const meta = {
            name: "review",
            description: "Review a change",
            phases: [{ title: "Inspect" }],
        };
        const run = vi.fn(async ({ args }: { args: unknown }) => args);
        const workflow = defineWorkflow({ meta, run });

        expect(workflow.meta).toEqual(meta);
        expect(workflow.meta).not.toBe(meta);
        expect(Object.isFrozen(workflow)).toBe(true);
        expect(Object.isFrozen(workflow.meta)).toBe(true);

        const session = new CopilotSession("session-workflow", {} as never);
        session.registerWorkflows([workflow]);
        expect(session.clientSessionApis.workflow).toBeDefined();
        expect(session.clientSessionApis.factory).toBeUndefined();

        await expect(
            session.clientSessionApis.workflow!.execute({
                sessionId: session.sessionId,
                name: "review",
                runId: "run-1",
                executionToken: "attempt-1",
                args: { path: "src" },
            })
        ).resolves.toEqual({ result: { path: "src" } });
        expect(run).toHaveBeenCalledOnce();
    });

    it("uses the workflow RPC namespace and workflow wire fields", async () => {
        const sendRequest = vi.fn(async (method: string, params: Record<string, unknown>) => {
            if (method === "session.workflow.run") {
                return {
                    runId: "run-workflow",
                    status: "completed",
                    result: params.args,
                };
            }
            if (method === "session.workflow.agent") {
                return { result: "reviewed" };
            }
            throw new Error(`Unexpected method: ${method}`);
        });
        const session = new CopilotSession("session-workflow-rpc", { sendRequest } as never);
        const workflow = defineWorkflow({
            meta: {
                name: "review",
                description: "Review a change",
                phases: [],
            },
            run: async ({ agent }) => agent("Review this"),
        });
        session.registerWorkflows([workflow]);

        await expect(
            session.workflow.run("review", {
                args: { path: "src" },
                notifyOnComplete: false,
            })
        ).resolves.toMatchObject({
            status: "completed",
            result: { path: "src" },
        });

        await expect(
            session.clientSessionApis.workflow!.execute({
                sessionId: session.sessionId,
                name: "review",
                runId: "run-workflow",
                executionToken: "attempt-1",
                args: {},
            })
        ).resolves.toEqual({ result: "reviewed" });

        expect(sendRequest).toHaveBeenCalledWith("session.workflow.agent", {
            sessionId: session.sessionId,
            workflowRunId: "run-workflow",
            executionToken: "attempt-1",
            prompt: "Review this",
            opts: {},
        });
        expect(sendRequest.mock.calls.some(([method]) => String(method).includes("factory"))).toBe(
            false
        );
    });

    it("serializes workflow metadata and rejects mixed contribution generations", async () => {
        const client = new CopilotClient();
        const workflow = defineWorkflow({
            meta: {
                name: "review",
                description: "Review a change",
                phases: [{ title: "Inspect" }],
                limits: { maxTotalSubagents: 2 },
            },
            run: async () => ({ ok: true }),
        });
        const factory = defineFactory({
            meta: {
                name: "legacy",
                description: "Legacy factory",
                phases: [],
            },
            run: async () => ({ ok: true }),
        });
        const sendRequest = vi.fn(async (method: string, params: Record<string, unknown>) => {
            if (method === "session.resume") {
                return { sessionId: params.sessionId };
            }
            throw new Error(`Unexpected method: ${method}`);
        });
        (client as never as { connection: unknown }).connection = { sendRequest };

        await client.resumeSessionForExtension(
            "session-workflow-registration",
            { onPermissionRequest: () => ({ kind: "approved" }) },
            { workflows: [workflow] }
        );

        const payload = sendRequest.mock.calls.find(
            ([method]) => method === "session.resume"
        )![1] as {
            factories?: unknown[];
            workflows?: unknown[];
        };
        expect(payload.workflows).toEqual([workflow.meta]);
        expect(payload.factories).toBeUndefined();
        expect(payload.workflows?.[0]).not.toHaveProperty("run");

        await expect(
            client.resumeSessionForExtension(
                "session-mixed-registration",
                { onPermissionRequest: () => ({ kind: "approved" }) },
                { factories: [factory], workflows: [workflow] }
            )
        ).rejects.toThrow("cannot include both factories and workflows");
    });

    it("preserves the legacy factory-array extension call shape", async () => {
        const client = new CopilotClient();
        const factory = defineFactory({
            meta: {
                name: "legacy",
                description: "Legacy factory",
                phases: [],
            },
            run: async () => ({ ok: true }),
        });
        const sendRequest = vi.fn(async (method: string, params: Record<string, unknown>) => {
            if (method === "session.resume") {
                return { sessionId: params.sessionId };
            }
            throw new Error(`Unexpected method: ${method}`);
        });
        (client as never as { connection: unknown }).connection = { sendRequest };

        await client.resumeSessionForExtension(
            "session-legacy-factory-registration",
            { onPermissionRequest: () => ({ kind: "approved" }) },
            [factory]
        );

        const payload = sendRequest.mock.calls.find(
            ([method]) => method === "session.resume"
        )![1] as {
            factories?: unknown[];
            workflows?: unknown[];
        };
        expect(payload.factories).toEqual([factory.meta]);
        expect(payload.workflows).toBeUndefined();
    });

    it("uses workflow-specific validation errors", async () => {
        const workflow = defineWorkflow({
            meta: {
                name: "invalid-result",
                description: "Returns an invalid value",
                phases: [],
            },
            run: async () => ({ nested: 1n }) as never,
        });
        const session = new CopilotSession("session-workflow-validation", {} as never);
        session.registerWorkflows([workflow]);

        await expect(
            session.clientSessionApis.workflow!.execute({
                sessionId: session.sessionId,
                name: "invalid-result",
                runId: "run-invalid",
                executionToken: "attempt-1",
                args: {},
            })
        ).rejects.toMatchObject({
            message: "Workflow result contains a function, symbol, or BigInt at $.nested",
            data: {
                code: "workflow_result_not_json",
                category: "unsupported_type",
            },
        });
    });

    it.each([
        [[{ title: "" }], "must not be empty"],
        [[{ title: "Inspect" }, { title: "Inspect" }], "declared more than once"],
    ])("rejects invalid workflow phases", (phases, message) => {
        expect(() =>
            defineWorkflow({
                meta: {
                    name: "invalid-phases",
                    description: "Invalid phases",
                    phases,
                },
                run: async () => null,
            })
        ).toThrow(message);
    });

    it.each([
        [{ maxTotalSubagents: 0 }, "positive integer"],
        [{ timeoutSeconds: Number.POSITIVE_INFINITY }, "positive, finite"],
        [{ maxAiCredits: Number.NaN }, "positive, finite"],
    ])("rejects invalid declared workflow limits", (limits, message) => {
        expect(() =>
            defineWorkflow({
                meta: {
                    name: "invalid-limits",
                    description: "Invalid limits",
                    phases: [],
                    limits,
                },
                run: async () => null,
            })
        ).toThrow(message);
    });

    it("rejects duplicate workflow names", () => {
        const first = defineWorkflow({
            meta: { name: "duplicate", description: "First", phases: [] },
            run: async () => null,
        });
        const second = defineWorkflow({
            meta: { name: "duplicate", description: "Second", phases: [] },
            run: async () => null,
        });
        const session = new CopilotSession("session-duplicate-workflow", {} as never);
        expect(() => session.registerWorkflows([first, second])).toThrow(
            'Duplicate workflow name "duplicate"'
        );
    });

    it("returns workflow-specific dispatch and resume errors", async () => {
        const sendRequest = vi.fn(async (method: string, params: { runId?: string }) => {
            if (method === "session.workflow.resume") {
                throw new ResponseError(ErrorCodes.InvalidRequest, "not resumable", {
                    code:
                        params.runId === "run-not-resumable"
                            ? "workflow_run_not_resumable"
                            : "workflow_storage_unavailable",
                });
            }
            throw new Error(`Unexpected method: ${method}`);
        });
        const session = new CopilotSession("session-workflow-errors", {
            sendRequest,
        } as never);

        await expect(session.workflow.resume("run-1")).rejects.toMatchObject({
            name: "WorkflowResumeError",
            code: "workflow_storage_unavailable",
        });
        await expect(session.workflow.resume("run-not-resumable")).rejects.toMatchObject({
            name: "WorkflowResumeError",
            code: "workflow_run_not_resumable",
        });

        session.registerWorkflows([]);
        expect(session.clientSessionApis.workflow).toBeUndefined();
        session.registerWorkflows([
            defineWorkflow({
                meta: { name: "known", description: "Known", phases: [] },
                run: async () => null,
            }),
        ]);
        await expect(
            session.clientSessionApis.workflow!.execute({
                sessionId: session.sessionId,
                name: "missing",
                runId: "run-missing",
                executionToken: "attempt-1",
                args: {},
            })
        ).rejects.toMatchObject({
            data: { code: "workflow_not_found", name: "missing" },
        });
    });

    it.each(["run", "resume", "pause"] as const)(
        "blocks workflow.%s while a workflow body is active",
        async (operation) => {
            const sendRequest = vi.fn(async () => ({
                runId: "nested",
                status: "completed",
            }));
            const session = new CopilotSession(`session-workflow-${operation}`, {
                sendRequest,
            } as never);
            session.registerWorkflows([
                defineWorkflow({
                    meta: { name: operation, description: operation, phases: [] },
                    run: async ({ session: joinedSession }) => {
                        if (operation === "run") {
                            await joinedSession.workflow.run("nested");
                        } else if (operation === "resume") {
                            await joinedSession.workflow.resume("nested");
                        } else {
                            await joinedSession.workflow.pause("nested");
                        }
                        return null;
                    },
                }),
            ]);

            await expect(
                session.clientSessionApis.workflow!.execute({
                    sessionId: session.sessionId,
                    name: operation,
                    runId: `${operation}-run`,
                    executionToken: "attempt-1",
                    args: {},
                })
            ).rejects.toThrow(`workflow.run, workflow.resume, and workflow.pause`);
            expect(sendRequest).not.toHaveBeenCalled();
        }
    );

    it("settles already-terminal workflow runs", async () => {
        const envelope = {
            runId: "run-complete",
            status: "completed" as const,
            result: "done",
        };
        const sendRequest = vi.fn(async () => envelope);
        const session = new CopilotSession("session-workflow-settle", {
            sendRequest,
        } as never);

        await expect(session.workflow.waitForRun("run-complete")).resolves.toEqual(envelope);
        expect(sendRequest).toHaveBeenCalledWith("session.workflow.getRun", {
            sessionId: session.sessionId,
            runId: "run-complete",
        });
    });

    it("waits for workflow settlement through compatibility invalidation events", async () => {
        const running = { runId: "run-wait", status: "running" as const };
        const terminal = {
            runId: "run-wait",
            status: "completed" as const,
            result: "done",
        };
        let current = running as typeof running | typeof terminal;
        const sendRequest = vi.fn(async () => current);
        const session = new CopilotSession("session-workflow-wait", {
            sendRequest,
        } as never);

        const settled = session.workflow.waitForRun("run-wait");
        await vi.waitFor(() => expect(sendRequest).toHaveBeenCalledTimes(1));
        current = terminal;
        (session as never as { _dispatchEvent(event: unknown): void })._dispatchEvent(
            runUpdatedEvent("run-wait", 1)
        );

        await expect(settled).resolves.toEqual(terminal);
        expect(sendRequest).toHaveBeenCalledTimes(2);
    });
});
