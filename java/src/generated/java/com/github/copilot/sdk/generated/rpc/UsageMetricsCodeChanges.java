/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Aggregated code change metrics
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsageMetricsCodeChanges(
    /** Total lines of code added */
    @JsonProperty("linesAdded") Long linesAdded,
    /** Total lines of code removed */
    @JsonProperty("linesRemoved") Long linesRemoved,
    /** Number of distinct files modified */
    @JsonProperty("filesModifiedCount") Long filesModifiedCount
) {
}
