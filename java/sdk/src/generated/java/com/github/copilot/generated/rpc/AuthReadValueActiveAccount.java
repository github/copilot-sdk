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
 * Variant {@code activeAccount} of {@link AuthReadValue}.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AuthReadValueActiveAccount extends AuthReadValue {

    @JsonProperty("kind")
    private final String kind = "activeAccount";

    @Override
    public String getKind() { return kind; }

    /** The active account, or absent when not logged in. */
    @JsonProperty("account")
    private AccountStatus account;

    /** Credential-free identity metadata for the active account, including resolved Copilot user information when available. */
    @JsonProperty("authInfo")
    private AuthIdentity authInfo;

    /** Opaque credential selection id for the active authentication. This remains available when the canonical account is not currently switchable. */
    @JsonProperty("authSelectionId")
    private String authSelectionId;

    public AccountStatus getAccount() { return account; }
    public void setAccount(AccountStatus account) { this.account = account; }

    public AuthIdentity getAuthInfo() { return authInfo; }
    public void setAuthInfo(AuthIdentity authInfo) { this.authInfo = authInfo; }

    public String getAuthSelectionId() { return authSelectionId; }
    public void setAuthSelectionId(String authSelectionId) { this.authSelectionId = authSelectionId; }


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
        private AccountStatus account;
        private AuthIdentity authInfo;
        private String authSelectionId;

        /**
         * Sets the {@code account} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder account(AccountStatus value) {
            this.account = value;
            return this;
        }

        /**
         * Sets the {@code authInfo} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder authInfo(AuthIdentity value) {
            this.authInfo = value;
            return this;
        }

        /**
         * Sets the {@code authSelectionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder authSelectionId(String value) {
            this.authSelectionId = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AuthReadValueActiveAccount build() {
            AuthReadValueActiveAccount instance = new AuthReadValueActiveAccount();
            instance.setAccount(account);
            instance.setAuthInfo(authInfo);
            instance.setAuthSelectionId(authSelectionId);
            return instance;
        }
    }
}
