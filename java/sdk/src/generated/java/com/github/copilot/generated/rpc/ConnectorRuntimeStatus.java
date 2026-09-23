/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Live status of one session-owned MCP projection.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConnectorRuntimeStatus(
    /** Opaque runtime server ID. */
    @JsonProperty("runtimeServerId") String runtimeServerId,
    /** Canonical Connector name that owns this server. */
    @JsonProperty("connectorName") String connectorName,
    /** Current live MCP host status. */
    @JsonProperty("status") ConnectorMcpStatus status
) {
}
