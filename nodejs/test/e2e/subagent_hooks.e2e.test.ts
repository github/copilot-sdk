/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { writeFile } from "fs/promises";
import { join } from "path";
import { describe, expect, it } from "vitest";
import type {
    CopilotRequestContext,
    PreToolUseHookInput,
    PreToolUseHookOutput,
    PostToolUseHookInput,
    PostToolUseHookOutput,
} from "../../src/index.js";
import { approveAll, CopilotRequestHandler } from "../../src/index.js";
import { createSdkTestContext, isCI } from "./harness/sdkTestContext.js";

interface RequestRecord {
    url: string;
    agentId?: string;
    parentAgentId?: string;
    interactionType?: string;
}

class RecordingRequestHandler extends CopilotRequestHandler {
    readonly records: RequestRecord[] = [];

    protected override async sendRequest(
        request: Request,
        ctx: CopilotRequestContext
    ): Promise<Response> {
        this.records.push({
            url: request.url,
            agentId: ctx.agentId,
            parentAgentId: ctx.parentAgentId,
            interactionType: ctx.interactionType,
        });
        return super.sendRequest(request, ctx);
    }
}

function isInferenceUrl(url: string): boolean {
    const u = url.toLowerCase();
    return (
        u.endsWith("/chat/completions") ||
        u.endsWith("/responses") ||
        u.endsWith("/v1/messages") ||
        u.endsWith("/messages")
    );
}

function expectSubagentRequestMetadata(records: RequestRecord[]): void {
    const inference = records.filter((r) => isInferenceUrl(r.url));
    expect(inference.length, "request handler should observe inference requests").toBeGreaterThan(
        0
    );

    const subagentRequest = inference.find((r) => r.parentAgentId);
    expect(
        subagentRequest,
        "sub-agent inference request should carry a parentAgentId"
    ).toBeDefined();
    expect(
        subagentRequest!.agentId,
        "sub-agent inference request should carry an agentId"
    ).toBeTruthy();
    expect(
        subagentRequest!.interactionType,
        "sub-agent inference request should carry an interactionType"
    ).toBeTruthy();
    expect(subagentRequest!.parentAgentId).not.toBe(subagentRequest!.agentId);
}

describe("Subagent hooks", async () => {
    // For snapshot recording (non-CI), use RECORD_GH_TOKEN if available
    const recordToken = !isCI ? process.env.RECORD_GH_TOKEN : undefined;
    const requestHandler = new RecordingRequestHandler();
    const { copilotClient: client, workDir } = await createSdkTestContext({
        copilotClientOptions: {
            ...(recordToken ? { gitHubToken: recordToken } : {}),
            requestHandler,
            env: { COPILOT_EXP_COPILOT_CLI_SESSION_BASED_SUBAGENTS: "true" },
        },
    });

    it("should invoke preToolUse and postToolUse hooks for sub-agent tool calls", async () => {
        const hookLog: { kind: "pre" | "post"; toolName: string; sessionId: string }[] = [];

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            hooks: {
                onPreToolUse: async (input: PreToolUseHookInput) => {
                    hookLog.push({
                        kind: "pre",
                        toolName: input.toolName,
                        sessionId: input.sessionId,
                    });
                    return { permissionDecision: "allow" } as PreToolUseHookOutput;
                },
                onPostToolUse: async (input: PostToolUseHookInput) => {
                    hookLog.push({
                        kind: "post",
                        toolName: input.toolName,
                        sessionId: input.sessionId,
                    });
                    return null as PostToolUseHookOutput;
                },
            },
        });

        // Create a file for the sub-agent to read
        await writeFile(join(workDir, "subagent-test.txt"), "Hello from subagent test!");

        await session.sendAndWait({
            prompt: "Use the task tool to spawn an explore agent that reads the file subagent-test.txt in the current directory and reports its contents. You must use the task tool.",
        });

        // Parent tool hooks fire for "task"
        const taskPre = hookLog.find((h) => h.kind === "pre" && h.toolName === "task");
        expect(taskPre, "preToolUse should fire for the parent's 'task' tool call").toBeDefined();

        // Sub-agent tool hooks fire for "view"
        const viewPre = hookLog.filter((h) => h.kind === "pre" && h.toolName === "view");
        const viewPost = hookLog.filter((h) => h.kind === "post" && h.toolName === "view");
        expect(
            viewPre.length,
            "preToolUse should fire for the sub-agent's 'view' tool call"
        ).toBeGreaterThan(0);
        expect(
            viewPost.length,
            "postToolUse should fire for the sub-agent's 'view' tool call"
        ).toBeGreaterThan(0);

        // input.sessionId distinguishes parent from sub-agent: parent tools and
        // sub-agent tools carry different sessionIds
        expect(viewPre[0].sessionId).not.toBe(taskPre!.sessionId);
        expectSubagentRequestMetadata(requestHandler.records);

        await session.disconnect();
    }, 120_000);
});
