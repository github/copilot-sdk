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
 * A neutral reference to the model provider that produced a model: an opaque id, a human-readable label, and the provider kind. Carried on each enumerated Model so consumers can group by provider without reaching into a provider-shaped internal type.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelProviderRef(
    /** Opaque, stable id of the provider that produced this model. Matches the enumerated `ModelProviderDescriptor.id`. */
    @JsonProperty("id") String id,
    /** Human-readable provider label, owned by the runtime so every consumer renders identical text. */
    @JsonProperty("label") String label,
    /** The provider kind. */
    @JsonProperty("kind") ModelProviderKind kind
) {
}
