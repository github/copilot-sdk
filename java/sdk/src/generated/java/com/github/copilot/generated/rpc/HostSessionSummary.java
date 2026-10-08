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
 * Metadata advertised by the AHP host, not a guarantee that history can be resumed.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record HostSessionSummary(
    /** Stable AHP resource URI identifying the session on this host. */
    @JsonProperty("resource") String resource,
    /** Authoritative title advertised by the host. */
    @JsonProperty("title") String title,
    /** Session creation time as an ISO 8601 timestamp. */
    @JsonProperty("createdAt") String createdAt,
    /** Last modification time as an ISO 8601 timestamp. */
    @JsonProperty("modifiedAt") String modifiedAt,
    /** Unsigned 32-bit AHP session status bitset (0..=4294967295); unknown bits must be preserved. */
    @JsonProperty("status") Long status,
    /** Optional activity description advertised by the host. */
    @JsonProperty("activity") String activity
) {
}
