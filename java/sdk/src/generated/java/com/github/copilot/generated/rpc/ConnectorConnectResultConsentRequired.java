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
 * Host-owned consent is required before bounded continuation can complete.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ConnectorConnectResultConsentRequired extends ConnectorConnectResult {

    @JsonProperty("kind")
    private final String kind = "consent_required";

    @Override
    public String getKind() { return kind; }

    /** Validated HTTPS consent URL. The runtime does not open it. */
    @JsonProperty("consentUrl")
    private String consentUrl;

    /** Opaque ID accepted by continueConnection. */
    @JsonProperty("continuationId")
    private String continuationId;

    public String getConsentUrl() { return consentUrl; }
    public void setConsentUrl(String consentUrl) { this.consentUrl = consentUrl; }

    public String getContinuationId() { return continuationId; }
    public void setContinuationId(String continuationId) { this.continuationId = continuationId; }


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
        private String consentUrl;
        private String continuationId;

        /**
         * Sets the {@code consentUrl} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder consentUrl(String value) {
            this.consentUrl = value;
            return this;
        }

        /**
         * Sets the {@code continuationId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder continuationId(String value) {
            this.continuationId = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ConnectorConnectResultConsentRequired build() {
            if (consentUrl == null) throw new IllegalStateException("Missing required property: consentUrl");
            if (continuationId == null) throw new IllegalStateException("Missing required property: continuationId");
            ConnectorConnectResultConsentRequired instance = new ConnectorConnectResultConsentRequired();
            instance.setConsentUrl(consentUrl);
            instance.setContinuationId(continuationId);
            return instance;
        }
    }
}
