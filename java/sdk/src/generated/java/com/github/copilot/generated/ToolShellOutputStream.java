/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Shell output source. Terminal output has no separate stdout/stderr attribution.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ToolShellOutputStream {
    /** The {@code stdout} variant. */
    STDOUT("stdout"),
    /** The {@code stderr} variant. */
    STDERR("stderr"),
    /** The {@code terminal} variant. */
    TERMINAL("terminal");

    private final String value;
    ToolShellOutputStream(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ToolShellOutputStream fromValue(String value) {
        for (ToolShellOutputStream v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ToolShellOutputStream value: " + value);
    }
}
