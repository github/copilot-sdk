/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Transport mechanism: stdio, http, sse (deprecated), or memory (in-process MCP server)
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum McpServerTransport {
    /** The {@code stdio} variant. */
    STDIO("stdio"),
    /** The {@code http} variant. */
    HTTP("http"),
    /** The {@code sse} variant. */
    SSE("sse"),
    /** The {@code memory} variant. */
    MEMORY("memory");

    private final String value;
    McpServerTransport(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpServerTransport fromValue(String value) {
        for (McpServerTransport v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpServerTransport value: " + value);
    }
}
