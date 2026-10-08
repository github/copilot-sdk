/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { existsSync } from "node:fs";
import { randomUUID } from "node:crypto";
import { join } from "node:path";
import { beforeEach, describe, expect, it, onTestFailed } from "vitest";
import {
    CopilotRequestHandler,
    type CopilotRequestContext,
    type PermissionRequest,
    type SessionEvent,
} from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { getNextEventOfType } from "./harness/sdkTestHelper.js";
import { isByokBackend } from "./harness/testBackend.js";

const MODEL = "claude-sonnet-5";
const COMPLETION = "SDK_ASSISTED_TURN_FINISHED";

class AssistedPermissionRequestHandler extends CopilotRequestHandler {
    judgeCalls = 0;
    requiresApproval = false;
    directoryName = "";

    protected override async sendRequest(
        request: Request,
        context: CopilotRequestContext
    ): Promise<Response> {
        const url = new URL(request.url);
        if (url.pathname.endsWith("/models")) {
            return Response.json({
                data: [
                    {
                        id: MODEL,
                        name: MODEL,
                        vendor: "Anthropic",
                        model_picker_enabled: true,
                        supported_endpoints: ["/chat/completions"],
                        capabilities: {
                            type: "chat",
                            family: MODEL,
                            tokenizer: "o200k_base",
                            limits: { max_context_window_tokens: 128000, max_output_tokens: 8192 },
                            supports: { streaming: true, tool_calls: true, vision: false },
                        },
                    },
                ],
            });
        }
        if (!url.pathname.endsWith("/chat/completions")) {
            return super.sendRequest(request, context);
        }
        const body: {
            messages: { role: string; content: unknown }[];
            tools?: { function: { name: string } }[];
            stream?: boolean;
        } = await request.json();
        const isJudge = JSON.stringify(body.messages).includes("exactly one line");
        let message: object;
        if (isJudge) {
            this.judgeCalls++;
            message = {
                role: "assistant",
                content: this.requiresApproval
                    ? "REQUIRE_APPROVAL: A person must approve this action."
                    : "ALLOW: The action creates a workspace-local fixture.",
            };
        } else if (
            JSON.stringify(body.messages.findLast((message) => message.role === "user"))?.includes(
                "ASSISTED_SETUP_ONLY"
            )
        ) {
            message = { role: "assistant", content: "SDK_ASSISTED_SETUP_READY" };
        } else if (body.messages.some((message) => message.role === "tool")) {
            message = {
                role: "assistant",
                content: COMPLETION,
                tool_calls: [
                    {
                        id: "finish-task",
                        type: "function",
                        function: {
                            name: "task_complete",
                            arguments: JSON.stringify({ summary: COMPLETION }),
                        },
                    },
                ],
            };
        } else {
            const shell = body.tools?.find((tool) =>
                ["bash", "powershell"].includes(tool.function.name)
            )?.function.name;
            if (!shell) {
                throw new Error("The runtime did not offer a shell tool");
            }
            message = {
                role: "assistant",
                content: "",
                tool_calls: [
                    {
                        id: `assisted-shell-${body.messages.filter((message) => message.role === "tool").length}`,
                        type: "function",
                        function: {
                            name: shell,
                            arguments: JSON.stringify({
                                command: `mkdir ${this.directoryName}`,
                                description: "Create a fixture directory",
                                initial_wait: 30,
                            }),
                        },
                    },
                ],
            };
        }
        const finishReason = "tool_calls" in message ? "tool_calls" : "stop";
        const completion = {
            id: "assisted-completion",
            model: MODEL,
            created: 1,
            choices: [
                {
                    index: 0,
                    ...(body.stream ? { delta: message } : { message }),
                    finish_reason: finishReason,
                },
            ],
            usage: { prompt_tokens: 10, completion_tokens: 10, total_tokens: 20 },
        };
        return new Response(
            body.stream
                ? `data: ${JSON.stringify({ ...completion, object: "chat.completion.chunk" })}\n\ndata: [DONE]\n\n`
                : JSON.stringify({ ...completion, object: "chat.completion" }),
            {
                headers: { "content-type": body.stream ? "text/event-stream" : "application/json" },
            }
        );
    }
}

// Assisted uses a CAPI judge; this suite owns its deterministic model responses.
describe.skipIf(isByokBackend)("Autopilot Assisted SDK permissions", async () => {
    const handler = new AssistedPermissionRequestHandler();
    const { copilotClient: client, workDir } = await createSdkTestContext({
        // CAPI model turns are not supported by the current in-process test transport.
        useStdio: true,
        copilotClientOptions: { requestHandler: handler },
    });
    beforeEach(() => {
        handler.judgeCalls = 0;
        handler.requiresApproval = false;
        handler.directoryName = `assisted-result-${randomUUID()}`;
    });

    it.each([
        [false, "approve-once"],
        [true, "approve-once"],
        [false, "reject"],
    ] as const)("honors host adjudication (resume=%s, result=%s)", async (resume, result) => {
        let phase = "creating the session";
        const outputDirectory = join(workDir, handler.directoryName);
        expect(existsSync(outputDirectory)).toBe(false);
        const requests: PermissionRequest[] = [];
        const events: SessionEvent[] = [];
        onTestFailed(() =>
            console.error(
                JSON.stringify(
                    {
                        judgeCalls: handler.judgeCalls,
                        phase,
                        requests,
                        events: events
                            .filter((event) =>
                                [
                                    "permission.requested",
                                    "permission.completed",
                                    "tool.execution_start",
                                    "tool.execution_complete",
                                    "session.task_complete",
                                    "session.permission_recovery",
                                    "session.error",
                                ].includes(event.type)
                            )
                            .slice(-10),
                    },
                    null,
                    2
                )
            )
        );
        const onPermissionRequest = (request: PermissionRequest) => {
            requests.push(request);
            return { kind: result };
        };
        let session = await client.createSession({
            model: MODEL,
            workingDirectory: workDir,
            featureFlags: { AUTO_APPROVAL: true },
            onPermissionRequest,
        });
        if (resume) {
            phase = "initializing before resume";
            await session.sendAndWait({
                prompt: "Initialize this session. ASSISTED_SETUP_ONLY",
            });
            const sessionId = session.sessionId;
            await session.disconnect();
            session = await client.resumeSession(sessionId, {
                onPermissionRequest,
                featureFlags: { AUTO_APPROVAL: true },
            });
        }
        try {
            phase = "selecting Autopilot";
            await session.rpc.mode.set({ mode: "autopilot" });
            phase = "selecting Assisted";
            const permissionMode = await session.rpc.permissions.setMode({
                mode: "assisted",
                assistedApprovalModel: MODEL,
                source: "rpc",
            });
            expect(permissionMode).toMatchObject({ success: true, mode: "assisted" });
            session.on((event) => events.push(event));
            phase = "running the protected operation";
            // sendAndWait deliberately ignores Autopilot idle; wait for task completion instead.
            const completion = getNextEventOfType(session, "session.task_complete");
            const [, completed] = await Promise.all([
                session.send({ prompt: "Create the requested workspace fixture." }),
                completion,
            ]);
            expect(completed.data).toMatchObject({ success: true, summary: COMPLETION });
            expect(
                events.filter((event) => event.type === "assistant.message").at(-1)?.data.content
            ).toBe(COMPLETION);
            expect(handler.judgeCalls).toBe(1);
            expect(requests).toHaveLength(1);
            expect(requests[0]?.kind).toBe("shell");
            const permissionEvents = events.filter(
                (event) => event.type === "permission.requested"
            );
            expect(permissionEvents).toHaveLength(1);
            expect(permissionEvents[0]?.data.promptRequest?.assistedApproval?.recommendation).toBe(
                "approve"
            );
            expect(events.filter((event) => event.type === "session.permission_recovery")).toEqual(
                []
            );
            const completions = events.filter(
                (event) =>
                    event.type === "tool.execution_complete" &&
                    event.data.toolCallId.startsWith("assisted-")
            );
            expect(completions).toHaveLength(1);
            expect(completions[0]?.data.success).toBe(result === "approve-once");
            expect(existsSync(outputDirectory)).toBe(result === "approve-once");
        } finally {
            await session.disconnect();
        }
    });

    it.each([false, true])(
        "delivers a human-required recommendation to the SDK provider (resume=%s)",
        async (resume) => {
            const outputDirectory = join(workDir, handler.directoryName);
            expect(existsSync(outputDirectory)).toBe(false);
            handler.requiresApproval = true;
            const requests: PermissionRequest[] = [];
            const events: SessionEvent[] = [];
            const onPermissionRequest = (request: PermissionRequest) => {
                requests.push(request);
                return { kind: "approve-once" } as const;
            };
            let session = await client.createSession({
                model: MODEL,
                workingDirectory: workDir,
                featureFlags: { AUTO_APPROVAL: true },
                onPermissionRequest,
            });
            if (resume) {
                await session.sendAndWait({
                    prompt: "Initialize this session. ASSISTED_SETUP_ONLY",
                });
                const sessionId = session.sessionId;
                await session.disconnect();
                session = await client.resumeSession(sessionId, {
                    onPermissionRequest,
                    featureFlags: { AUTO_APPROVAL: true },
                });
            }
            try {
                await session.rpc.mode.set({ mode: "autopilot" });
                const permissionMode = await session.rpc.permissions.setMode({
                    mode: "assisted",
                    assistedApprovalModel: MODEL,
                    source: "rpc",
                });
                expect(permissionMode).toMatchObject({ success: true, mode: "assisted" });
                session.on((event) => events.push(event));
                const completion = getNextEventOfType(session, "session.task_complete");
                const [, completed] = await Promise.all([
                    session.send({ prompt: "Create the requested workspace fixture." }),
                    completion,
                ]);
                expect(completed.data).toMatchObject({ success: true, summary: COMPLETION });
                expect(
                    events.filter((event) => event.type === "assistant.message").at(-1)?.data
                        .content
                ).toBe(COMPLETION);
                expect(handler.judgeCalls).toBe(1);
                expect(requests).toHaveLength(1);
                const permissionEvents = events.filter(
                    (event) => event.type === "permission.requested"
                );
                expect(permissionEvents).toHaveLength(1);
                expect(permissionEvents[0]?.data.recoveryEpisodeId).toBeUndefined();
                expect(
                    permissionEvents[0]?.data.promptRequest?.assistedApproval?.recommendation
                ).toBe("requireApproval");
                expect(
                    events
                        .filter((event) => event.type === "session.permission_recovery")
                        .map((event) => event.data.status)
                ).toEqual([]);
                expect(
                    events
                        .filter(
                            (event) =>
                                event.type === "tool.execution_complete" &&
                                event.data.toolCallId.startsWith("assisted-")
                        )
                        .map((event) => event.data.success)
                ).toEqual([true]);
                expect(existsSync(outputDirectory)).toBe(true);
            } finally {
                await session.disconnect();
            }
        }
    );
});
