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
 * Declared or approved factory resource ceilings.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FactoryDeclaredLimits(
    @JsonProperty("maxConcurrentSubagents") Long maxConcurrentSubagents,
    @JsonProperty("maxTotalSubagents") Long maxTotalSubagents,
    @JsonProperty("timeoutSeconds") Double timeoutSeconds,
    @JsonProperty("maxAiCredits") Double maxAiCredits
) {
}
