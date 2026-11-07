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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Variant {@code token} of {@link EntraTokenAcquireRequest}.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class EntraTokenAcquireRequestToken extends EntraTokenAcquireRequest {

    @JsonProperty("kind")
    private final String kind = "token";

    @Override
    public String getKind() { return kind; }

    /** Effective application client ID, including any resource-specific override. */
    @JsonProperty("clientId")
    private String clientId;

    /** Interaction policy. Renewal must remain silent. */
    @JsonProperty("reason")
    private EntraTokenAcquireReason reason;

    /** Pinned opaque host account ID. The response must identify this exact account. */
    @JsonProperty("accountId")
    private String accountId;

    /** Scopes required for this resource token. */
    @JsonProperty("scopes")
    private List<String> scopes;

    /** Target resource audience, when known. */
    @JsonProperty("audience")
    private String audience;

    /** Target Entra tenant, when different from the host's default. */
    @JsonProperty("tenant")
    private String tenant;

    /** Bypass cached access tokens without changing account selection or interaction policy. */
    @JsonProperty("skipCachedAccessToken")
    private Boolean skipCachedAccessToken;

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public EntraTokenAcquireReason getReason() { return reason; }
    public void setReason(EntraTokenAcquireReason reason) { this.reason = reason; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public List<String> getScopes() { return scopes; }
    public void setScopes(List<String> scopes) { this.scopes = scopes; }

    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }

    public String getTenant() { return tenant; }
    public void setTenant(String tenant) { this.tenant = tenant; }

    public Boolean getSkipCachedAccessToken() { return skipCachedAccessToken; }
    public void setSkipCachedAccessToken(Boolean skipCachedAccessToken) { this.skipCachedAccessToken = skipCachedAccessToken; }


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
        private String clientId;
        private EntraTokenAcquireReason reason;
        private String accountId;
        private List<String> scopes;
        private String audience;
        private String tenant;
        private Boolean skipCachedAccessToken;

        /**
         * Sets the {@code clientId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder clientId(String value) {
            this.clientId = value;
            return this;
        }

        /**
         * Sets the {@code reason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder reason(EntraTokenAcquireReason value) {
            this.reason = value;
            return this;
        }

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
         * Sets the {@code scopes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder scopes(List<String> value) {
            this.scopes = value;
            return this;
        }

        /**
         * Sets the {@code audience} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder audience(String value) {
            this.audience = value;
            return this;
        }

        /**
         * Sets the {@code tenant} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder tenant(String value) {
            this.tenant = value;
            return this;
        }

        /**
         * Sets the {@code skipCachedAccessToken} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder skipCachedAccessToken(Boolean value) {
            this.skipCachedAccessToken = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public EntraTokenAcquireRequestToken build() {
            if (clientId == null) throw new IllegalStateException("Missing required property: clientId");
            if (reason == null) throw new IllegalStateException("Missing required property: reason");
            if (accountId == null) throw new IllegalStateException("Missing required property: accountId");
            if (scopes == null) throw new IllegalStateException("Missing required property: scopes");
            if (skipCachedAccessToken == null) throw new IllegalStateException("Missing required property: skipCachedAccessToken");
            EntraTokenAcquireRequestToken instance = new EntraTokenAcquireRequestToken();
            instance.setClientId(clientId);
            instance.setReason(reason);
            instance.setAccountId(accountId);
            instance.setScopes(scopes);
            instance.setAudience(audience);
            instance.setTenant(tenant);
            instance.setSkipCachedAccessToken(skipCachedAccessToken);
            return instance;
        }
    }
}
