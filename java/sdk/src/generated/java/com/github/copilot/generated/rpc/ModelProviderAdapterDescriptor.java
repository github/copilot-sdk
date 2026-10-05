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
 * A normalized model-provider adapter in the session's effective catalog.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelProviderAdapterDescriptor(
    /** Stable opaque identity for routing to this adapter. Unique in the effective catalog, independent of live registration generations. */
    @JsonProperty("adapterId") String adapterId,
    /** Descriptive provider family, such as `ollama`. Different adapters may have the same family; use adapterId for routing. */
    @JsonProperty("providerKind") String providerKind,
    /** Human-readable provider name. */
    @JsonProperty("displayName") String displayName,
    /** Attribution for the adapter itself. */
    @JsonProperty("provenance") ModelProviderAttribution provenance,
    /** Adapter-declared policy for passive and automatic discovery. */
    @JsonProperty("automaticDiscovery") ModelProviderAutomaticDiscoveryPolicy automaticDiscovery,
    /** Operations supported by this provider adapter. */
    @JsonProperty("operations") List<ModelProviderAdapterOperationDescriptor> operations
) {
}
