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
    /** Factory active-run timeout in milliseconds. */
    @JsonProperty("timeout") Double timeout
) {
}
