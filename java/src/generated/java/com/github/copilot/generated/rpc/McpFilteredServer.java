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
 * Schema for the `McpFilteredServer` type.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpFilteredServer(
    /** Filtered server name */
    @JsonProperty("name") String name,
    /** Human-readable filter reason */
    @JsonProperty("reason") String reason,
    /** PII-free filter reason */
    @JsonProperty("redactedReason") String redactedReason,
    /** Enterprise login associated with an allowlist policy */
    @JsonProperty("enterpriseName") String enterpriseName
) {
}
