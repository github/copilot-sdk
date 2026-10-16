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
 * A server-advertised routing preference. Identifiers and execution types are extensible.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AutoTierDescriptor(
    /** Opaque routing identifier transmitted unchanged to the provider. */
    @JsonProperty("id") String id,
    /** Human-readable label, not a routing identifier. */
    @JsonProperty("displayName") String displayName,
    /** Description displayed beside the preference. */
    @JsonProperty("description") String description,
    /** Execution kind; this client supports `auto` preferences on the Auto model. */
    @JsonProperty("type") String type,
    /** Current account-specific availability. */
    @JsonProperty("status") AutoTierStatus status
) {
}
