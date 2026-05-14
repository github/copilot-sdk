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
    // Enable session-based subagents so createSubagentSession is used
    env.COPILOT_EXP_COPILOT_CLI_SESSION_BASED_SUBAGENTS = "true";

    it("should invoke preToolUse and postToolUse hooks for sub-agent tool calls", async () => {
        const preToolUseInputs: PreToolUseHookInput[] = [];
        const postToolUseInputs: PostToolUseHookInput[] = [];

        const session = await client.createSession({
            onPermissionRequest: approveAll,
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

        // Create a file for the sub-agent to read
        await writeFile(join(workDir, "subagent-test.txt"), "Hello from subagent test!");

        // Use a prompt that causes the model to spawn a sub-agent via the task tool.
        // The sub-agent will use tools (e.g., view) to read the file.
        await session.sendAndWait({
            prompt:
                "Use the task tool to spawn an explore agent that reads the file subagent-test.txt in the current directory and reports its contents. You must use the task tool.",
        });

        // preToolUse should have been called for the parent's task tool call
        const taskPreHooks = preToolUseInputs.filter((i) => i.toolName === "task");
        expect(taskPreHooks.length).toBeGreaterThanOrEqual(1);

        // preToolUse should ALSO have been called for the sub-agent's "view" tool
        const viewPreHooks = preToolUseInputs.filter((i) => i.toolName === "view");
        expect(viewPreHooks.length).toBeGreaterThan(0);

        // postToolUse should also have been called for the sub-agent's "view" tool
        const viewPostHooks = postToolUseInputs.filter((i) => i.toolName === "view");
        expect(viewPostHooks.length).toBeGreaterThan(0);

        await session.disconnect();
    }, 120_000);
});
