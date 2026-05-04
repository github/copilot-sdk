/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { readFile, writeFile } from "fs/promises";
import { join } from "path";
import { describe, expect, it } from "vitest";
import { z } from "zod";
import type {
    PermissionRequest,
    PermissionRequestResult,
    ToolResultObject,
} from "../../src/index.js";
import { approveAll, defineTool } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { getFinalAssistantMessage, getNextEventOfType } from "./harness/sdkTestHelper.js";

describe("Permission callbacks", async () => {
    const { copilotClient: client, workDir } = await createSdkTestContext();

    it("should invoke permission handler for write operations", async () => {
        const permissionRequests: PermissionRequest[] = [];

        const session = await client.createSession({
            onPermissionRequest: (request, invocation) => {
                permissionRequests.push(request);
                expect(invocation.sessionId).toBe(session.sessionId);

                // Approve the permission
                const result: PermissionRequestResult = { kind: "approve-once" };
                return result;
            },
        });

        await writeFile(join(workDir, "test.txt"), "original content");

        await session.sendAndWait({
            prompt: "Edit test.txt and replace 'original' with 'modified'",
        });

        // Should have received at least one permission request
        expect(permissionRequests.length).toBeGreaterThan(0);

        // Should include write permission request
        const writeRequests = permissionRequests.filter((req) => req.kind === "write");
        expect(writeRequests.length).toBeGreaterThan(0);

        await session.disconnect();
    });

    it("should deny permission when handler returns denied", async () => {
        const session = await client.createSession({
            onPermissionRequest: () => {
                return { kind: "reject" };
            },
        });

        const originalContent = "protected content";
        const testFile = join(workDir, "protected.txt");
        await writeFile(testFile, originalContent);

        await session.sendAndWait({
            prompt: "Edit protected.txt and replace 'protected' with 'hacked'.",
        });

        // Verify the file was NOT modified
        const content = await readFile(testFile, "utf-8");
        expect(content).toBe(originalContent);

        await session.disconnect();
    });

    it("should deny tool operations when handler explicitly denies", async () => {
        let permissionDenied = false;

        const session = await client.createSession({
            onPermissionRequest: () => ({
                kind: "user-not-available",
            }),
        });
        session.on((event) => {
            if (
                event.type === "tool.execution_complete" &&
                !event.data.success &&
                event.data.error?.message.includes("Permission denied")
            ) {
                permissionDenied = true;
            }
        });

        await session.sendAndWait({ prompt: "Run 'node --version'" });

        expect(permissionDenied).toBe(true);

        await session.disconnect();
    });

    it("should deny tool operations when handler explicitly denies after resume", async () => {
        const session1 = await client.createSession({ onPermissionRequest: approveAll });
        const sessionId = session1.sessionId;
        await session1.sendAndWait({ prompt: "What is 1+1?" });

        const session2 = await client.resumeSession(sessionId, {
            onPermissionRequest: () => ({
                kind: "user-not-available",
            }),
        });
        let permissionDenied = false;
        session2.on((event) => {
            if (
                event.type === "tool.execution_complete" &&
                !event.data.success &&
                event.data.error?.message.includes("Permission denied")
            ) {
                permissionDenied = true;
            }
        });

        await session2.sendAndWait({ prompt: "Run 'node --version'" });

        expect(permissionDenied).toBe(true);

        await session2.disconnect();
    });

    it("should work with approve-all permission handler", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        const message = await session.sendAndWait({
            prompt: "What is 2+2?",
        });
        expect(message?.data.content).toContain("4");

        await session.disconnect();
    });

    it("should handle async permission handler", async () => {
        const permissionRequests: PermissionRequest[] = [];

        const session = await client.createSession({
            onPermissionRequest: async (request, _invocation) => {
                permissionRequests.push(request);

                await Promise.resolve();

                return { kind: "approve-once" };
            },
        });

        await session.sendAndWait({
            prompt: "Run 'echo test' and tell me what happens",
        });

        expect(permissionRequests.length).toBeGreaterThan(0);

        await session.disconnect();
    });

    it("should resume session with permission handler", async () => {
        const permissionRequests: PermissionRequest[] = [];

        // Create initial session
        const session1 = await client.createSession({ onPermissionRequest: approveAll });
        const sessionId = session1.sessionId;
        await session1.sendAndWait({ prompt: "What is 1+1?" });

        // Resume with permission handler
        const session2 = await client.resumeSession(sessionId, {
            onPermissionRequest: (request) => {
                permissionRequests.push(request);
                return { kind: "approve-once" };
            },
        });

        await session2.sendAndWait({
            prompt: "Run 'echo resumed' for me",
        });

        // Should have permission requests from resumed session
        expect(permissionRequests.length).toBeGreaterThan(0);

        await session2.disconnect();
    });

    it("should handle permission handler errors gracefully", async () => {
        const session = await client.createSession({
            onPermissionRequest: () => {
                throw new Error("Handler error");
            },
        });

        const message = await session.sendAndWait({
            prompt: "Run 'echo test'. If you can't, say 'failed'.",
        });

        // Should handle the error and deny permission
        expect(message?.data.content?.toLowerCase()).toMatch(/fail|cannot|unable|permission/);

        await session.disconnect();
    });

    it("should receive toolCallId in permission requests", async () => {
        let receivedToolCallId = false;

        const session = await client.createSession({
            onPermissionRequest: (request) => {
                if (request.toolCallId) {
                    receivedToolCallId = true;
                    expect(typeof request.toolCallId).toBe("string");
                    expect(request.toolCallId.length).toBeGreaterThan(0);
                }
                return { kind: "approve-once" };
            },
        });

        await session.sendAndWait({
            prompt: "Run 'echo test'",
        });

        expect(receivedToolCallId).toBe(true);

        await session.disconnect();
    });

    it("should wait for slow permission handler", async () => {
        let handlerStartedResolve: () => void;
        let releaseHandler: () => void;
        let targetToolCallId: string | undefined;

        const handlerStarted = new Promise<void>((resolve) => {
            let resolved = false;
            handlerStartedResolve = () => {
                if (!resolved) {
                    resolved = true;
                    resolve();
                }
            };
        });
        const handlerGate = new Promise<void>((resolve) => {
            releaseHandler = resolve;
        });

        let permissionCount = 0;
        const lifecycle: Array<{ phase: string; toolCallId?: string }> = [];

        const session = await client.createSession({
            onPermissionRequest: async (
                request: PermissionRequest
            ): Promise<PermissionRequestResult> => {
                permissionCount++;
                targetToolCallId = request.toolCallId;
                lifecycle.push({ phase: "permission-start", toolCallId: request.toolCallId });
                handlerStartedResolve!();
                await handlerGate;
                lifecycle.push({ phase: "permission-complete", toolCallId: request.toolCallId });
                return { kind: "approve-once" };
            },
        });
        session.on((event) => {
            if (event.type === "tool.execution_start") {
                lifecycle.push({ phase: "tool-start", toolCallId: event.data.toolCallId });
            } else if (event.type === "tool.execution_complete") {
                lifecycle.push({ phase: "tool-complete", toolCallId: event.data.toolCallId });
            }
        });

        const sessionDone = getFinalAssistantMessage(session);

        void session.send({ prompt: "Run 'echo slow_handler_test'" });

        // Wait for permission handler to be invoked
        await handlerStarted;
        expect(
            lifecycle.some(
                (entry) =>
                    entry.phase === "tool-complete" &&
                    (!targetToolCallId || entry.toolCallId === targetToolCallId)
            )
        ).toBe(false);

        // Handler is blocked — release it now
        releaseHandler!();

        const answer = await sessionDone;
        expect(answer.data.content).toContain("slow_handler_test");
        expect(permissionCount).toBe(1);
        const permissionCompleteIndex = lifecycle.findIndex(
            (entry) =>
                entry.phase === "permission-complete" &&
                (!targetToolCallId || entry.toolCallId === targetToolCallId)
        );
        const toolCompleteIndex = lifecycle.findIndex(
            (entry) =>
                entry.phase === "tool-complete" &&
                (!targetToolCallId || entry.toolCallId === targetToolCallId)
        );
        expect(permissionCompleteIndex).toBeGreaterThanOrEqual(0);
        expect(toolCompleteIndex).toBeGreaterThanOrEqual(0);
        expect(permissionCompleteIndex).toBeLessThan(toolCompleteIndex);

        await session.disconnect();
    });

    it("should handle concurrent permission requests from parallel tools", async () => {
        let resolveFirst: (() => void) | undefined;
        let resolveSecond: (() => void) | undefined;
        const firstArrived = new Promise<void>((r) => (resolveFirst = r));
        const secondArrived = new Promise<void>((r) => (resolveSecond = r));
        let requestCount = 0;
        let firstToolCalled = false;
        let secondToolCalled = false;
        const permissionRequests: Array<PermissionRequest & { toolName?: string }> = [];
        const toolCompletions: string[] = [];

        const session = await client.createSession({
            tools: [
                defineTool("first_permission_tool", {
                    description: "First concurrent permission test tool",
                    parameters: z.object({}),
                    handler: async (): Promise<ToolResultObject> => {
                        firstToolCalled = true;
                        return {
                            textResultForLlm:
                                "first_permission_tool completed after permission approval",
                            resultType: "rejected",
                        };
                    },
                }),
                defineTool("second_permission_tool", {
                    description: "Second concurrent permission test tool",
                    parameters: z.object({}),
                    handler: async (): Promise<ToolResultObject> => {
                        secondToolCalled = true;
                        return {
                            textResultForLlm:
                                "second_permission_tool completed after permission approval",
                            resultType: "rejected",
                        };
                    },
                }),
            ],
            availableTools: ["first_permission_tool", "second_permission_tool"],
            onPermissionRequest: async (
                request: PermissionRequest
            ): Promise<PermissionRequestResult> => {
                permissionRequests.push(request as PermissionRequest & { toolName?: string });
                requestCount++;
                if (requestCount === 1) resolveFirst?.();
                if (requestCount === 2) resolveSecond?.();
                // Wait until both have arrived before approving
                await Promise.all([firstArrived, secondArrived]);
                return { kind: "approve-once" };
            },
        });
        session.on((event) => {
            if (event.type === "tool.execution_complete" && event.data.error?.message) {
                toolCompletions.push(event.data.error.message);
            }
        });

        const idle = getNextEventOfType(session, "session.idle");
        await session.send({
            prompt: "Call both first_permission_tool and second_permission_tool in the same turn. Do not call any other tools.",
        });
        await Promise.all([firstArrived, secondArrived]);
        await idle;

        expect(requestCount).toBe(2);
        expect(
            permissionRequests.some((request) => request.toolName === "first_permission_tool")
        ).toBe(true);
        expect(
            permissionRequests.some((request) => request.toolName === "second_permission_tool")
        ).toBe(true);
        expect(firstToolCalled).toBe(true);
        expect(secondToolCalled).toBe(true);
        expect(
            toolCompletions.some((message) =>
                message.includes("first_permission_tool completed after permission approval")
            )
        ).toBe(true);
        expect(
            toolCompletions.some((message) =>
                message.includes("second_permission_tool completed after permission approval")
            )
        ).toBe(true);

        await session.disconnect();
    });

    it("should deny permission with noresult kind", async () => {
        // With no-result, the TypeScript SDK does not send any response to the CLI's permission
        // request, leaving the tool execution pending. We verify the permission handler fires.
        let resolvePermissionCalled!: () => void;
        const permissionCalled = new Promise<void>((resolve) => {
            resolvePermissionCalled = resolve;
        });

        const session = await client.createSession({
            onPermissionRequest: (_request: PermissionRequest): PermissionRequestResult => {
                resolvePermissionCalled();
                return { kind: "no-result" };
            },
        });

        void session.send({ prompt: "Run 'node --version'" });

        await permissionCalled;

        await session.disconnect();
    });

    it("should short circuit permission handler when set approve all enabled", async () => {
        let handlerCalled = false;

        const session = await client.createSession({
            onPermissionRequest: (_request: PermissionRequest): PermissionRequestResult => {
                handlerCalled = true;
                return { kind: "approve-once" };
            },
        });

        // Enable approve-all server-side short circuit
        await session.rpc.permissions.setApproveAll({ enabled: true });

        try {
            const answer = await session.sendAndWait({
                prompt: "Run 'echo test' and tell me what happens",
            });
            expect(handlerCalled).toBe(false);
            expect(answer?.data.content).toContain("test");
        } finally {
            await session.rpc.permissions.setApproveAll({ enabled: false });
        }

        await session.disconnect();
    });
});
