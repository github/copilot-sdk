/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc.fixtures;

import java.util.concurrent.CompletableFuture;

import com.github.copilot.rpc.ToolInvocation;
import com.github.copilot.tool.CopilotTool;
import com.github.copilot.tool.Param;

/**
 * Tool fixture for {@link ToolInvocation} runtime context injection.
 */
public class InvocationAwareTools {

    @CopilotTool("Reports progress with invocation context")
    public String reportProgress(@Param(value = "Current phase", required = true) String phase,
            ToolInvocation invocation) {
        return "phase=" + phase + ",sessionId=" + invocation.getSessionId() + ",toolCallId="
                + invocation.getToolCallId() + ",toolName=" + invocation.getToolName();
    }

    @CopilotTool("Reports progress asynchronously with invocation context")
    public CompletableFuture<String> reportProgressAsync(@Param(value = "Current phase", required = true) String phase,
            ToolInvocation invocation) {
        return CompletableFuture.completedFuture("async phase=" + phase + ",sessionId=" + invocation.getSessionId()
                + ",toolCallId=" + invocation.getToolCallId());
    }
}
