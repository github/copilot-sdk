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
 * The applied host allowlist and effective session model policy after intersection.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionModelSetAllowedModelsResult(
    /** Normalized host allowlist. Omitted when the host restriction was cleared, or when a relay client does not return the host policy. */
    @JsonProperty("allowedModels") List<String> allowedModels,
    /** Effective exact IDs or repository policy patterns after applying the host restriction. Omitted by relay clients that do not return the host policy. */
    @JsonProperty("effectiveAllowedModels") List<String> effectiveAllowedModels,
    /** Effective deterministic fallback model, when the policy defines one. */
    @JsonProperty("fallbackModel") String fallbackModel,
    /** Selected session model after reconciling a now-disallowed concrete selection. */
    @JsonProperty("modelId") String modelId
) {
}
