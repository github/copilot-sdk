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
 * Variant {@code loginAccounts} of {@link AuthEnumerateQuery}.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AuthEnumerateQueryLoginAccounts extends AuthEnumerateQuery {

    @JsonProperty("kind")
    private final String kind = "loginAccounts";

    @Override
    public String getKind() { return kind; }

    /** The provider whose cached base identities should be listed. */
    @JsonProperty("provider")
    private LoginProviderKind provider;

    public LoginProviderKind getProvider() { return provider; }
    public void setProvider(LoginProviderKind provider) { this.provider = provider; }


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
        private LoginProviderKind provider;

        /**
         * Sets the {@code provider} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder provider(LoginProviderKind value) {
            this.provider = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AuthEnumerateQueryLoginAccounts build() {
            if (provider == null) throw new IllegalStateException("Missing required property: provider");
            AuthEnumerateQueryLoginAccounts instance = new AuthEnumerateQueryLoginAccounts();
            instance.setProvider(provider);
            return instance;
        }
    }
}
