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
 * Variant {@code logout} of {@link AuthWrite}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AuthWriteLogout extends AuthWrite {

    @JsonProperty("kind")
    private final String kind = "logout";

    @Override
    public String getKind() { return kind; }

    /** Stable account id of the account to log out. Use this for canonical account entries that are not switchable. */
    @JsonProperty("accountId")
    private String accountId;

    /** Opaque selection id of the account to log out; absent logs out the active account. */
    @JsonProperty("selectionId")
    private String selectionId;

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getSelectionId() { return selectionId; }
    public void setSelectionId(String selectionId) { this.selectionId = selectionId; }


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
        private String accountId;
        private String selectionId;

        /**
         * Sets the {@code accountId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder accountId(String value) {
            this.accountId = value;
            return this;
        }

        /**
         * Sets the {@code selectionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder selectionId(String value) {
            this.selectionId = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AuthWriteLogout build() {
            AuthWriteLogout instance = new AuthWriteLogout();
            instance.setAccountId(accountId);
            instance.setSelectionId(selectionId);
            return instance;
        }
    }
}
