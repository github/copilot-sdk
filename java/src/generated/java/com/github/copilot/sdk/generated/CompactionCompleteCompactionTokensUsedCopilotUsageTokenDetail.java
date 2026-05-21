/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Token usage detail for a single billing category
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CompactionCompleteCompactionTokensUsedCopilotUsageTokenDetail(
    /** Number of tokens in this billing batch */
    @JsonProperty("batchSize") Double batchSize,
    /** Cost per batch of tokens */
    @JsonProperty("costPerBatch") Double costPerBatch,
    /** Total token count for this entry */
    @JsonProperty("tokenCount") Double tokenCount,
    /** Token category (e.g., "input", "output") */
    @JsonProperty("tokenType") String tokenType
) {
}
