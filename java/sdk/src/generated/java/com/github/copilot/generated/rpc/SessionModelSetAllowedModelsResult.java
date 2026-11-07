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
 * The applied host allowlist and effective session model policy after intersection.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionModelSetAllowedModelsResult(
    /** Normalized host allowlist. Omitted when the host restriction was cleared, or when a relay client does not return the host policy. */
    @JsonProperty("allowedModels") List<String> allowedModels,
    /** Effective exact IDs or repository policy patterns after applying the host restriction. Omitted by relay clients that do not return the host policy. */
    @JsonProperty("effectiveAllowedModels") List<String> effectiveAllowedModels,
    /** Effective deterministic fallback model, when the policy defines one. */
    @JsonProperty("fallbackModel") String fallbackModel,
    /** Selected session model after reconciling a now-disallowed concrete selection. */
    @JsonProperty("modelId") String modelId
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionModelSetAllowedModelsResult {}

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
        private List<String> allowedModels;
        private List<String> effectiveAllowedModels;
        private String fallbackModel;
        private String modelId;

        /**
         * Sets the {@code allowedModels} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder allowedModels(List<String> value) {
            this.allowedModels = value;
            return this;
        }

        /**
         * Sets the {@code effectiveAllowedModels} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder effectiveAllowedModels(List<String> value) {
            this.effectiveAllowedModels = value;
            return this;
        }

        /**
         * Sets the {@code fallbackModel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder fallbackModel(String value) {
            this.fallbackModel = value;
            return this;
        }

        /**
         * Sets the {@code modelId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder modelId(String value) {
            this.modelId = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionModelSetAllowedModelsResult build() {
            return new SessionModelSetAllowedModelsResult(allowedModels, effectiveAllowedModels, fallbackModel, modelId);
        }
    }
}
