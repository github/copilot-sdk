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
import com.github.copilot.generated.ManagedPermissionsContext;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Effective enterprise managed settings for an account, resolved without a session.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagedSettingsResolveResult(
    /** Reusable source-composed permission policy for managedSettings.permissions.evaluate. Unlike resolved.settings.permissions, this retains every source's allowlist and default-prompt semantics. Refresh through resolve when account/device policy changes. Absent on runtimes that do not support permission evaluation. */
    @JsonProperty("permissionsContext") ManagedPermissionsContext permissionsContext,
    /** Printable opaque identity of the account the settings were resolved for, suitable for comparison and storage, not an account selectionId. Absent when no account was available, in which case only device policy is reported. */
    @JsonProperty("account") String account,
    /** Effective managed settings from the device and account (server) channels, in the same shape as `session.managedSettings.get`, excluding session-local injection. */
    @JsonProperty("resolved") ManagedSettingsResolvedData resolved,
    /** Typed effective values of managed settings, keyed like the managed-settings schema and already resolved across channels, with the `{ "overridable": ... }` wrapper removed. Present when policy sets at least one typed key. Keys not typed here are available in `resolved.settings`. */
    @JsonProperty("values") ManagedSettingsValues values,
    /** Per-key lock state and provenance for the entries in `values`, using the same key names. */
    @JsonProperty("meta") ManagedSettingsMeta meta,
    /** Each managed-settings channel consulted, strongest first, with the validated document it delivered before merging. `resolved.settings` is the merged result. More channels may be added over time. */
    @JsonProperty("layers") List<ManagedSettingsLayer> layers,
    /** Warnings about unavailable policy sources or a failed refresh served from cache. A cached response is not proof of a successful live fetch; `resolved.failClosed` separately describes enforcement. */
    @JsonProperty("diagnostics") List<ManagedSettingsDiagnostic> diagnostics
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ManagedSettingsResolveResult {}

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
        private ManagedPermissionsContext permissionsContext;
        private String account;
        private ManagedSettingsResolvedData resolved;
        private ManagedSettingsValues values;
        private ManagedSettingsMeta meta;
        private List<ManagedSettingsLayer> layers;
        private List<ManagedSettingsDiagnostic> diagnostics;

        /**
         * Sets the {@code permissionsContext} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder permissionsContext(ManagedPermissionsContext value) {
            this.permissionsContext = value;
            return this;
        }

        /**
         * Sets the {@code account} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder account(String value) {
            this.account = value;
            return this;
        }

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
        public ManagedSettingsResolveResult build() {
            if (resolved == null) throw new IllegalStateException("Missing required property: resolved");
            if (layers == null) throw new IllegalStateException("Missing required property: layers");
            if (diagnostics == null) throw new IllegalStateException("Missing required property: diagnostics");
            return new ManagedSettingsResolveResult(permissionsContext, account, resolved, values, meta, layers, diagnostics);
        }
    }
}
