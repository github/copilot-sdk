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
 * Serializable reference to a discovered provider instance.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelProviderInstanceReference(
    /** Stable opaque identity of the adapter that owns this reference. Must be present in the target session's effective catalog. */
    @JsonProperty("adapterId") String adapterId,
    /** Descriptive provider family. Must match the selected adapter; not a routing key. */
    @JsonProperty("providerKind") String providerKind,
    /** Stable instance identifier derived by the provider adapter, such as `ollama:{normalizedEndpoint}`. */
    @JsonProperty("id") String id,
    /** Absolute provider management URI. The adapter validates normalization, supported schemes, and permission to access it against its bound configuration; a reference does not grant authority. */
    @JsonProperty("managementEndpoint") String managementEndpoint
) {
}
