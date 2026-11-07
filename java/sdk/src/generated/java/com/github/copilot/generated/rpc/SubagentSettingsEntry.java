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
 * Subagent model, reasoning effort, context tier, and auto-invocation settings
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubagentSettingsEntry(
    /** Model override for matching subagents */
    @JsonProperty("model") String model,
    /** Provider identifier for the configured model override */
    @JsonProperty("providerId") String providerId,
    /** Whether the configured model strategy is preferred or required */
    @JsonProperty("modelPolicy") AgentModelPolicy modelPolicy,
    /** Reasoning effort override for matching subagents */
    @JsonProperty("effortLevel") String effortLevel,
    /** Context tier override for matching subagents */
    @JsonProperty("contextTier") SubagentSettingsEntryContextTier contextTier,
    /** Whether this agent's runtime-defined proactive invocation prompting is enabled, if supported. Currently consumed by the built-in rubber-duck agent. */
    @JsonProperty("autoInvoke") Boolean autoInvoke
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SubagentSettingsEntry {}

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
        private String model;
        private String providerId;
        private AgentModelPolicy modelPolicy;
        private String effortLevel;
        private SubagentSettingsEntryContextTier contextTier;
        private Boolean autoInvoke;

        /**
         * Sets the {@code model} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder model(String value) {
            this.model = value;
            return this;
        }

        /**
         * Sets the {@code providerId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder providerId(String value) {
            this.providerId = value;
            return this;
        }

        /**
         * Sets the {@code modelPolicy} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder modelPolicy(AgentModelPolicy value) {
            this.modelPolicy = value;
            return this;
        }

        /**
         * Sets the {@code effortLevel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder effortLevel(String value) {
            this.effortLevel = value;
            return this;
        }

        /**
         * Sets the {@code contextTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder contextTier(SubagentSettingsEntryContextTier value) {
            this.contextTier = value;
            return this;
        }

        /**
         * Sets the {@code autoInvoke} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder autoInvoke(Boolean value) {
            this.autoInvoke = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SubagentSettingsEntry build() {
            return new SubagentSettingsEntry(model, providerId, modelPolicy, effortLevel, contextTier, autoInvoke);
        }
    }
}
