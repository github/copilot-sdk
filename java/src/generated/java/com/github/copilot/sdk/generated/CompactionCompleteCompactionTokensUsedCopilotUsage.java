/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Per-request cost and usage data from the CAPI copilot_usage response field
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CompactionCompleteCompactionTokensUsedCopilotUsage(
    /** Itemized token usage breakdown */
    @JsonProperty("tokenDetails") List<CompactionCompleteCompactionTokensUsedCopilotUsageTokenDetail> tokenDetails,
    /** Total cost in nano-AIU (AI Units) for this request */
    @JsonProperty("totalNanoAiu") Double totalNanoAiu
) {
}
