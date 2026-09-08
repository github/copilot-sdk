/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Why a passive MCP OAuth probe determined authentication is needed.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum McpOauthProbeNeedsAuthReason {
    /** The {@code initial} variant. */
    INITIAL("initial"),
    /** The {@code refresh} variant. */
    REFRESH("refresh"),
    /** The {@code upscope} variant. */
    UPSCOPE("upscope");

    private final String value;
    McpOauthProbeNeedsAuthReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpOauthProbeNeedsAuthReason fromValue(String value) {
        for (McpOauthProbeNeedsAuthReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpOauthProbeNeedsAuthReason value: " + value);
    }
}
