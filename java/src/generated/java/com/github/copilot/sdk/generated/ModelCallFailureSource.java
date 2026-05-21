/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import javax.annotation.processing.Generated;

/**
 * Where the failed model call originated
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ModelCallFailureSource {
    /** The {@code top_level} variant. */
    TOP_LEVEL("top_level"),
    /** The {@code subagent} variant. */
    SUBAGENT("subagent"),
    /** The {@code mcp_sampling} variant. */
    MCP_SAMPLING("mcp_sampling");

    private final String value;
    ModelCallFailureSource(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ModelCallFailureSource fromValue(String value) {
        for (ModelCallFailureSource v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ModelCallFailureSource value: " + value);
    }
}
