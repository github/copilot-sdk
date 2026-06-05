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
 * Target model identifier and optional reasoning effort, summary, capability overrides, and context tier.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionModelSwitchToParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Model identifier to switch to */
    @JsonProperty("modelId") String modelId,
    /** Reasoning effort level to use for the model. "none" disables reasoning. */
    @JsonProperty("reasoningEffort") String reasoningEffort,
    /** Reasoning summary mode to request for supported model clients */
    @JsonProperty("reasoningSummary") ReasoningSummary reasoningSummary,
    /** Override individual model capabilities resolved by the runtime */
    @JsonProperty("modelCapabilities") ModelCapabilitiesOverride modelCapabilities,
    /** Explicit context tier for the selected model. `"default"` / `"long_context"` apply the requested tier; omit this field to use normal model behavior with no explicit tier. */
    @JsonProperty("contextTier") ContextTier contextTier
) {
}
