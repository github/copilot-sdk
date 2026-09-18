/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Authoritative session connector state. Account IDs are opaque routing identifiers and credentials are never included.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConnectorStatus(
    /** Connector API contract version. */
    @JsonProperty("apiVersion") Long apiVersion,
    /** Current feature and session availability. */
    @JsonProperty("availability") ConnectorAvailability availability,
    /** Opaque account selection pinned to this session, when one has been selected. */
    @JsonProperty("accountId") String accountId,
    /** Latest validated catalog snapshot, when available. */
    @JsonProperty("catalog") ConnectorCatalogResult catalog,
    /** Live MCP status for every Connector-owned runtime server. */
    @JsonProperty("runtimeServers") List<ConnectorRuntimeStatus> runtimeServers,
    /** Number of active opaque connection continuations. */
    @JsonProperty("pendingConnections") Long pendingConnections
) {
}
