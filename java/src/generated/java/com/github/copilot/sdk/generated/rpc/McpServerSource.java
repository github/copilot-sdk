/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Configuration source: user, workspace, plugin, or builtin
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum McpServerSource {
    /** The {@code user} variant. */
    USER("user"),
    /** The {@code workspace} variant. */
    WORKSPACE("workspace"),
    /** The {@code plugin} variant. */
    PLUGIN("plugin"),
    /** The {@code builtin} variant. */
    BUILTIN("builtin");

    private final String value;
    McpServerSource(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpServerSource fromValue(String value) {
        for (McpServerSource v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpServerSource value: " + value);
    }
}
