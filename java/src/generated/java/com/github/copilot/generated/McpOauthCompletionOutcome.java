/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * How the pending MCP OAuth request was completed
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum McpOauthCompletionOutcome {
    /** The {@code token} variant. */
    TOKEN("token"),
    /** The {@code cancelled} variant. */
    CANCELLED("cancelled");

    private final String value;
    McpOauthCompletionOutcome(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpOauthCompletionOutcome fromValue(String value) {
        for (McpOauthCompletionOutcome v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpOauthCompletionOutcome value: " + value);
    }
}
