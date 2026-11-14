/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Evidence about the summarizer's latest model call when a compaction failed. Every value is a string so that new values stay readable by older runtimes.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CompactionCompleteFailureDiagnostics(
    /** Failure reason of the summarizer's latest failed model call, kept when a later attempt or the compaction deadline ended the compaction. Uses the failureReason values. */
    @JsonProperty("lastAttemptFailureReason") String lastAttemptFailureReason,
    /** Error code the provider returned for that call, or its error type when it gave no code */
    @JsonProperty("providerErrorCode") String providerErrorCode,
    /** Bounded kind of that call's transport failure, such as dns, tls, proxy, connection_refused, connection_reset, connect_timeout, or timeout */
    @JsonProperty("transportErrorKind") String transportErrorKind,
    /** Finish reason of the summarizer's latest successful response, such as stop, length, or content_filter */
    @JsonProperty("finishReason") String finishReason
) {
}
