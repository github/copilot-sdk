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
 * A normalized model-provider instance discovered by the runtime.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelProviderInstance(
    /** Self-contained reference for subsequent provider operations. */
    @JsonProperty("reference") ModelProviderInstanceReference reference,
    /** Human-readable instance name. */
    @JsonProperty("displayName") String displayName,
    /** Attribution for the adapter that produced this instance. */
    @JsonProperty("provenance") ModelProviderProvenance provenance,
    /** Inference API endpoint when the provider exposes one separately from its management endpoint. */
    @JsonProperty("inferenceEndpoint") String inferenceEndpoint,
    /** Provider family to use for inference against this instance. */
    @JsonProperty("inferenceType") ProviderEndpointType inferenceType,
    /** Wire API to use for inference against this instance, when required by the provider family. */
    @JsonProperty("inferenceWireApi") ProviderEndpointWireApi inferenceWireApi,
    /** Transport to use for inference against this instance. */
    @JsonProperty("inferenceTransport") ProviderEndpointTransport inferenceTransport
) {
}
