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
 * Variant {@code identity} of {@link EntraTokenAcquireRequest}.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class EntraTokenAcquireRequestIdentity extends EntraTokenAcquireRequest {

    @JsonProperty("kind")
    private final String kind = "identity";

    @Override
    public String getKind() { return kind; }

    /** Effective application client ID. Treat it as opaque. */
    @JsonProperty("clientId")
    private String clientId;

    /** Interaction policy. Only a fresh explicit sign-in may select an unpinned account. */
    @JsonProperty("reason")
    private EntraTokenAcquireReason reason;

    /** Host account to revalidate. When supplied, the response must identify this exact account. */
    @JsonProperty("accountId")
    private String accountId;

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public EntraTokenAcquireReason getReason() { return reason; }
    public void setReason(EntraTokenAcquireReason reason) { this.reason = reason; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }


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
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public EntraTokenAcquireRequestIdentity build() {
            if (clientId == null) throw new IllegalStateException("Missing required property: clientId");
            if (reason == null) throw new IllegalStateException("Missing required property: reason");
            EntraTokenAcquireRequestIdentity instance = new EntraTokenAcquireRequestIdentity();
            instance.setClientId(clientId);
            instance.setReason(reason);
            instance.setAccountId(accountId);
            return instance;
        }
    }
}
