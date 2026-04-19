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
 * Result for the {@code session.ui.elicitation} RPC method.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionUiElicitationResult(
    /** The user's response: accept (submitted), decline (rejected), or cancel (dismissed) */
    @JsonProperty("action") SessionUiElicitationResultAction action,
    /** The form values submitted by the user (present when action is 'accept') */
    @JsonProperty("content") Map<String, Object> content
) {

    /** The user's response: accept (submitted), decline (rejected), or cancel (dismissed) */
    public enum SessionUiElicitationResultAction {
        /** The {@code accept} variant. */
        ACCEPT("accept"),
        /** The {@code decline} variant. */
        DECLINE("decline"),
        /** The {@code cancel} variant. */
        CANCEL("cancel");

        private final String value;
        SessionUiElicitationResultAction(String value) { this.value = value; }
        @com.fasterxml.jackson.annotation.JsonValue
        public String getValue() { return value; }
        @com.fasterxml.jackson.annotation.JsonCreator
        public static SessionUiElicitationResultAction fromValue(String value) {
            for (SessionUiElicitationResultAction v : values()) {
                if (v.value.equals(value)) return v;
            }
            throw new IllegalArgumentException("Unknown SessionUiElicitationResultAction value: " + value);
        }
    }
}
