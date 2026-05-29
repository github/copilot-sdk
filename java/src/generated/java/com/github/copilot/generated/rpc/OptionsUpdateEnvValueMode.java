/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * How env values are passed to MCP servers (`direct` inlines literal values; `indirect` resolves at launch).
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum OptionsUpdateEnvValueMode {
    /** The {@code direct} variant. */
    DIRECT("direct"),
    /** The {@code indirect} variant. */
    INDIRECT("indirect");

    private final String value;
    OptionsUpdateEnvValueMode(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static OptionsUpdateEnvValueMode fromValue(String value) {
        for (OptionsUpdateEnvValueMode v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown OptionsUpdateEnvValueMode value: " + value);
    }
}
