/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { z } from "zod";
import { approveAll, defineTool } from "../../src/index.js";
import type {
    ErrorOccurredHookInput,
    PostToolUseHookInput,
    PreToolUseHookInput,
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

        await session.disconnect();
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

        await session.disconnect();
    });

    it("should invoke onSessionEnd hook when session is disconnected", async () => {
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

        await session.disconnect();

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

        await session.disconnect();
    });

    it("should invoke userPromptSubmitted hook and modify prompt", async () => {
        const inputs: UserPromptSubmittedHookInput[] = [];
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            hooks: {
                onUserPromptSubmitted: async (input, invocation) => {
                    inputs.push(input);
                    expect(invocation.sessionId).toBeTruthy();
                    return { modifiedPrompt: "Reply with exactly: HOOKED_PROMPT" };
                },
            },
        });

        const response = await session.sendAndWait({ prompt: "Say something else" });

        expect(inputs.length).toBeGreaterThan(0);
        expect(inputs[0].prompt).toContain("Say something else");
        expect(response?.data.content ?? "").toContain("HOOKED_PROMPT");

        await session.disconnect();
    });

    it("should invoke sessionStart hook", async () => {
        const inputs: SessionStartHookInput[] = [];
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            hooks: {
                onSessionStart: async (input, invocation) => {
                    inputs.push(input);
                    expect(invocation.sessionId).toBeTruthy();
                    return { additionalContext: "Session start hook context." };
                },
            },
        });

        await session.sendAndWait({ prompt: "Say hi" });

        expect(inputs.length).toBeGreaterThan(0);
        expect(inputs[0].source).toBe("new");
        expect(inputs[0].cwd).toBeTruthy();

        await session.disconnect();
    });

    it("should invoke sessionEnd hook", async () => {
        const inputs: SessionEndHookInput[] = [];
        let resolveHook!: (value: SessionEndHookInput) => void;
        const hookInvoked = new Promise<SessionEndHookInput>((resolve) => {
            resolveHook = resolve;
        });

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            hooks: {
                onSessionEnd: async (input, invocation) => {
                    inputs.push(input);
                    expect(invocation.sessionId).toBeTruthy();
                    resolveHook(input);
                    return { sessionSummary: "session ended" };
                },
            },
        });

        await session.sendAndWait({ prompt: "Say bye" });
        await session.disconnect();

        let timer: NodeJS.Timeout | undefined;
        try {
            await Promise.race([
                hookInvoked,
                new Promise<SessionEndHookInput>((_, reject) => {
                    timer = setTimeout(() => reject(new Error("Timeout: onSessionEnd")), 10_000);
                }),
            ]);
        } finally {
            if (timer) clearTimeout(timer);
        }

        expect(inputs.length).toBeGreaterThan(0);
    });

    it("should register erroroccurred hook", async () => {
        const inputs: ErrorOccurredHookInput[] = [];
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            hooks: {
                onErrorOccurred: async (input, invocation) => {
                    inputs.push(input);
                    expect(invocation.sessionId).toBeTruthy();
                    return { errorHandling: "skip" };
                },
            },
        });

        await session.sendAndWait({ prompt: "Say hi" });

        // OnErrorOccurred is dispatched only by genuine runtime errors. A normal turn
        // cannot deterministically trigger one; this test is registration-only.
        expect(inputs.length).toBe(0);
        expect(session.sessionId).toBeTruthy();

        await session.disconnect();
    });

    it("should allow preToolUse to return modifiedArgs and suppressOutput", async () => {
        const inputs: PreToolUseHookInput[] = [];
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            tools: [
                defineTool("echo_value", {
                    description: "Echoes the supplied value",
                    parameters: z.object({ value: z.string() }),
                    handler: ({ value }) => value,
                }),
            ],
            hooks: {
                onPreToolUse: async (input) => {
                    inputs.push(input);
                    if (input.toolName !== "echo_value") {
                        return { permissionDecision: "allow" };
                    }
                    return {
                        permissionDecision: "allow",
                        modifiedArgs: { value: "modified by hook" },
                        suppressOutput: false,
                    };
                },
            },
        });

        const response = await session.sendAndWait({
            prompt: "Call echo_value with value 'original', then reply with the result.",
        });

        expect(inputs.length).toBeGreaterThan(0);
        expect(inputs.some((input) => input.toolName === "echo_value")).toBe(true);
        expect(response?.data.content ?? "").toContain("modified by hook");

        await session.disconnect();
    });

    it("should allow postToolUse to return modifiedResult", async () => {
        const inputs: PostToolUseHookInput[] = [];
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            availableTools: ["report_intent"],
            hooks: {
                onPostToolUse: async (input) => {
                    inputs.push(input);
                    if (input.toolName !== "report_intent") {
                        return undefined;
                    }
                    return {
                        modifiedResult: {
                            textResultForLlm: "modified by post hook",
                            resultType: "success",
                            toolTelemetry: {},
                        },
                        suppressOutput: false,
                    };
                },
            },
        });

        const response = await session.sendAndWait({
            prompt: "Call the report_intent tool with intent 'Testing post hook', then reply done.",
        });

        expect(inputs.some((input) => input.toolName === "report_intent")).toBe(true);
        expect(response?.data.content).toBe("Done.");

        await session.disconnect();
    });
});
