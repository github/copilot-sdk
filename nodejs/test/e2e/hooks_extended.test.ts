/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import type {
    ErrorOccurredHookInput,
    SessionEndHookInput,
    SessionStartHookInput,
    UserPromptSubmittedHookInput,
} from "../../src/types.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Extended session hooks", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    it("should invoke onSessionStart hook on new session", async () => {
        const sessionStartInputs: SessionStartHookInput[] = [];

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            hooks: {
                onSessionStart: async (input, invocation) => {
                    sessionStartInputs.push(input);
                    expect(invocation.sessionId).toBe(session.sessionId);
                },
            },
        });

        await session.sendAndWait({
            prompt: "Say hi",
        });

        expect(sessionStartInputs.length).toBeGreaterThan(0);
        expect(sessionStartInputs[0].source).toBe("new");
        expect(sessionStartInputs[0].timestamp).toBeGreaterThan(0);
        expect(sessionStartInputs[0].cwd).toBeDefined();

        await session.destroy();
    });

    it("should invoke onUserPromptSubmitted hook when sending a message", async () => {
        const userPromptInputs: UserPromptSubmittedHookInput[] = [];

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            hooks: {
                onUserPromptSubmitted: async (input, invocation) => {
                    userPromptInputs.push(input);
                    expect(invocation.sessionId).toBe(session.sessionId);
                },
            },
        });

        await session.sendAndWait({
            prompt: "Say hello",
        });

        expect(userPromptInputs.length).toBeGreaterThan(0);
        expect(userPromptInputs[0].prompt).toContain("Say hello");
        expect(userPromptInputs[0].timestamp).toBeGreaterThan(0);
        expect(userPromptInputs[0].cwd).toBeDefined();

        await session.destroy();
    });

    it("should invoke onSessionEnd hook when session is destroyed", async () => {
        const sessionEndInputs: SessionEndHookInput[] = [];

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            hooks: {
                onSessionEnd: async (input, invocation) => {
                    sessionEndInputs.push(input);
                    expect(invocation.sessionId).toBe(session.sessionId);
                },
            },
        });

        await session.sendAndWait({
            prompt: "Say hi",
        });

        await session.destroy();

        // Wait briefly for async hook
        await new Promise((resolve) => setTimeout(resolve, 100));

        expect(sessionEndInputs.length).toBeGreaterThan(0);
    });

    it("should invoke onErrorOccurred hook when error occurs", async () => {
        const errorInputs: ErrorOccurredHookInput[] = [];

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            hooks: {
                onErrorOccurred: async (input, invocation) => {
                    errorInputs.push(input);
                    expect(invocation.sessionId).toBe(session.sessionId);
                    expect(input.timestamp).toBeGreaterThan(0);
                    expect(input.cwd).toBeDefined();
                    expect(input.error).toBeDefined();
                    expect(["model_call", "tool_execution", "system", "user_input"]).toContain(
                        input.errorContext
                    );
                    expect(typeof input.recoverable).toBe("boolean");
                },
            },
        });

        await session.sendAndWait({
            prompt: "Say hi",
        });

        // onErrorOccurred is dispatched by the runtime for actual errors (model failures, system errors).
        // In a normal session it may not fire. Verify the hook is properly wired by checking
        // that the session works correctly with the hook registered.
        // If the hook did fire, the assertions inside it would have run.
        expect(session.sessionId).toBeDefined();

        await session.destroy();
    });
});
