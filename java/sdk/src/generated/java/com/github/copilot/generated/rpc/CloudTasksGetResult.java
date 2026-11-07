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
 * The HTTP outcome of reading a cloud task, rather than the task alone. A 404 arrives here as a non-ok result with `statusCode` 404, not as an error, so a caller can tell an absent task apart from a failed call.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record CloudTasksGetResult(
    /** Whether the API returned the task successfully. */
    @JsonProperty("ok") Boolean ok,
    /** The task record, as returned by the API. Present only when `ok` is true; a caller parses it itself because the runtime does not narrow the API's own shape. */
    @JsonProperty("payloadJson") String payloadJson,
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
    CloudTasksGetResult {}

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
        private String payloadJson;
        private Long statusCode;
        private String errorMessage;
        private String errorKind;
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
         * Sets the {@code payloadJson} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder payloadJson(String value) {
            this.payloadJson = value;
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
        public CloudTasksGetResult build() {
            if (ok == null) throw new IllegalStateException("Missing required property: ok");
            return new CloudTasksGetResult(ok, payloadJson, statusCode, errorMessage, errorKind, errorPayloadJson);
        }
    }
}
