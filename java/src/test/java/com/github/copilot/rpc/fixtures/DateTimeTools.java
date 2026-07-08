/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc.fixtures;

import java.time.LocalDateTime;

import com.github.copilot.tool.CopilotTool;
import com.github.copilot.tool.CopilotToolParam;

/**
 * Fixture testing java.time argument deserialization via ObjectMapper with
 * JavaTimeModule.
 */
public class DateTimeTools {

    @CopilotTool("Schedule an event at a given time")
    public String scheduleEvent(@CopilotToolParam(value = "When to schedule", required = true) LocalDateTime when) {
        return "Scheduled at " + when.getYear() + "-" + String.format("%02d", when.getMonthValue()) + "-"
                + String.format("%02d", when.getDayOfMonth()) + "T" + String.format("%02d", when.getHour()) + ":"
                + String.format("%02d", when.getMinute());
    }
}
