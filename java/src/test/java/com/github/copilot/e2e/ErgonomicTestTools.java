/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.e2e;

import com.github.copilot.tool.CopilotTool;
import com.github.copilot.tool.Param;

/**
 * Tool fixture for the ergonomic {@code @CopilotTool} E2E integration test.
 *
 * <p>
 * This class exercises the annotation-based tool definition API, producing
 * identical wire-level tool schemas to the low-level
 * {@code ToolDefinition.create()} API.
 */
class ErgonomicTestTools {

    String currentPhase;

    @CopilotTool("Sets the current phase of the agent")
    public String setCurrentPhase(@Param("The phase to transition to") String phase) {
        currentPhase = phase;
        return "Phase set to " + phase;
    }

    @CopilotTool("Search for items by keyword")
    public String searchItems(@Param("Search keyword") String keyword) {
        return "Found: item_alpha, item_beta";
    }

    @CopilotTool(value = "Custom grep override", name = "grep", overridesBuiltInTool = true)
    public String grepOverride(@Param("Search query") String query) {
        return "CUSTOM_GREP: " + query;
    }
}
