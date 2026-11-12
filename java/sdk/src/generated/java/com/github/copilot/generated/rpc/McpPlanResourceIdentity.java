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
 * Normalised identity of the MCP server a plan targets, independent of how the card spelled it.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpPlanResourceIdentity(
    /** Canonical, normalised name of the server, for example `io.github.owner/server`. */
    @JsonProperty("canonicalName") String canonicalName,
    /** Local configuration key the server would be recorded under. */
    @JsonProperty("serverName") String serverName,
    /** Version advertised by the card, when it declares one. */
    @JsonProperty("version") String version,
    /** Registry identifier of the server, when it came from a registry. */
    @JsonProperty("registryId") String registryId
) {
}
