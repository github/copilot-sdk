/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Current normalized autopilot objective lifecycle status.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AutopilotObjectiveStatus {
    /** The {@code active} variant. */
    ACTIVE("active"),
    /** The {@code paused} variant. */
    PAUSED("paused"),
    /** The {@code completed} variant. */
    COMPLETED("completed");

    private final String value;
    AutopilotObjectiveStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AutopilotObjectiveStatus fromValue(String value) {
        for (AutopilotObjectiveStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AutopilotObjectiveStatus value: " + value);
    }
}
