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
 * Saves through the exact authenticated local attachment that discovered a model.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record ProvidersSourceAddModelParams(
    /** Session whose owning server receives the save. */
    @JsonProperty("sessionId") String sessionId,
    /** Original authenticated local attachment, not a replacement with the same session id. */
    @JsonProperty("attachId") Long attachId,
    /** The ordinary server-owned save request, without a caller-selected destination. */
    @JsonProperty("configuration") SavedProviderConfigurationAddModelRequest configuration
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    ProvidersSourceAddModelParams {}

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
        private String sessionId;
        private Long attachId;
        private SavedProviderConfigurationAddModelRequest configuration;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code attachId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder attachId(Long value) {
            this.attachId = value;
            return this;
        }

        /**
         * Sets the {@code configuration} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder configuration(SavedProviderConfigurationAddModelRequest value) {
            this.configuration = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ProvidersSourceAddModelParams build() {
            if (sessionId == null) throw new IllegalStateException("Missing required property: sessionId");
            if (attachId == null) throw new IllegalStateException("Missing required property: attachId");
            if (configuration == null) throw new IllegalStateException("Missing required property: configuration");
            return new ProvidersSourceAddModelParams(sessionId, attachId, configuration);
        }
    }
}
