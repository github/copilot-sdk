/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Outcome of starting the original prepared owned login.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum McpOwnedOauthLoginStatus {
    /** The {@code awaiting-browser} variant. */
    AWAITING_BROWSER("awaiting-browser"),
    /** The {@code connected} variant. */
    CONNECTED("connected");

    private final String value;
    McpOwnedOauthLoginStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpOwnedOauthLoginStatus fromValue(String value) {
        for (McpOwnedOauthLoginStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpOwnedOauthLoginStatus value: " + value);
    }
}
