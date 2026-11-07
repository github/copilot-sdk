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
 * Optional capability overrides (vision, tool_calls, reasoning, etc.).
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelCapabilitiesOverride(
    /** Feature flags indicating what the model supports */
    @JsonProperty("supports") ModelCapabilitiesOverrideSupports supports,
    /** Token limits for prompts, outputs, and context window */
    @JsonProperty("limits") ModelCapabilitiesOverrideLimits limits
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ModelCapabilitiesOverride {}

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
        private ModelCapabilitiesOverrideSupports supports;
        private ModelCapabilitiesOverrideLimits limits;

        /**
         * Sets the {@code supports} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder supports(ModelCapabilitiesOverrideSupports value) {
            this.supports = value;
            return this;
        }

        /**
         * Sets the {@code limits} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder limits(ModelCapabilitiesOverrideLimits value) {
            this.limits = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ModelCapabilitiesOverride build() {
            return new ModelCapabilitiesOverride(supports, limits);
        }
    }
}
