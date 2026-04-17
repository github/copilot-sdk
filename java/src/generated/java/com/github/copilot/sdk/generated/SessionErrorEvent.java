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
 * The {@code session.error} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionErrorEvent extends SessionEvent {

    @Override
    public String getType() { return "session.error"; }

    @JsonProperty("data")
    private SessionErrorEventData data;

    public SessionErrorEventData getData() { return data; }
    public void setData(SessionErrorEventData data) { this.data = data; }

    /** Data payload for {@link SessionErrorEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionErrorEventData(
        /** Category of error (e.g., "authentication", "authorization", "quota", "rate_limit", "context_limit", "query") */
        @JsonProperty("errorType") String errorType,
        /** Human-readable error message */
        @JsonProperty("message") String message,
        /** Error stack trace, when available */
        @JsonProperty("stack") String stack,
        /** HTTP status code from the upstream request, if applicable */
        @JsonProperty("statusCode") Long statusCode,
        /** GitHub request tracing ID (x-github-request-id header) for correlating with server-side logs */
        @JsonProperty("providerCallId") String providerCallId,
        /** Optional URL associated with this error that the user can open in a browser */
        @JsonProperty("url") String url
    ) {
    }
}
