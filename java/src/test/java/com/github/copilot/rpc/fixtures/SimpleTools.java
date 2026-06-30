/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc.fixtures;

import com.github.copilot.tool.CopilotTool;
import com.github.copilot.tool.CopilotToolParam;

/**
 * Simple tool fixture with basic String-returning methods.
 */
public class SimpleTools {

    @CopilotTool("Greets a user by name")
    public String greetUser(@CopilotToolParam(value = "The user's name", required = true) String name) {
        return "Hello, " + name + "!";
    }

    @CopilotTool("Adds two numbers together")
    public String addNumbers(@CopilotToolParam(value = "First number") int a,
            @CopilotToolParam(value = "Second number") int b) {
        return String.valueOf(a + b);
    }
}
