/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Allowed values for the `McpAppsHostContextDetailsAvailableDisplayMode` enumeration.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum McpAppsHostContextDetailsAvailableDisplayMode {
    /** The {@code inline} variant. */
    INLINE("inline"),
    /** The {@code fullscreen} variant. */
    FULLSCREEN("fullscreen"),
    /** The {@code pip} variant. */
    PIP("pip");

    private final String value;
    McpAppsHostContextDetailsAvailableDisplayMode(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpAppsHostContextDetailsAvailableDisplayMode fromValue(String value) {
        for (McpAppsHostContextDetailsAvailableDisplayMode v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpAppsHostContextDetailsAvailableDisplayMode value: " + value);
    }
}
