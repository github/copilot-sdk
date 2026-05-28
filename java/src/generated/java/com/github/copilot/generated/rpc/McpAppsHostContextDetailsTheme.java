/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * UI theme preference per SEP-1865
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum McpAppsHostContextDetailsTheme {
    /** The {@code light} variant. */
    LIGHT("light"),
    /** The {@code dark} variant. */
    DARK("dark");

    private final String value;
    McpAppsHostContextDetailsTheme(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpAppsHostContextDetailsTheme fromValue(String value) {
        for (McpAppsHostContextDetailsTheme v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpAppsHostContextDetailsTheme value: " + value);
    }
}
