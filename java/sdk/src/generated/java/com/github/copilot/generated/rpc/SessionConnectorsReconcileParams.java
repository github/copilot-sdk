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
 * Requests authoritative Connector-to-MCP reconciliation for the pinned account.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionConnectorsReconcileParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Opaque account selection ID. It must match the account already pinned to the session, if any. */
    @JsonProperty("accountId") String accountId,
    /** When true, refresh the catalog before reconciling. A disabled Connector API performs no service request. */
    @JsonProperty("refreshCatalog") Boolean refreshCatalog
) {
}
