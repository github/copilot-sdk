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
 * An account the host has signed in to, identified by the server it lives on and the login it uses there. The same person can appear more than once when they use both github.com and an Enterprise server.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record LoggedInUser(
    /** Host the account belongs to, such as `github.com` or an Enterprise server. */
    @JsonProperty("host") String host,
    /** Account login on that host. */
    @JsonProperty("login") String login,
    /** Account kind, when the host recorded one. Consumers must tolerate new strings. */
    @JsonProperty("kind") String kind,
    /** Source account this account was derived from, when one was recorded. */
    @JsonProperty("derivedFrom") String derivedFrom
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    LoggedInUser {}

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
        private String host;
        private String login;
        private String kind;
        private String derivedFrom;

        /**
         * Sets the {@code host} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder host(String value) {
            this.host = value;
            return this;
        }

        /**
         * Sets the {@code login} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder login(String value) {
            this.login = value;
            return this;
        }

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
         * Sets the {@code derivedFrom} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder derivedFrom(String value) {
            this.derivedFrom = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public LoggedInUser build() {
            if (host == null) throw new IllegalStateException("Missing required property: host");
            if (login == null) throw new IllegalStateException("Missing required property: login");
            return new LoggedInUser(host, login, kind, derivedFrom);
        }
    }
}
