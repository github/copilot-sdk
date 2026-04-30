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
 * The {@code model.call_failure} session event.
 *
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
        /** HTTP status code from the failed request */
        @JsonProperty("statusCode") Long statusCode,
        /** Duration of the failed API call in milliseconds */
        @JsonProperty("durationMs") Double durationMs,
        /** Where the failed model call originated */
        @JsonProperty("source") ModelCallFailureSource source,
        /** Raw provider/runtime error message for restricted telemetry */
        @JsonProperty("errorMessage") String errorMessage
    ) {
    }
}
