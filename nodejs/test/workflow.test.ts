/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { afterEach, describe, expect, it, onTestFinished, vi } from "vitest";
import { ResponseError } from "vscode-jsonrpc/node.js";
import { CopilotClient } from "../src/client.js";
import { joinSession } from "../src/extension.js";
import { CopilotSession } from "../src/session.js";
import { defineWorkflow, WorkflowRunError, type WorkflowDefinition } from "../src/workflow.js";

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

    it("dispatches workflow.execute by name and returns a structured unknown-name error", async () => {
        const run = vi.fn(async ({ args, log }) => {
            log("ignored in phase 2");
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
        const session = new CopilotSession("session-execute", {} as never);
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
