/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Execution outcome classification.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ToolResultType {
    /** The {@code success} variant. */
    SUCCESS("success"),
    /** The {@code failure} variant. */
    FAILURE("failure"),
    /** The {@code timeout} variant. */
    TIMEOUT("timeout"),
    /** The {@code rejected} variant. */
    REJECTED("rejected"),
    /** The {@code denied} variant. */
    DENIED("denied");

    private final String value;
    ToolResultType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ToolResultType fromValue(String value) {
        for (ToolResultType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ToolResultType value: " + value);
    }
}
