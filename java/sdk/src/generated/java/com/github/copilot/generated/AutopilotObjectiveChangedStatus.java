/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Current autopilot objective status, if one exists
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AutopilotObjectiveChangedStatus {
    /** The {@code active} variant. */
    ACTIVE("active"),
    /** The {@code paused} variant. */
    PAUSED("paused"),
    /** The {@code cap_reached} variant. */
    CAP_REACHED("cap_reached"),
    /** The {@code completed} variant. */
    COMPLETED("completed");

    private final String value;
    AutopilotObjectiveChangedStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AutopilotObjectiveChangedStatus fromValue(String value) {
        for (AutopilotObjectiveChangedStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AutopilotObjectiveChangedStatus value: " + value);
    }
}
