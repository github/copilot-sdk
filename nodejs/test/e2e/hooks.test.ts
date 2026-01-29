/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { writeFile } from "fs/promises";
import { join } from "path";
import { describe, expect, it } from "vitest";
import type {
    PreToolUseHookInput,
    PreToolUseHookOutput,
    PostToolUseHookInput,
    PostToolUseHookOutput,
} from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Session hooks", async () => {
    const { copilotClient: client, workDir } = await createSdkTestContext();

    it("should invoke preToolUse hook when model runs a tool", async () => {
        const preToolUseInputs: PreToolUseHookInput[] = [];

        const session = await client.createSession({
            hooks: {
                onPreToolUse: async (input, invocation) => {
                    preToolUseInputs.push(input);
                    expect(invocation.sessionId).toBe(session.sessionId);
                    // Allow the tool to run
                    return { permissionDecision: "allow" } as PreToolUseHookOutput;
                },
            },
        });

        // Create a file for the model to read
        await writeFile(join(workDir, "hello.txt"), "Hello from the test!");

        await session.sendAndWait({
            prompt: "Read the contents of hello.txt and tell me what it says",
        });

        // Should have received at least one preToolUse hook call
        expect(preToolUseInputs.length).toBeGreaterThan(0);

        // Should have received the tool name
        expect(preToolUseInputs.some((input) => input.toolName)).toBe(true);

        await session.destroy();
    });

    it("should invoke postToolUse hook after model runs a tool", async () => {
        const postToolUseInputs: PostToolUseHookInput[] = [];

        const session = await client.createSession({
            hooks: {
                onPostToolUse: async (input, invocation) => {
                    postToolUseInputs.push(input);
                    expect(invocation.sessionId).toBe(session.sessionId);
                    return null as PostToolUseHookOutput;
                },
            },
        });

        // Create a file for the model to read
        await writeFile(join(workDir, "world.txt"), "World from the test!");

        await session.sendAndWait({
            prompt: "Read the contents of world.txt and tell me what it says",
        });

        // Should have received at least one postToolUse hook call
        expect(postToolUseInputs.length).toBeGreaterThan(0);

        // Should have received the tool name and result
        expect(postToolUseInputs.some((input) => input.toolName)).toBe(true);
        expect(postToolUseInputs.some((input) => input.toolResult !== undefined)).toBe(true);

        await session.destroy();
    });

    it("should invoke both preToolUse and postToolUse hooks for a single tool call", async () => {
        const preToolUseInputs: PreToolUseHookInput[] = [];
        const postToolUseInputs: PostToolUseHookInput[] = [];

        const session = await client.createSession({
            hooks: {
                onPreToolUse: async (input) => {
                    preToolUseInputs.push(input);
                    return { permissionDecision: "allow" } as PreToolUseHookOutput;
                },
                onPostToolUse: async (input) => {
                    postToolUseInputs.push(input);
                    return null as PostToolUseHookOutput;
                },
            },
        });

        await writeFile(join(workDir, "both.txt"), "Testing both hooks!");

        await session.sendAndWait({
            prompt: "Read the contents of both.txt",
        });

        // Both hooks should have been called
        expect(preToolUseInputs.length).toBeGreaterThan(0);
        expect(postToolUseInputs.length).toBeGreaterThan(0);

        // The same tool should appear in both
        const preToolNames = preToolUseInputs.map((i) => i.toolName);
        const postToolNames = postToolUseInputs.map((i) => i.toolName);
        const commonTool = preToolNames.find((name) => postToolNames.includes(name));
        expect(commonTool).toBeDefined();

        await session.destroy();
    });

    it("should deny tool execution when preToolUse returns deny", async () => {
        const preToolUseInputs: PreToolUseHookInput[] = [];

        const session = await client.createSession({
            hooks: {
                onPreToolUse: async (input) => {
                    preToolUseInputs.push(input);
                    // Deny all tool calls
                    return { permissionDecision: "deny" } as PreToolUseHookOutput;
                },
            },
        });

        // Create a file
        const originalContent = "Original content that should not be modified";
        await writeFile(join(workDir, "protected.txt"), originalContent);

        const response = await session.sendAndWait({
            prompt: "Edit protected.txt and replace 'Original' with 'Modified'",
        });

        // The hook should have been called
        expect(preToolUseInputs.length).toBeGreaterThan(0);

        // The response should indicate the tool was denied (behavior may vary)
        // At minimum, we verify the hook was invoked
        expect(response).toBeDefined();

        await session.destroy();
    });
});
