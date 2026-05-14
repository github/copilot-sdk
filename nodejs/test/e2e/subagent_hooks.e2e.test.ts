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
import { approveAll } from "../../src/index.js";
import { createSdkTestContext, isCI } from "./harness/sdkTestContext.js";

describe("Subagent hooks", async () => {
    // For snapshot recording (non-CI), use RECORD_GH_TOKEN if available
    const recordToken = !isCI ? process.env.RECORD_GH_TOKEN : undefined;
    const { copilotClient: client, workDir, env } = await createSdkTestContext({
        ...(recordToken ? { copilotClientOptions: { gitHubToken: recordToken } } : {}),
    });
    // Sub-agent hook propagation requires the session-based subagents feature flag.
    // Without this flag, the legacy callback-bridge path is used, which does not
    // support SDK preToolUse/postToolUse hooks for sub-agent tool calls.
    env.COPILOT_EXP_COPILOT_CLI_SESSION_BASED_SUBAGENTS = "true";

    it("should invoke preToolUse and postToolUse hooks for sub-agent tool calls", async () => {
        // Track hooks with agentSessionId so we can verify parent vs sub-agent
        const hookLog: { kind: "pre" | "post"; toolName: string; agentSessionId: string; index: number }[] = [];
        let hookIndex = 0;

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            hooks: {
                onPreToolUse: async (input: PreToolUseHookInput) => {
                    hookLog.push({
                        kind: "pre",
                        toolName: input.toolName,
                        agentSessionId: input.agentSessionId,
                        index: hookIndex++,
                    });
                    return { permissionDecision: "allow" } as PreToolUseHookOutput;
                },
                onPostToolUse: async (input: PostToolUseHookInput) => {
                    hookLog.push({
                        kind: "post",
                        toolName: input.toolName,
                        agentSessionId: input.agentSessionId,
                        index: hookIndex++,
                    });
                    return null as PostToolUseHookOutput;
                },
            },
        });

        // Create a file for the sub-agent to read
        await writeFile(join(workDir, "subagent-test.txt"), "Hello from subagent test!");

        // Use a prompt that causes the model to spawn a sub-agent via the task tool.
        // The sub-agent will use tools (e.g., view) to read the file.
        await session.sendAndWait({
            prompt:
                "Use the task tool to spawn an explore agent that reads the file subagent-test.txt in the current directory and reports its contents. You must use the task tool.",
        });

        // --- Parent tool hooks ---
        // The parent agent calls "task" to spawn the sub-agent.
        const taskPre = hookLog.find((h) => h.kind === "pre" && h.toolName === "task");
        const taskPost = hookLog.find((h) => h.kind === "post" && h.toolName === "task");
        expect(taskPre, "preToolUse should fire for the parent's 'task' tool call").toBeDefined();
        expect(taskPost, "postToolUse should fire for the parent's 'task' tool call").toBeDefined();

        // --- Sub-agent tool hooks ---
        // The sub-agent uses "view" (or similar) to read the file. These hooks prove
        // that sub-agent tool calls trigger hooks back to the SDK.
        const viewPre = hookLog.filter((h) => h.kind === "pre" && h.toolName === "view");
        const viewPost = hookLog.filter((h) => h.kind === "post" && h.toolName === "view");
        expect(viewPre.length, "preToolUse should fire for the sub-agent's 'view' tool call").toBeGreaterThan(0);
        expect(viewPost.length, "postToolUse should fire for the sub-agent's 'view' tool call").toBeGreaterThan(0);

        // --- agentSessionId distinguishes parent from sub-agent ---
        // The parent's "task" hook and the sub-agent's "view" hook should have
        // different agentSessionIds, proving the SDK exposes which session
        // (parent vs sub-agent) originated each tool call.
        const parentSessionId = taskPre!.agentSessionId;
        const subagentSessionId = viewPre[0].agentSessionId;
        expect(parentSessionId).toBeDefined();
        expect(subagentSessionId).toBeDefined();
        expect(subagentSessionId).not.toBe(parentSessionId);

        // All parent tool hooks share the same agentSessionId
        const parentHooks = hookLog.filter((h) => h.agentSessionId === parentSessionId);
        expect(parentHooks.every((h) => ["task", "read_agent", "report_intent"].includes(h.toolName))).toBe(true);

        // All sub-agent tool hooks share a different agentSessionId
        const subagentHooks = hookLog.filter((h) => h.agentSessionId === subagentSessionId);
        expect(subagentHooks.length).toBeGreaterThan(0);
        expect(subagentHooks.some((h) => h.toolName === "view")).toBe(true);

        // --- Ordering: sub-agent tool calls occur after the parent spawns the sub-agent ---
        expect(viewPre[0].index).toBeGreaterThan(taskPre!.index);

        await session.disconnect();
    }, 120_000);
});
