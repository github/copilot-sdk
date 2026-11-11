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
 * JSON-RPC error.data for fetch.send/read transport failures (-32603). The transport-code token remains unchanged.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FetchTransportErrorData(
    /** The shared engine's failure category. */
    @JsonProperty("kind") FetchTransportErrorKind kind,
    /** The engine transport code, when available. An unclassified HTTP client failure uses "unknown". */
    @JsonProperty("code") String code,
    /** The engine's permanent-request-failure classification. This does not select a retry policy. */
    @JsonProperty("permanentRequestFailure") Boolean permanentRequestFailure,
    /** The engine's network-error classification. This does not select a retry policy. */
    @JsonProperty("networkError") Boolean networkError
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public FetchTransportErrorData {}

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
        private FetchTransportErrorKind kind;
        private String code;
        private Boolean permanentRequestFailure;
        private Boolean networkError;

        /**
         * Sets the {@code kind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder kind(FetchTransportErrorKind value) {
            this.kind = value;
            return this;
        }

        /**
         * Sets the {@code code} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder code(String value) {
            this.code = value;
            return this;
        }

        /**
         * Sets the {@code permanentRequestFailure} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder permanentRequestFailure(Boolean value) {
            this.permanentRequestFailure = value;
            return this;
        }

        /**
         * Sets the {@code networkError} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder networkError(Boolean value) {
            this.networkError = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public FetchTransportErrorData build() {
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            if (permanentRequestFailure == null) throw new IllegalStateException("Missing required property: permanentRequestFailure");
            if (networkError == null) throw new IllegalStateException("Missing required property: networkError");
            return new FetchTransportErrorData(kind, code, permanentRequestFailure, networkError);
        }
    }
}
