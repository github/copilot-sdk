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
 * Declared or approved workflow resource ceilings.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowDeclaredLimits(
    /** Maximum concurrently active subagents. */
    @JsonProperty("maxConcurrentSubagents") Long maxConcurrentSubagents,
    /** Maximum total subagents spawned by the run. */
    @JsonProperty("maxTotalSubagents") Long maxTotalSubagents,
    /** Maximum accumulated active execution time in seconds. */
    @JsonProperty("timeoutSeconds") Double timeoutSeconds,
    /** Maximum AI credits consumed by subagents and descendants. */
    @JsonProperty("maxAiCredits") Double maxAiCredits
) {
}
