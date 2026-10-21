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
 * Validated local runtime requirements for an Azure ML model.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AzureMlProfile(
    /** Sanitized model-picker display label. */
    @JsonProperty("displayName") String displayName,
    /** Published download size, when available. */
    @JsonProperty("downloadBytes") Long downloadBytes,
    /** Peak disk use during installation, when available. */
    @JsonProperty("peakDiskBytes") Long peakDiskBytes,
    /** Recommended graphics memory, when available. */
    @JsonProperty("recommendedVramBytes") Long recommendedVramBytes,
    /** Maximum input tokens accepted by the model. */
    @JsonProperty("maxPromptTokens") Long maxPromptTokens,
    /** Maximum context window accepted by the model. */
    @JsonProperty("maxContextWindowTokens") Long maxContextWindowTokens,
    /** Maximum output tokens produced by the model. */
    @JsonProperty("maxOutputTokens") Long maxOutputTokens,
    /** Whether the model supports tool calls. */
    @JsonProperty("supportsToolCalls") Boolean supportsToolCalls,
    /** Validated local runtime configuration. */
    @JsonProperty("serveOptions") Object serveOptions,
    /** Published license, when available. */
    @JsonProperty("license") AzureMlLicense license
) {
}
