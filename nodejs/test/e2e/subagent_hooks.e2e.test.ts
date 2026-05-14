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
        const hookLog: { kind: "pre" | "post"; toolName: string; sessionId: string }[] = [];

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            hooks: {
                onPreToolUse: async (input: PreToolUseHookInput) => {
                    hookLog.push({ kind: "pre", toolName: input.toolName, sessionId: input.sessionId });
                    return { permissionDecision: "allow" } as PreToolUseHookOutput;
                },
                onPostToolUse: async (input: PostToolUseHookInput) => {
                    hookLog.push({ kind: "post", toolName: input.toolName, sessionId: input.sessionId });
                    return null as PostToolUseHookOutput;
                },
            },
        });

        // Create a file for the sub-agent to read
        await writeFile(join(workDir, "subagent-test.txt"), "Hello from subagent test!");

        await session.sendAndWait({
            prompt:
                "Use the task tool to spawn an explore agent that reads the file subagent-test.txt in the current directory and reports its contents. You must use the task tool.",
        });

        // Parent tool hooks fire for "task"
        const taskPre = hookLog.find((h) => h.kind === "pre" && h.toolName === "task");
        expect(taskPre, "preToolUse should fire for the parent's 'task' tool call").toBeDefined();

        // Sub-agent tool hooks fire for "view"
        const viewPre = hookLog.filter((h) => h.kind === "pre" && h.toolName === "view");
        const viewPost = hookLog.filter((h) => h.kind === "post" && h.toolName === "view");
        expect(viewPre.length, "preToolUse should fire for the sub-agent's 'view' tool call").toBeGreaterThan(0);
        expect(viewPost.length, "postToolUse should fire for the sub-agent's 'view' tool call").toBeGreaterThan(0);

        // input.sessionId distinguishes parent from sub-agent: parent tools and
        // sub-agent tools carry different sessionIds
        expect(viewPre[0].sessionId).not.toBe(taskPre!.sessionId);

        await session.disconnect();
    }, 120_000);
});
