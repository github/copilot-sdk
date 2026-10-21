/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * How a reviewed plugin's MCP server is reached.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum PluginInstallationMcpTransport {
    /** The {@code remote} variant. */
    REMOTE("remote"),
    /** The {@code local} variant. */
    LOCAL("local");

    private final String value;
    PluginInstallationMcpTransport(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static PluginInstallationMcpTransport fromValue(String value) {
        for (PluginInstallationMcpTransport v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown PluginInstallationMcpTransport value: " + value);
    }
}
