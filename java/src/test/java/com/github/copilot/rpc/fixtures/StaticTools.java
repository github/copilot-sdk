/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc.fixtures;

import com.github.copilot.tool.CopilotTool;
import com.github.copilot.tool.CopilotToolParam;

/**
 * Tool fixture with a static {@code @CopilotTool} method, used to test
 * {@code ToolDefinition.fromClass()} invocation path.
 */
public class StaticTools {

    @CopilotTool("Returns a greeting for the given name")
    public static String greet(@CopilotToolParam(value = "The name to greet", required = true) String name) {
        return "Hi, " + name + "!";
    }
}
