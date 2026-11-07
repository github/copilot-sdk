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
 * The effective managed settings the runtime would enforce for the given documents, in the same shape `managedSettings.resolve` returns.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagedSettingsComposeResult(
    /** Effective managed settings, in the same shape as `session.managedSettings.get`. */
    @JsonProperty("resolved") ManagedSettingsResolvedData resolved,
    /** Typed effective values, as in `managedSettings.resolve`. */
    @JsonProperty("values") ManagedSettingsValues values,
    /** Per-key lock state and provenance for `values`. */
    @JsonProperty("meta") ManagedSettingsMeta meta,
    /** Only the supplied channels, strongest first, with canonical documents. Empty canonical documents are represented as absent settings, as in live resolution. */
    @JsonProperty("layers") List<ManagedSettingsLayer> layers,
    /** Warnings about ignored content, with paths prefixed by the channel name. */
    @JsonProperty("diagnostics") List<ManagedSettingsDiagnostic> diagnostics
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ManagedSettingsComposeResult {}

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
        private ManagedSettingsResolvedData resolved;
        private ManagedSettingsValues values;
        private ManagedSettingsMeta meta;
        private List<ManagedSettingsLayer> layers;
        private List<ManagedSettingsDiagnostic> diagnostics;

        /**
         * Sets the {@code resolved} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder resolved(ManagedSettingsResolvedData value) {
            this.resolved = value;
            return this;
        }

        /**
         * Sets the {@code values} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder values(ManagedSettingsValues value) {
            this.values = value;
            return this;
        }

        /**
         * Sets the {@code meta} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder meta(ManagedSettingsMeta value) {
            this.meta = value;
            return this;
        }

        /**
         * Sets the {@code layers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder layers(List<ManagedSettingsLayer> value) {
            this.layers = value;
            return this;
        }

        /**
         * Sets the {@code diagnostics} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder diagnostics(List<ManagedSettingsDiagnostic> value) {
            this.diagnostics = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ManagedSettingsComposeResult build() {
            if (resolved == null) throw new IllegalStateException("Missing required property: resolved");
            if (layers == null) throw new IllegalStateException("Missing required property: layers");
            if (diagnostics == null) throw new IllegalStateException("Missing required property: diagnostics");
            return new ManagedSettingsComposeResult(resolved, values, meta, layers, diagnostics);
        }
    }
}
