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
 * Progress and retry facts for one active download.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AzureMlDownloadStatus(
    /** Current transfer phase. */
    @JsonProperty("phase") String phase,
    /** Bytes completed across all files. */
    @JsonProperty("completedBytes") Long completedBytes,
    /** Exact bytes expected across all files. */
    @JsonProperty("totalBytes") Long totalBytes,
    /** Next retry attempt, if the transfer is retrying. */
    @JsonProperty("retryAttempt") Long retryAttempt,
    /** Configured retry limit, if the transfer is retrying. */
    @JsonProperty("maxAttempts") Long maxAttempts,
    /** Redacted failure reason, if the transfer is retrying. */
    @JsonProperty("retryReason") String retryReason,
    /** Planned delay before retry, if the transfer is retrying. */
    @JsonProperty("retryDelayMs") Long retryDelayMs
) {
}
