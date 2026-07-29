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
 * Wire-only per-invocation factory resource ceiling overrides.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FactoryRunLimits(
    /** Maximum number of factory subagents that may run concurrently. */
    @JsonProperty("maxConcurrentSubagents") Long maxConcurrentSubagents,
    /** Maximum total number of factory subagents that may be admitted. */
    @JsonProperty("maxTotalSubagents") Long maxTotalSubagents,
    /** Maximum accumulated active-execution time in seconds. Active execution includes the entire extension body, subprocess waits, queued-agent waits, and sleeps; time between resumed attempts is not counted. */
    @JsonProperty("timeoutSeconds") Double timeoutSeconds,
    /** Maximum AI credits consumed by factory subagents and their descendants. The post-paid ceiling is soft: parallel turns can settle beyond it before the run stops. */
    @JsonProperty("maxAiCredits") Double maxAiCredits
) {
}
