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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * The outcome of reading a cloud session's logs. `resolved` distinguishes "this credential has no Copilot API endpoint" from "the request was made and failed"; every other field describes a request that was actually attempted.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record CloudTasksGetSessionLogsResult(
    /** Whether a Copilot API endpoint could be resolved for the credential at all. When false no request was attempted and every other field is absent or empty, which a caller must not report as a transport failure. */
    @JsonProperty("resolved") Boolean resolved,
    /** Whether the API returned the logs successfully. Meaningful only when `resolved` is true. */
    @JsonProperty("ok") Boolean ok,
    /** The logs, as returned by the API. Absent when none were returned; a caller that wants a list should treat absence as empty. */
    @JsonProperty("logsJson") String logsJson,
    /** Diagnostics the retrieval produced, in order. Worth logging whatever the outcome, because they explain a partial or empty result that is otherwise reported as success. */
    @JsonProperty("debugMessages") List<String> debugMessages,
    /** A retrieval fault that is not an HTTP status, such as a malformed response body. */
    @JsonProperty("errorString") String errorString,
    /** HTTP status code of the response, when one was received. */
    @JsonProperty("statusCode") Long statusCode,
    /** Human-readable failure explanation, when the call failed. */
    @JsonProperty("errorMessage") String errorMessage,
    /** Categorised failure kind, when the call failed. */
    @JsonProperty("errorKind") String errorKind,
    /** The error body the API returned, preserved verbatim so a host can surface the service's own message. */
    @JsonProperty("errorPayloadJson") String errorPayloadJson
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    CloudTasksGetSessionLogsResult {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static final class Builder {
        private Builder() {}
        private Boolean resolved;
        private Boolean ok;
        private String logsJson;
        private List<String> debugMessages;
        private String errorString;
        private Long statusCode;
        private String errorMessage;
        private String errorKind;
        private String errorPayloadJson;

        /**
         * Sets the {@code resolved} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder resolved(Boolean value) {
            this.resolved = value;
            return this;
        }

        /**
         * Sets the {@code ok} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder ok(Boolean value) {
            this.ok = value;
            return this;
        }

        /**
         * Sets the {@code logsJson} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder logsJson(String value) {
            this.logsJson = value;
            return this;
        }

        /**
         * Sets the {@code debugMessages} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder debugMessages(List<String> value) {
            this.debugMessages = value;
            return this;
        }

        /**
         * Sets the {@code errorString} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder errorString(String value) {
            this.errorString = value;
            return this;
        }

        /**
         * Sets the {@code statusCode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder statusCode(Long value) {
            this.statusCode = value;
            return this;
        }

        /**
         * Sets the {@code errorMessage} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder errorMessage(String value) {
            this.errorMessage = value;
            return this;
        }

        /**
         * Sets the {@code errorKind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder errorKind(String value) {
            this.errorKind = value;
            return this;
        }

        /**
         * Sets the {@code errorPayloadJson} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder errorPayloadJson(String value) {
            this.errorPayloadJson = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CloudTasksGetSessionLogsResult build() {
            if (resolved == null) throw new IllegalStateException("Missing required property: resolved");
            if (ok == null) throw new IllegalStateException("Missing required property: ok");
            return new CloudTasksGetSessionLogsResult(resolved, ok, logsJson, debugMessages, errorString, statusCode, errorMessage, errorKind, errorPayloadJson);
        }
    }
}
