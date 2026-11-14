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
 * Optional overrides for the runtime retry policy.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubFetchRetryConfig(
    /** Maximum retries; the runtime default is five. */
    @JsonProperty("maxRetries") Long maxRetries,
    /** Initial retry delay; the runtime default is five seconds. */
    @JsonProperty("defaultRetryDelaySeconds") Double defaultRetryDelaySeconds,
    /** Delay multiplier; the runtime default is two. */
    @JsonProperty("backoffFactor") Double backoffFactor,
    /** Whether repeated failures open the runtime circuit breaker. Defaults to true. */
    @JsonProperty("enableCircuitBreaker") Boolean enableCircuitBreaker
) {
}
