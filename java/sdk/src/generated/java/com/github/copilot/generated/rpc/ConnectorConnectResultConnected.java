/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * The service is connected and the session MCP graph was reconciled.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ConnectorConnectResultConnected extends ConnectorConnectResult {

    @JsonProperty("kind")
    private final String kind = "connected";

    @Override
    public String getKind() { return kind; }

    /** Fresh authoritative Connector state after MCP reconciliation. */
    @JsonProperty("status")
    private ConnectorStatus status;

    public ConnectorStatus getStatus() { return status; }
    public void setStatus(ConnectorStatus status) { this.status = status; }
}
