/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Terminal outcome of the original authorization reservation, including reconnect
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum McpOauthRecoveryCompletedOutcome {
    /** The {@code succeeded} variant. */
    SUCCEEDED("succeeded"),
    /** The {@code cancelled} variant. */
    CANCELLED("cancelled"),
    /** The {@code failed} variant. */
    FAILED("failed");

    private final String value;
    McpOauthRecoveryCompletedOutcome(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpOauthRecoveryCompletedOutcome fromValue(String value) {
        for (McpOauthRecoveryCompletedOutcome v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpOauthRecoveryCompletedOutcome value: " + value);
    }
}
