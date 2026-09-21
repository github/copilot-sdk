/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Stable OAuth scope whose absence prevents Connector management.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ConnectorAuthorizationScope {
    /** The {@code write_plugin_gateway_connections} variant. */
    WRITE_PLUGIN_GATEWAY_CONNECTIONS("write_plugin_gateway_connections");

    private final String value;
    ConnectorAuthorizationScope(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ConnectorAuthorizationScope fromValue(String value) {
        for (ConnectorAuthorizationScope v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ConnectorAuthorizationScope value: " + value);
    }
}
