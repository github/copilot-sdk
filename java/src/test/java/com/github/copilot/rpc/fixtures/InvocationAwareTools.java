/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc.fixtures;

import java.util.concurrent.CompletableFuture;

import com.github.copilot.rpc.RecordInvocationArgs;
import com.github.copilot.rpc.ToolInvocation;
import com.github.copilot.tool.CopilotTool;
import com.github.copilot.tool.CopilotToolParam;

/**
 * Tool fixture for {@link ToolInvocation} runtime context injection.
 */
public class InvocationAwareTools {

    @CopilotTool("Reports progress with invocation context")
    public String reportProgress(@CopilotToolParam("Current phase") String phase, ToolInvocation invocation) {
        return "phase=" + phase + ",sessionId=" + invocation.getSessionId() + ",toolCallId="
                + invocation.getToolCallId() + ",toolName=" + invocation.getToolName();
    }

    @CopilotTool("Reports progress asynchronously with invocation context")
    public CompletableFuture<String> reportProgressAsync(@CopilotToolParam("Current phase") String phase,
            ToolInvocation invocation) {
        return CompletableFuture.completedFuture("async phase=" + phase + ",sessionId=" + invocation.getSessionId()
                + ",toolCallId=" + invocation.getToolCallId() + ",toolName=" + invocation.getToolName());
    }

    @CopilotTool("Reports progress with invocation first")
    public String reportProgressFirst(ToolInvocation invocation, @CopilotToolParam("Current phase") String phase) {
        return "first phase=" + phase + ",sessionId=" + invocation.getSessionId() + ",toolCallId="
                + invocation.getToolCallId() + ",toolName=" + invocation.getToolName();
    }

    @CopilotTool("Reports context with invocation only")
    public String onlyContext(ToolInvocation invocation) {
        return "only sessionId=" + invocation.getSessionId() + ",toolCallId=" + invocation.getToolCallId()
                + ",toolName=" + invocation.getToolName();
    }

    @CopilotTool("Reports progress with invocation in the middle")
    public String reportProgressMiddle(@CopilotToolParam("Current phase") String phase, ToolInvocation invocation,
            @CopilotToolParam("Maximum items") int limit) {
        return "middle phase=" + phase + ",limit=" + limit + ",sessionId=" + invocation.getSessionId() + ",toolCallId="
                + invocation.getToolCallId() + ",toolName=" + invocation.getToolName();
    }

    @CopilotTool("Reports progress with record args and invocation")
    public String reportProgressWithRecord(RecordInvocationArgs args, ToolInvocation invocation) {
        return "record query=" + args.query() + ",limit=" + args.limit() + ",sessionId=" + invocation.getSessionId()
                + ",toolCallId=" + invocation.getToolCallId() + ",toolName=" + invocation.getToolName();
    }
}
