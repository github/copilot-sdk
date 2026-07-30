/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.e2e;

import com.github.copilot.tool.CopilotTool;
import com.github.copilot.tool.CopilotToolParam;

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
    public String setCurrentPhase(@CopilotToolParam("The phase to transition to") String phase) {
        currentPhase = phase;
        return "Phase set to " + phase;
    }

    @CopilotTool("Search for items by keyword")
    public String searchItems(@CopilotToolParam("Search keyword") String keyword) {
        return "Found: " + keyword + " -> item_alpha, item_beta";
    }

    @CopilotTool("Returns the current status")
    public String getStatus() {
        return "Status: OK";
    }

    @CopilotTool("Combines two values into a single string")
    public String combineValues(@CopilotToolParam("First value") String value1,
            @CopilotToolParam("Second value") String value2) {
        return "combined: " + value1 + " + " + value2;
    }
}
