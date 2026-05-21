/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import javax.annotation.processing.Generated;

/**
 * New connection status: connected, failed, needs-auth, pending, disabled, or not_configured
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum McpServerStatusChangedStatus {
    /** The {@code connected} variant. */
    CONNECTED("connected"),
    /** The {@code failed} variant. */
    FAILED("failed"),
    /** The {@code needs-auth} variant. */
    NEEDS_AUTH("needs-auth"),
    /** The {@code pending} variant. */
    PENDING("pending"),
    /** The {@code disabled} variant. */
    DISABLED("disabled"),
    /** The {@code not_configured} variant. */
    NOT_CONFIGURED("not_configured");

    private final String value;
    McpServerStatusChangedStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpServerStatusChangedStatus fromValue(String value) {
        for (McpServerStatusChangedStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpServerStatusChangedStatus value: " + value);
    }
}
