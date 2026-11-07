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
 * One canonical signed-in account entry shared by account-list and account-switch surfaces.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountStatus(
    /** Stable opaque account id used for forest lineage and account management. */
    @JsonProperty("accountId") String accountId,
    /** Authentication host URL. */
    @JsonProperty("host") String host,
    /** Authenticated login/username. */
    @JsonProperty("login") String login,
    /** Human-readable identity and provider label owned by the runtime. */
    @JsonProperty("label") String label,
    /** The provider kind of this account. */
    @JsonProperty("kind") AccountKind kind,
    /** Human-readable credential source used to authenticate this account, such as `gh` or `GITHUB_TOKEN`. */
    @JsonProperty("authSource") String authSource,
    /** Opaque accountId of the account this one was derived from; absent for a root account. */
    @JsonProperty("derivedFrom") String derivedFrom,
    /** Whether this is the active account. */
    @JsonProperty("active") Boolean active,
    /** Opaque selection id accepted by switchActive. Absent when switching is not currently valid. */
    @JsonProperty("selectionId") String selectionId,
    /** Whether the account is currently a valid switch target. */
    @JsonProperty("switchable") Boolean switchable,
    /** Canonical availability treatment for this account. */
    @JsonProperty("health") AccountHealth health,
    /** Human-readable availability or recovery reason. */
    @JsonProperty("statusMessage") String statusMessage
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AccountStatus {}

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
        private String accountId;
        private String host;
        private String login;
        private String label;
        private AccountKind kind;
        private String authSource;
        private String derivedFrom;
        private Boolean active;
        private String selectionId;
        private Boolean switchable;
        private AccountHealth health;
        private String statusMessage;

        /**
         * Sets the {@code accountId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder accountId(String value) {
            this.accountId = value;
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
         * Sets the {@code label} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder label(String value) {
            this.label = value;
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
         * Sets the {@code authSource} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder authSource(String value) {
            this.authSource = value;
            return this;
        }

        /**
         * Sets the {@code derivedFrom} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder derivedFrom(String value) {
            this.derivedFrom = value;
            return this;
        }

        /**
         * Sets the {@code active} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder active(Boolean value) {
            this.active = value;
            return this;
        }

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
         * Sets the {@code switchable} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder switchable(Boolean value) {
            this.switchable = value;
            return this;
        }

        /**
         * Sets the {@code health} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder health(AccountHealth value) {
            this.health = value;
            return this;
        }

        /**
         * Sets the {@code statusMessage} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder statusMessage(String value) {
            this.statusMessage = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AccountStatus build() {
            if (host == null) throw new IllegalStateException("Missing required property: host");
            if (login == null) throw new IllegalStateException("Missing required property: login");
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            if (active == null) throw new IllegalStateException("Missing required property: active");
            return new AccountStatus(accountId, host, login, label, kind, authSource, derivedFrom, active, selectionId, switchable, health, statusMessage);
        }
    }

    /**
     * Retains the constructor published in Java SDK 1.0.28.
     * @deprecated Use named construction via {@link #builder()}.
     * @param host Authentication host URL.
     * @param login Authenticated login/username.
     * @param kind The provider kind of this account.
     * @param authSource Human-readable credential source used to authenticate this account, such as `gh` or `GITHUB_TOKEN`.
     * @param derivedFrom Opaque accountId of the account this one was derived from; absent for a root account.
     * @param active Whether this is the active account.
     * @param selectionId Opaque selection id accepted by switchActive. Absent when switching is not currently valid.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AccountStatus(
        String host,
        String login,
        AccountKind kind,
        String authSource,
        String derivedFrom,
        Boolean active,
        String selectionId
    ) {
        this(null, host, login, null, kind, authSource, derivedFrom, active, selectionId, null, null, null);
    }
}
