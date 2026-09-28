/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Transport exposed by a remote endpoint
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum McpPlanRemoteTransport {
    /** The {@code http} variant. */
    HTTP("http"),
    /** The {@code streamable-http} variant. */
    STREAMABLE_HTTP("streamable-http"),
    /** The {@code sse} variant. */
    SSE("sse");

    private final String value;
    McpPlanRemoteTransport(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpPlanRemoteTransport fromValue(String value) {
        for (McpPlanRemoteTransport v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpPlanRemoteTransport value: " + value);
    }
}
