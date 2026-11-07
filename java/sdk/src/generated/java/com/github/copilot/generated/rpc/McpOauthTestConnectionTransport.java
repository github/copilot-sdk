/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Remote protocol selected for the tokenless handshake.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
enum McpOauthTestConnectionTransport {
    /** The {@code http} variant. */
    HTTP("http"),
    /** The {@code sse} variant. */
    SSE("sse");

    private final String value;
    McpOauthTestConnectionTransport(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpOauthTestConnectionTransport fromValue(String value) {
        for (McpOauthTestConnectionTransport v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpOauthTestConnectionTransport value: " + value);
    }
}
