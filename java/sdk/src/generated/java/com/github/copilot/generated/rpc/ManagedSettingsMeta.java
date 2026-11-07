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
 * Per-key lock state and provenance for `ManagedSettingsValues`, with the same field names. Producers emit each typed key in values and meta together; both outer objects are omitted when no typed key is set.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagedSettingsMeta(
    /** Lock state and provenance of `values.model`. */
    @JsonProperty("model") ManagedSettingMeta model,
    /** Lock state and provenance of `values.autoTier`. */
    @JsonProperty("autoTier") ManagedSettingMeta autoTier,
    /** Lock state and provenance of values.fusionTier, including a legacy alias's original source. An unmanaged value imposes no tier lock. */
    @JsonProperty("fusionTier") ManagedSettingMeta fusionTier,
    /** Lock state and provenance of `values.effortLevel`. */
    @JsonProperty("effortLevel") ManagedSettingMeta effortLevel,
    /** Lock state and provenance of `values.contextTier`. */
    @JsonProperty("contextTier") ManagedSettingMeta contextTier,
    /** Provenance of `values.communication`. Always reported as not overridable: users cannot replace the organization's information. */
    @JsonProperty("communication") ManagedSettingMeta communication
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ManagedSettingsMeta {}

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
        private ManagedSettingMeta model;
        private ManagedSettingMeta autoTier;
        private ManagedSettingMeta fusionTier;
        private ManagedSettingMeta effortLevel;
        private ManagedSettingMeta contextTier;
        private ManagedSettingMeta communication;

        /**
         * Sets the {@code model} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder model(ManagedSettingMeta value) {
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
        public Builder autoTier(ManagedSettingMeta value) {
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
        public Builder fusionTier(ManagedSettingMeta value) {
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
        public Builder effortLevel(ManagedSettingMeta value) {
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
        public Builder contextTier(ManagedSettingMeta value) {
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
        public Builder communication(ManagedSettingMeta value) {
            this.communication = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ManagedSettingsMeta build() {
            return new ManagedSettingsMeta(model, autoTier, fusionTier, effortLevel, contextTier, communication);
        }
    }

    /**
     * Retains the constructor published in Java SDK 1.0.28.
     * @deprecated Use named construction via {@link #builder()}.
     * @param model Lock state and provenance of `values.model`.
     * @param autoTier Lock state and provenance of `values.autoTier`.
     * @param fusionTier Lock state and provenance of values.fusionTier, including a legacy alias's original source. An unmanaged value imposes no tier lock.
     * @param effortLevel Lock state and provenance of `values.effortLevel`.
     * @param contextTier Lock state and provenance of `values.contextTier`.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ManagedSettingsMeta(
        ManagedSettingMeta model,
        ManagedSettingMeta autoTier,
        ManagedSettingMeta fusionTier,
        ManagedSettingMeta effortLevel,
        ManagedSettingMeta contextTier
    ) {
        this(model, autoTier, fusionTier, effortLevel, contextTier, null);
    }
}
