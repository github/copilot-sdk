/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc.fixtures;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import com.github.copilot.tool.CopilotTool;
import com.github.copilot.tool.CopilotToolParam;

/**
 * Tool fixture with Optional parameter types for testing correct argument
 * extraction (null-check + wrapping instead of mapper.convertValue).
 */
public class OptionalParamTools {

    @CopilotTool("Greet with optional title")
    public String greetWithTitle(@CopilotToolParam("Name") String name,
            @CopilotToolParam("Optional title") Optional<String> title) {
        return title.map(t -> t + " " + name).orElse(name);
    }

    @CopilotTool("Multiply with optional factor")
    public String multiply(@CopilotToolParam("Base value") int base,
            @CopilotToolParam("Optional factor") OptionalInt factor) {
        return String.valueOf(base * factor.orElse(1));
    }

    @CopilotTool("Scale with optional ratio")
    public String scale(@CopilotToolParam("Value") double value,
            @CopilotToolParam("Optional ratio") OptionalDouble ratio) {
        return String.valueOf(value * ratio.orElse(1.0));
    }

    @CopilotTool("Offset with optional delta")
    public String offset(@CopilotToolParam("Base") long base, @CopilotToolParam("Optional delta") OptionalLong delta) {
        return String.valueOf(base + delta.orElse(0L));
    }
}
