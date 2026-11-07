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
 * Typed effective values of managed settings. Each field mirrors the managed-settings schema key of the same name; more keys are added as they are typed.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagedSettingsValues(
    /** Managed default model identifier. When model availability was resolved, aliases and family names are projected to a concrete available model ID; otherwise the configured value is returned. */
    @JsonProperty("model") String model,
    /** Managed Auto routing preference, used when the selected model is `auto`. */
    @JsonProperty("autoTier") AutoTier autoTier,
    /** Managed Fusion preference. Takes precedence over autoTier even when unmanaged, which means no managed tier and forbids legacy fallback. Legacy Auto values are mapped only when no managed source supplies fusionTier. */
    @JsonProperty("fusionTier") ManagedFusionTier fusionTier,
    /** Managed reasoning-effort default for the managed concrete model. The runtime clamps it to an entitled effort when model availability is known. */
    @JsonProperty("effortLevel") String effortLevel,
    /** Managed context-tier default for the managed concrete model. */
    @JsonProperty("contextTier") ContextTier contextTier,
    /** Organization-specific information for clients to surface to end users. Present when policy sets `communication.support.url`. */
    @JsonProperty("communication") ManagedCommunicationSettings communication
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ManagedSettingsValues {}

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
        private String model;
        private AutoTier autoTier;
        private ManagedFusionTier fusionTier;
        private String effortLevel;
        private ContextTier contextTier;
        private ManagedCommunicationSettings communication;

        /**
         * Sets the {@code model} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder model(String value) {
            this.model = value;
            return this;
        }

        /**
         * Sets the {@code autoTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder autoTier(AutoTier value) {
            this.autoTier = value;
            return this;
        }

        /**
         * Sets the {@code fusionTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder fusionTier(ManagedFusionTier value) {
            this.fusionTier = value;
            return this;
        }

        /**
         * Sets the {@code effortLevel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
        public Builder contextTier(ContextTier value) {
            this.contextTier = value;
            return this;
        }

        /**
         * Sets the {@code communication} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder communication(ManagedCommunicationSettings value) {
            this.communication = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ManagedSettingsValues build() {
            return new ManagedSettingsValues(model, autoTier, fusionTier, effortLevel, contextTier, communication);
        }
    }

    /**
     * Retains the constructor published in Java SDK 1.0.28.
     * @deprecated Use named construction via {@link #builder()}.
     * @param model Managed default model identifier. When model availability was resolved, aliases and family names are projected to a concrete available model ID; otherwise the configured value is returned.
     * @param autoTier Managed Auto routing preference, used when the selected model is `auto`.
     * @param fusionTier Managed Fusion preference. Takes precedence over autoTier even when unmanaged, which means no managed tier and forbids legacy fallback. Legacy Auto values are mapped only when no managed source supplies fusionTier.
     * @param effortLevel Managed reasoning-effort default for the managed concrete model. The runtime clamps it to an entitled effort when model availability is known.
     * @param contextTier Managed context-tier default for the managed concrete model.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ManagedSettingsValues(
        String model,
        AutoTier autoTier,
        ManagedFusionTier fusionTier,
        String effortLevel,
        ContextTier contextTier
    ) {
        this(model, autoTier, fusionTier, effortLevel, contextTier, null);
    }
}
