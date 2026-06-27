/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Reason the runtime is requesting host-provided MCP OAuth credentials
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum McpOauthRequestReason {
    /** The {@code initial} variant. */
    INITIAL("initial"),
    /** The {@code refresh} variant. */
    REFRESH("refresh"),
    /** The {@code reauth} variant. */
    REAUTH("reauth"),
    /** The {@code upscope} variant. */
    UPSCOPE("upscope");

    private final String value;
    McpOauthRequestReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpOauthRequestReason fromValue(String value) {
        for (McpOauthRequestReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpOauthRequestReason value: " + value);
    }
}
