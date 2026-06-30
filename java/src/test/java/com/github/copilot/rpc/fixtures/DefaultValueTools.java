/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc.fixtures;

import com.github.copilot.tool.CopilotTool;
import com.github.copilot.tool.CopilotToolParam;

/**
 * Fixture testing default parameter values.
 */
public class DefaultValueTools {

    @CopilotTool("Method with a default value parameter")
    public String withDefault(@CopilotToolParam(value = "A label", required = true) String label,
            @CopilotToolParam(value = "A count", required = false, defaultValue = "42") int count) {
        return label + ":" + count;
    }
}
