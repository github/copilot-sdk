/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * How the pending MCP headers refresh request resolved.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum McpHeadersRefreshCompletedOutcome {
    /** The {@code headers} variant. */
    HEADERS("headers"),
    /** The {@code none} variant. */
    NONE("none"),
    /** The {@code timeout} variant. */
    TIMEOUT("timeout");

    private final String value;
    McpHeadersRefreshCompletedOutcome(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpHeadersRefreshCompletedOutcome fromValue(String value) {
        for (McpHeadersRefreshCompletedOutcome v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpHeadersRefreshCompletedOutcome value: " + value);
    }
}
