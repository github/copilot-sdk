/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { readFile, writeFile } from "fs/promises";
import { join } from "path";
import { describe, expect, it } from "vitest";
import type { PermissionRequest, PermissionRequestResult } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Permission callbacks", async () => {
    const { copilotClient: client, workDir } = await createSdkTestContext();

    it("should invoke permission handler for write operations", async () => {
        const permissionRequests: PermissionRequest[] = [];

        const session = await client.createSession({
            onPermissionRequest: (request, invocation) => {
                permissionRequests.push(request);
                expect(invocation.sessionId).toBe(session.sessionId);

                // Approve the permission
                const result: PermissionRequestResult = { kind: "approved" };
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

        await session.destroy();
    });

    it("should deny permission when handler returns denied", async () => {
        const session = await client.createSession({
            onPermissionRequest: () => {
                return { kind: "denied-interactively-by-user" };
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

        await session.destroy();
    });

    it("should work without permission handler (default behavior)", async () => {
        // Create session without onPermissionRequest handler
        const session = await client.createSession();

        const message = await session.sendAndWait({
            prompt: "What is 2+2?",
        });
        expect(message?.data.content).toContain("4");

        await session.destroy();
    });

    it("should handle async permission handler", async () => {
        const permissionRequests: PermissionRequest[] = [];

        const session = await client.createSession({
            onPermissionRequest: async (request, _invocation) => {
                permissionRequests.push(request);

                // Simulate async permission check (e.g., user prompt)
                await new Promise((resolve) => setTimeout(resolve, 10));

                return { kind: "approved" };
            },
        });

        await session.sendAndWait({
            prompt: "Run 'echo test' and tell me what happens",
        });

        expect(permissionRequests.length).toBeGreaterThan(0);

        await session.destroy();
    });

    it("should resume session with permission handler", async () => {
        const permissionRequests: PermissionRequest[] = [];

        // Create session without permission handler
        const session1 = await client.createSession();
        const sessionId = session1.sessionId;
        await session1.sendAndWait({ prompt: "What is 1+1?" });

        // Resume with permission handler
        const session2 = await client.resumeSession(sessionId, {
            onPermissionRequest: (request) => {
                permissionRequests.push(request);
                return { kind: "approved" };
            },
        });

        await session2.sendAndWait({
            prompt: "Run 'echo resumed' for me",
        });

        // Should have permission requests from resumed session
        expect(permissionRequests.length).toBeGreaterThan(0);

        await session2.destroy();
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

        await session.destroy();
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
                return { kind: "approved" };
            },
        });

        await session.sendAndWait({
            prompt: "Run 'echo test'",
        });

        expect(receivedToolCallId).toBe(true);

        await session.destroy();
    });
});
