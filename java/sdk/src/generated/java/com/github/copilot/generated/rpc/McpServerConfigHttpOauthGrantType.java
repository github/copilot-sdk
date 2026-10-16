/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * OAuth grant type to use when authenticating to the remote MCP server.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum McpServerConfigHttpOauthGrantType {
    /** The {@code authorization_code} variant. */
    AUTHORIZATION_CODE("authorization_code"),
    /** The {@code client_credentials} variant. */
    CLIENT_CREDENTIALS("client_credentials");

    private final String value;
    McpServerConfigHttpOauthGrantType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpServerConfigHttpOauthGrantType fromValue(String value) {
        for (McpServerConfigHttpOauthGrantType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpServerConfigHttpOauthGrantType value: " + value);
    }
}
