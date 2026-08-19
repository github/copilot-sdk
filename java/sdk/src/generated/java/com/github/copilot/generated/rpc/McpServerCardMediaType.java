/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * JSON MCP card media type accepted for install planning
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum McpServerCardMediaType {
    /** The {@code application/mcp-server-card+json} variant. */
    APPLICATION_MCP_SERVER_CARD+JSON("application/mcp-server-card+json"),
    /** The {@code application/mcp-server+json} variant. */
    APPLICATION_MCP_SERVER+JSON("application/mcp-server+json");

    private final String value;
    McpServerCardMediaType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpServerCardMediaType fromValue(String value) {
        for (McpServerCardMediaType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpServerCardMediaType value: " + value);
    }
}
