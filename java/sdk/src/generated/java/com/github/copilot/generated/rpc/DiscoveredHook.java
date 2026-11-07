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
 * One server-discovered hook action from user, repository, plugin, or managed-policy configuration.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DiscoveredHook(
    /** Deterministic identifier for this server-discovered action row. It remains stable while the project, origin, source, event, action content, and duplicate ordinal are unchanged. This is row identity, not the key persisted in disabledHooks. */
    @JsonProperty("id") String id,
    /** Hook event that invokes this action. */
    @JsonProperty("hookType") HookType hookType,
    /** Configuration tier that contributed this hook action. */
    @JsonProperty("origin") HookOrigin origin,
    /** Human-readable source label, such as a hook file path, settings source, or plugin name. */
    @JsonProperty("source") String source,
    /** Input project path for which this server-side action was resolved. Set on every row returned for project-scoped discovery, including repeated user and policy actions. */
    @JsonProperty("projectPath") String projectPath,
    /** Whether this action is enabled under the server-side discovery settings. Concrete sessions may differ because they can add session-specific directories, plugins, or trust. False when its disable key is present in the user's disabled-hooks setting or disable-all settings suppress the action. */
    @JsonProperty("enabled") Boolean enabled,
    /** Durable content hash used by hook enablement. Identical actions may intentionally share this key. Omitted when changing the user's disabled-hooks setting cannot change the action's current server-discovered state, including managed-policy hooks, session-start prompt actions, actions suppressed by disable-all settings, and projectless plugin actions that require project-directory expansion. */
    @JsonProperty("disableKey") String disableKey,
    /** File URI of the document defining this action on the selected runtime host, not the command or script it executes. Inline hooks point to their containing settings document or plugin manifest. Omitted for sources without a backing file, such as registry policy. */
    @JsonProperty("definitionFileUri") String definitionFileUri,
    /** Opaque identity of the plugin owning this action. Matches plugins.list().plugins[].id and MCP effectiveSource.plugin.id on the same runtime host. Compare the complete value without parsing it. Omitted for non-plugin hooks. */
    @JsonProperty("pluginId") String pluginId
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public DiscoveredHook {}

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
        private String id;
        private HookType hookType;
        private HookOrigin origin;
        private String source;
        private String projectPath;
        private Boolean enabled;
        private String disableKey;
        private String definitionFileUri;
        private String pluginId;

        /**
         * Sets the {@code id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder id(String value) {
            this.id = value;
            return this;
        }

        /**
         * Sets the {@code hookType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder hookType(HookType value) {
            this.hookType = value;
            return this;
        }

        /**
         * Sets the {@code origin} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder origin(HookOrigin value) {
            this.origin = value;
            return this;
        }

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder source(String value) {
            this.source = value;
            return this;
        }

        /**
         * Sets the {@code projectPath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder projectPath(String value) {
            this.projectPath = value;
            return this;
        }

        /**
         * Sets the {@code enabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder enabled(Boolean value) {
            this.enabled = value;
            return this;
        }

        /**
         * Sets the {@code disableKey} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder disableKey(String value) {
            this.disableKey = value;
            return this;
        }

        /**
         * Sets the {@code definitionFileUri} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder definitionFileUri(String value) {
            this.definitionFileUri = value;
            return this;
        }

        /**
         * Sets the {@code pluginId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder pluginId(String value) {
            this.pluginId = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public DiscoveredHook build() {
            if (id == null) throw new IllegalStateException("Missing required property: id");
            if (hookType == null) throw new IllegalStateException("Missing required property: hookType");
            if (origin == null) throw new IllegalStateException("Missing required property: origin");
            if (enabled == null) throw new IllegalStateException("Missing required property: enabled");
            return new DiscoveredHook(id, hookType, origin, source, projectPath, enabled, disableKey, definitionFileUri, pluginId);
        }
    }

    /**
     * Creates a record without plugin ownership or content-root metadata.
     * @deprecated Use named construction via {@link #builder()}.
     * @param id Deterministic identifier for this server-discovered action row. It remains stable while the project, origin, source, event, action content, and duplicate ordinal are unchanged. This is row identity, not the key persisted in disabledHooks.
     * @param hookType Hook event that invokes this action.
     * @param origin Configuration tier that contributed this hook action.
     * @param source Human-readable source label, such as a hook file path, settings source, or plugin name.
     * @param projectPath Input project path for which this server-side action was resolved. Set on every row returned for project-scoped discovery, including repeated user and policy actions.
     * @param enabled Whether this action is enabled under the server-side discovery settings. Concrete sessions may differ because they can add session-specific directories, plugins, or trust. False when its disable key is present in the user's disabled-hooks setting or disable-all settings suppress the action.
     * @param disableKey Durable content hash used by hook enablement. Identical actions may intentionally share this key. Omitted when changing the user's disabled-hooks setting cannot change the action's current server-discovered state, including managed-policy hooks, session-start prompt actions, actions suppressed by disable-all settings, and projectless plugin actions that require project-directory expansion.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public DiscoveredHook(
        String id,
        HookType hookType,
        HookOrigin origin,
        String source,
        String projectPath,
        Boolean enabled,
        String disableKey
    ) {
        this(id, hookType, origin, source, projectPath, enabled, disableKey, null, null);
    }
}
