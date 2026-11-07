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
 * The outcome of starting a cloud task. A rejected request and a failed one are reported separately: `reason` and `message` describe a request the service declined, while `statusCode` and `errorMessage` describe the HTTP call itself and appear only when one was actually made.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record CloudTasksCreateResult(
    /** Whether the task was created. */
    @JsonProperty("ok") Boolean ok,
    /** The created task record, as returned by the service. Present only when `ok` is true; a caller parses it itself because the runtime does not narrow the service's own shape. */
    @JsonProperty("responseJson") String responseJson,
    /** Why the request was declined, when it was. Carried as a free-form string rather than a fixed set because the vocabulary is the service's and grows with it; `policy_blocked`, `validation`, `rate_limited` and `error` are the values seen so far, and a caller should treat an unrecognised one as a generic failure rather than rejecting the response. */
    @JsonProperty("reason") String reason,
    /** The explanation to show for a declined request, when the service supplied one. This describes the rejection itself, unlike `errorMessage`, which describes a failed HTTP call. */
    @JsonProperty("message") String message,
    /** HTTP status code of the response, when one was received. */
    @JsonProperty("statusCode") Long statusCode,
    /** Human-readable explanation of a failed HTTP call, when one failed. */
    @JsonProperty("errorMessage") String errorMessage,
    /** The error body the service returned, preserved verbatim so a host can surface the service's own message. */
    @JsonProperty("errorPayloadJson") String errorPayloadJson
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    CloudTasksCreateResult {}

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
        private Boolean ok;
        private String responseJson;
        private String reason;
        private String message;
        private Long statusCode;
        private String errorMessage;
        private String errorPayloadJson;

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
         * Sets the {@code responseJson} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder responseJson(String value) {
            this.responseJson = value;
            return this;
        }

        /**
         * Sets the {@code reason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder reason(String value) {
            this.reason = value;
            return this;
        }

        /**
         * Sets the {@code message} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder message(String value) {
            this.message = value;
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
        public CloudTasksCreateResult build() {
            if (ok == null) throw new IllegalStateException("Missing required property: ok");
            return new CloudTasksCreateResult(ok, responseJson, reason, message, statusCode, errorMessage, errorPayloadJson);
        }
    }
}
