/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc.fixtures;

import com.github.copilot.tool.CopilotTool;
import com.github.copilot.tool.Param;

/**
 * Fixture testing argument coercion with multiple types including an enum.
 */
public class ArgCoercionTools {

    public enum Color {
        RED, GREEN, BLUE
    }

    @CopilotTool("Method with mixed argument types")
    public String mixedArgs(@Param("Text input") String text, @Param("A count") int count,
            @Param("A flag") boolean flag, @Param("A color") Color color) {
        return text + "-" + count + "-" + flag + "-" + color.name();
    }
}
