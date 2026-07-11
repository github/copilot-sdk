/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Current display mode (SEP-1865)
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum McpAppsHostContextDetailsDisplayMode {
    /** The {@code inline} variant. */
    INLINE("inline"),
    /** The {@code fullscreen} variant. */
    FULLSCREEN("fullscreen"),
    /** The {@code pip} variant. */
    PIP("pip");

    private final String value;
    McpAppsHostContextDetailsDisplayMode(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpAppsHostContextDetailsDisplayMode fromValue(String value) {
        for (McpAppsHostContextDetailsDisplayMode v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpAppsHostContextDetailsDisplayMode value: " + value);
    }
}
