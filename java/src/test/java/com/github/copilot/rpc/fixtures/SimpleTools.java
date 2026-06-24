/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc.fixtures;

import com.github.copilot.tool.CopilotTool;
import com.github.copilot.tool.Param;

/**
 * Simple tool fixture with basic String-returning methods.
 */
public class SimpleTools {

    @CopilotTool("Greets a user by name")
    public String greetUser(@Param(value = "The user's name", required = true) String name) {
        return "Hello, " + name + "!";
    }

    @CopilotTool("Adds two numbers together")
    public String addNumbers(@Param(value = "First number") int a, @Param(value = "Second number") int b) {
        return String.valueOf(a + b);
    }
}
