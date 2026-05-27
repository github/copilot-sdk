/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Allowed values for the `ToolExecutionCompleteToolDescriptionMetaUIVisibility` enumeration.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ToolExecutionCompleteToolDescriptionMetaUIVisibility {
    /** The {@code model} variant. */
    MODEL("model"),
    /** The {@code app} variant. */
    APP("app");

    private final String value;
    ToolExecutionCompleteToolDescriptionMetaUIVisibility(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ToolExecutionCompleteToolDescriptionMetaUIVisibility fromValue(String value) {
        for (ToolExecutionCompleteToolDescriptionMetaUIVisibility v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ToolExecutionCompleteToolDescriptionMetaUIVisibility value: " + value);
    }
}
