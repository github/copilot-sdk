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
 * Session event "model.call_failure". Failed LLM API call metadata for telemetry
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ModelCallFailureEvent extends SessionEvent {

    @Override
    public String getType() { return "model.call_failure"; }

    @JsonProperty("data")
    private ModelCallFailureEventData data;

    public ModelCallFailureEventData getData() { return data; }
    public void setData(ModelCallFailureEventData data) { this.data = data; }

    /** Data payload for {@link ModelCallFailureEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ModelCallFailureEventData(
        /** Model identifier used for the failed API call */
        @JsonProperty("model") String model,
        /** What initiated this API call (e.g., "sub-agent", "mcp-sampling"); absent for user-initiated calls */
        @JsonProperty("initiator") String initiator,
        /** Completion ID from the model provider (e.g., chatcmpl-abc123) */
        @JsonProperty("apiCallId") String apiCallId,
        /** GitHub request tracing ID (x-github-request-id header) for server-side log correlation */
        @JsonProperty("providerCallId") String providerCallId,
        /** Copilot service request ID (x-copilot-service-request-id header) for CAPI log correlation */
        @JsonProperty("serviceRequestId") String serviceRequestId,
        /** HTTP status code from the failed request */
        @JsonProperty("statusCode") Long statusCode,
        /** Duration of the failed API call in milliseconds */
        @JsonProperty("durationMs") Long durationMs,
        /** Where the failed model call originated */
        @JsonProperty("source") ModelCallFailureSource source,
        /** Raw provider/runtime error message for restricted telemetry */
        @JsonProperty("errorMessage") String errorMessage,
        /** For HTTP 400 failures only: whether the response carried a structured CAPI error envelope (structured_error, a deterministic validation failure) or no error body (bodyless, the transient gateway/proxy signature). Absent for non-400 failures. */
        @JsonProperty("badRequestKind") ModelCallFailureBadRequestKind badRequestKind,
        /** For HTTP 400 failures only: the `code` from the CAPI error envelope (e.g. 'model_max_prompt_tokens_exceeded') identifying which deterministic validation failure occurred. Raw server-controlled string, emitted only through restricted telemetry. Absent for bodyless or non-400 failures. */
        @JsonProperty("errorCode") String errorCode,
        /** For HTTP 400 failures only: the `type` from the CAPI error envelope (e.g. 'websocket_error'), a coarser companion to errorCode for envelopes that carry no code. Raw server-controlled string, emitted only through restricted telemetry. Absent for bodyless or non-400 failures. */
        @JsonProperty("errorType") String errorType
    ) {
    }
}
