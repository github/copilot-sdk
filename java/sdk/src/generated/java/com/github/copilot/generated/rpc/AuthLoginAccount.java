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
 * A credential-free account choice after sign-in.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthLoginAccount(
    /** Opaque identifier supplied to the next login step to select this account. */
    @JsonProperty("selectionId") String selectionId,
    /** Host coordinate owned by the selected account's provider. */
    @JsonProperty("host") String host,
    /** Human-readable login for the account choice. */
    @JsonProperty("login") String login,
    /** Provider kind that owns this account choice. */
    @JsonProperty("kind") AccountKind kind
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AuthLoginAccount {}

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
        private String selectionId;
        private String host;
        private String login;
        private AccountKind kind;

        /**
         * Sets the {@code selectionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder selectionId(String value) {
            this.selectionId = value;
            return this;
        }

        /**
         * Sets the {@code host} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
        public Builder kind(AccountKind value) {
            this.kind = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AuthLoginAccount build() {
            if (selectionId == null) throw new IllegalStateException("Missing required property: selectionId");
            if (host == null) throw new IllegalStateException("Missing required property: host");
            if (login == null) throw new IllegalStateException("Missing required property: login");
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            return new AuthLoginAccount(selectionId, host, login, kind);
        }
    }
}
