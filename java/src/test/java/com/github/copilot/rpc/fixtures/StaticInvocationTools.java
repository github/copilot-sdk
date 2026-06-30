/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc.fixtures;

import com.github.copilot.rpc.ToolInvocation;
import com.github.copilot.tool.CopilotTool;
import com.github.copilot.tool.CopilotToolParam;

/**
 * Static tool fixture for {@link ToolInvocation} runtime context injection.
 */
public class StaticInvocationTools {

    @CopilotTool("Returns invocation context from a static tool")
    public static String reportStatic(@CopilotToolParam("Current phase") String phase, ToolInvocation invocation) {
        return "phase=" + phase + ",sessionId=" + invocation.getSessionId() + ",toolCallId="
                + invocation.getToolCallId() + ",toolName=" + invocation.getToolName();
    }
}
