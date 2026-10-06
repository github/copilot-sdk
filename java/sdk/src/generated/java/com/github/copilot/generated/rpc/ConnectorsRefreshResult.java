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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Entries for the selected account.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConnectorsRefreshResult(
    /** Opaque account ID. */
    @JsonProperty("accountId") String accountId,
    /** Revision. */
    @JsonProperty("revision") Long revision,
    /** Refresh time in Unix epoch milliseconds. */
    @JsonProperty("refreshedAtMs") Long refreshedAtMs,
    /** Entries. */
    @JsonProperty("connectors") List<ConnectorDiscoveryCatalogEntry> connectors
) {
}
