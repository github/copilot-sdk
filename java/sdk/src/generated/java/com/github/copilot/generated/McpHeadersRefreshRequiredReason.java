/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Why dynamic headers are being requested.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum McpHeadersRefreshRequiredReason {
    /** The {@code startup} variant. */
    STARTUP("startup"),
    /** The {@code ttl-expired} variant. */
    TTL_EXPIRED("ttl-expired"),
    /** The {@code auth-failed} variant. */
    AUTH_FAILED("auth-failed");

    private final String value;
    McpHeadersRefreshRequiredReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpHeadersRefreshRequiredReason fromValue(String value) {
        for (McpHeadersRefreshRequiredReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpHeadersRefreshRequiredReason value: " + value);
    }
}
