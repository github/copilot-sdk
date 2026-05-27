/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * The type of operation performed on the autopilot objective state file
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AutopilotObjectiveChangedOperation {
    /** The {@code create} variant. */
    CREATE("create"),
    /** The {@code update} variant. */
    UPDATE("update"),
    /** The {@code delete} variant. */
    DELETE("delete");

    private final String value;
    AutopilotObjectiveChangedOperation(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AutopilotObjectiveChangedOperation fromValue(String value) {
        for (AutopilotObjectiveChangedOperation v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AutopilotObjectiveChangedOperation value: " + value);
    }
}
