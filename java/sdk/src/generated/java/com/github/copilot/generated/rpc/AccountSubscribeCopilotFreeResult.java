/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * What came of a Copilot Free enrollment attempt.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record AccountSubscribeCopilotFreeResult(
    /** `subscribed` when GitHub answered, and `status` then carries its answer. `no-token` when no credential could be resolved for the identity, which is the caller's cue to ask the user to sign in again rather than to report a failure. `failed` when the attempt itself errored, and `message` then explains it. */
    @JsonProperty("kind") String kind,
    /** GitHub's enrollment status when `kind` is `subscribed`, for example `subscribed` or `already_subscribed`. Absent otherwise. */
    @JsonProperty("status") String status,
    /** Explanation to show the user. Present on `failed`, and optionally alongside a `subscribed` status. */
    @JsonProperty("message") String message
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    AccountSubscribeCopilotFreeResult {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    public static final class Builder {
        private Builder() {}
        private String kind;
        private String status;
        private String message;

        /**
         * Sets the {@code kind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder kind(String value) {
            this.kind = value;
            return this;
        }

        /**
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder status(String value) {
            this.status = value;
            return this;
        }

        /**
         * Sets the {@code message} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder message(String value) {
            this.message = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AccountSubscribeCopilotFreeResult build() {
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            return new AccountSubscribeCopilotFreeResult(kind, status, message);
        }
    }
}
