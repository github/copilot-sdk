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
 * Model identifier and token limits used to compute the context-info breakdown.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMetadataContextInfoParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Advertised prompt allowance. Pass 0 to resolve the selected model and context tier from the session. */
    @JsonProperty("promptTokenLimit") Long promptTokenLimit,
    /** Requested output allowance to reserve against the combined context ceiling. Pass 0 to resolve the session's request cap, falling back to the model's advertised output limit. */
    @JsonProperty("outputTokenLimit") Long outputTokenLimit,
    /** Model identifier used for tokenization. Omit to use the session default. Used both for token counting and to compute display values. */
    @JsonProperty("selectedModel") String selectedModel,
    /** Provider owning selectedModel. Omit to use the current provider for the current model, or deterministic legacy lookup for a different model. */
    @JsonProperty("providerId") String providerId
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionMetadataContextInfoParams {}

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
        private Long promptTokenLimit;
        private Long outputTokenLimit;
        private String selectedModel;
        private String providerId;

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
         * Sets the {@code promptTokenLimit} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder promptTokenLimit(Long value) {
            this.promptTokenLimit = value;
            return this;
        }

        /**
         * Sets the {@code outputTokenLimit} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder outputTokenLimit(Long value) {
            this.outputTokenLimit = value;
            return this;
        }

        /**
         * Sets the {@code selectedModel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder selectedModel(String value) {
            this.selectedModel = value;
            return this;
        }

        /**
         * Sets the {@code providerId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder providerId(String value) {
            this.providerId = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionMetadataContextInfoParams build() {
            if (promptTokenLimit == null) throw new IllegalStateException("Missing required property: promptTokenLimit");
            if (outputTokenLimit == null) throw new IllegalStateException("Missing required property: outputTokenLimit");
            return new SessionMetadataContextInfoParams(sessionId, promptTokenLimit, outputTokenLimit, selectedModel, providerId);
        }
    }
}
