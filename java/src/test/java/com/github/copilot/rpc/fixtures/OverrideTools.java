/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc.fixtures;

import com.github.copilot.tool.CopilotTool;
import com.github.copilot.tool.CopilotToolParam;

/**
 * Fixture testing tool override flag.
 */
public class OverrideTools {

    @CopilotTool(value = "Custom grep implementation", name = "grep", overridesBuiltInTool = true)
    public String customGrep(@CopilotToolParam(value = "Search pattern", required = true) String pattern) {
        return "Found: " + pattern;
    }
}
