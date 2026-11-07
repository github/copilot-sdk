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
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * An inert runtime-issued login handle. Preparation alone performs no activation or OAuth work.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMcpOauthPrepareLoginResult(
    /** Retain with the original requester and use for one login or cancellation. */
    @JsonProperty("loginId") String loginId,
    /** Original expiry, not extended by consumption, retries or cancellation. */
    @JsonProperty("expiresAt") OffsetDateTime expiresAt
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionMcpOauthPrepareLoginResult {}

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
        private String loginId;
        private OffsetDateTime expiresAt;

        /**
         * Sets the {@code loginId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder loginId(String value) {
            this.loginId = value;
            return this;
        }

        /**
         * Sets the {@code expiresAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder expiresAt(OffsetDateTime value) {
            this.expiresAt = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionMcpOauthPrepareLoginResult build() {
            if (loginId == null) throw new IllegalStateException("Missing required property: loginId");
            if (expiresAt == null) throw new IllegalStateException("Missing required property: expiresAt");
            return new SessionMcpOauthPrepareLoginResult(loginId, expiresAt);
        }
    }
}
