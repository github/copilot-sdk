/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Request parameters for the {@code session.ui.handlePendingElicitation} RPC method.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionUiHandlePendingElicitationParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** The unique request ID from the elicitation.requested event */
    @JsonProperty("requestId") String requestId,
    /** The elicitation response (accept with form values, decline, or cancel) */
    @JsonProperty("result") SessionUiHandlePendingElicitationParamsResult result
) {

    /** The elicitation response (accept with form values, decline, or cancel) */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionUiHandlePendingElicitationParamsResult(
        /** The user's response: accept (submitted), decline (rejected), or cancel (dismissed) */
        @JsonProperty("action") SessionUiHandlePendingElicitationParamsResultAction action,
        /** The form values submitted by the user (present when action is 'accept') */
        @JsonProperty("content") Map<String, Object> content
    ) {

        /** The user's response: accept (submitted), decline (rejected), or cancel (dismissed) */
        public enum SessionUiHandlePendingElicitationParamsResultAction {
            /** The {@code accept} variant. */
            ACCEPT("accept"),
            /** The {@code decline} variant. */
            DECLINE("decline"),
            /** The {@code cancel} variant. */
            CANCEL("cancel");

            private final String value;
            SessionUiHandlePendingElicitationParamsResultAction(String value) { this.value = value; }
            @com.fasterxml.jackson.annotation.JsonValue
            public String getValue() { return value; }
            @com.fasterxml.jackson.annotation.JsonCreator
            public static SessionUiHandlePendingElicitationParamsResultAction fromValue(String value) {
                for (SessionUiHandlePendingElicitationParamsResultAction v : values()) {
                    if (v.value.equals(value)) return v;
                }
                throw new IllegalArgumentException("Unknown SessionUiHandlePendingElicitationParamsResultAction value: " + value);
            }
        }
    }
}
