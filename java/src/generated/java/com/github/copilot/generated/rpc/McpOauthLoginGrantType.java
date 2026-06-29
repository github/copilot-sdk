/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * OAuth grant type override for this login.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum McpOauthLoginGrantType {
    /** The {@code authorization_code} variant. */
    AUTHORIZATION_CODE("authorization_code"),
    /** The {@code client_credentials} variant. */
    CLIENT_CREDENTIALS("client_credentials");

    private final String value;
    McpOauthLoginGrantType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpOauthLoginGrantType fromValue(String value) {
        for (McpOauthLoginGrantType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpOauthLoginGrantType value: " + value);
    }
}
