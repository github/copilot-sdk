/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Live MCP status of one Connector-owned runtime server.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ConnectorMcpStatus {
    /** The {@code connected} variant. */
    CONNECTED("connected"),
    /** The {@code pending} variant. */
    PENDING("pending"),
    /** The {@code needs_auth} variant. */
    NEEDS_AUTH("needs_auth"),
    /** The {@code failed} variant. */
    FAILED("failed"),
    /** The {@code stopped} variant. */
    STOPPED("stopped"),
    /** The {@code disabled} variant. */
    DISABLED("disabled"),
    /** The {@code not_configured} variant. */
    NOT_CONFIGURED("not_configured");

    private final String value;
    ConnectorMcpStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ConnectorMcpStatus fromValue(String value) {
        for (ConnectorMcpStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ConnectorMcpStatus value: " + value);
    }
}
