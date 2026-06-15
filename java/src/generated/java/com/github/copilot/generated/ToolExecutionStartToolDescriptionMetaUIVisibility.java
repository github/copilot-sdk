/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Allowed values for the `ToolExecutionStartToolDescriptionMetaUIVisibility` enumeration.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ToolExecutionStartToolDescriptionMetaUIVisibility {
    /** The {@code model} variant. */
    MODEL("model"),
    /** The {@code app} variant. */
    APP("app");

    private final String value;
    ToolExecutionStartToolDescriptionMetaUIVisibility(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ToolExecutionStartToolDescriptionMetaUIVisibility fromValue(String value) {
        for (ToolExecutionStartToolDescriptionMetaUIVisibility v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ToolExecutionStartToolDescriptionMetaUIVisibility value: " + value);
    }
}
