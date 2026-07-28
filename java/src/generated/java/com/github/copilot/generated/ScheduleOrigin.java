/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Who created the schedule: `user` (an explicit user action such as `/every` or `/after`) or `model` (the agent via the `manage_schedule` tool). Gates whether a scheduled skill that opted out of model invocation may fire: only user-created schedules may.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ScheduleOrigin {
    /** The {@code user} variant. */
    USER("user"),
    /** The {@code model} variant. */
    MODEL("model");

    private final String value;
    ScheduleOrigin(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ScheduleOrigin fromValue(String value) {
        for (ScheduleOrigin v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ScheduleOrigin value: " + value);
    }
}
