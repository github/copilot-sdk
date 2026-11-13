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
 * Attribution for the adapter that produced a provider row.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelProviderProvenance(
    /** Stable opaque adapter identity from the effective catalog. Treat this as a whole identifier, not a parseable owner or kind. */
    @JsonProperty("adapterId") String adapterId,
    /** Descriptive provider family that produced this row; not a routing key. */
    @JsonProperty("providerKind") String providerKind,
    /** Kind of component that supplied the adapter. */
    @JsonProperty("source") ModelProviderProvenanceSource source,
    /** Stable contributor identifier when the adapter has an owner outside the runtime. Independent of the contribution mechanism and not a routing key. */
    @JsonProperty("ownerId") String ownerId,
    /** Human-readable contributor name, not the adapter display name. */
    @JsonProperty("ownerDisplayName") String ownerDisplayName
) {
}
