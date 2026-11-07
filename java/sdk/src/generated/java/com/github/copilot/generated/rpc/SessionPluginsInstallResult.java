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
 * Result of installing a plugin.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionPluginsInstallResult(
    /** The newly installed plugin's metadata */
    @JsonProperty("plugin") InstalledPluginInfo plugin,
    /** Number of skills discovered and installed from the plugin */
    @JsonProperty("skillsInstalled") Long skillsInstalled,
    /** Where the completed plugin tree was staged before atomic promotion */
    @JsonProperty("stagingMode") PluginInstallStagingMode stagingMode,
    /** Optional post-install message provided by the plugin (e.g. setup instructions) */
    @JsonProperty("postInstallMessage") String postInstallMessage,
    /** Set when the install path is deprecated (e.g. direct repo / URL / local installs). Callers should surface this to end users. */
    @JsonProperty("deprecationWarning") String deprecationWarning,
    /** Report-and-ignore warnings raised while reading the plugin's manifest, such as a field the manifest's schema does not define and the loader therefore ignored. Callers should surface these to end users: a plugin whose skills sit behind an ignored field otherwise installs, lists, and contributes nothing, with no signal to its author. Absent when the manifest raised none. */
    @JsonProperty("warnings") List<String> warnings
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionPluginsInstallResult {}

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
        private InstalledPluginInfo plugin;
        private Long skillsInstalled;
        private PluginInstallStagingMode stagingMode;
        private String postInstallMessage;
        private String deprecationWarning;
        private List<String> warnings;

        /**
         * Sets the {@code plugin} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder plugin(InstalledPluginInfo value) {
            this.plugin = value;
            return this;
        }

        /**
         * Sets the {@code skillsInstalled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder skillsInstalled(Long value) {
            this.skillsInstalled = value;
            return this;
        }

        /**
         * Sets the {@code stagingMode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder stagingMode(PluginInstallStagingMode value) {
            this.stagingMode = value;
            return this;
        }

        /**
         * Sets the {@code postInstallMessage} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder postInstallMessage(String value) {
            this.postInstallMessage = value;
            return this;
        }

        /**
         * Sets the {@code deprecationWarning} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder deprecationWarning(String value) {
            this.deprecationWarning = value;
            return this;
        }

        /**
         * Sets the {@code warnings} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder warnings(List<String> value) {
            this.warnings = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionPluginsInstallResult build() {
            if (plugin == null) throw new IllegalStateException("Missing required property: plugin");
            if (skillsInstalled == null) throw new IllegalStateException("Missing required property: skillsInstalled");
            return new SessionPluginsInstallResult(plugin, skillsInstalled, stagingMode, postInstallMessage, deprecationWarning, warnings);
        }
    }

    /**
     * Retains the constructor published in Java SDK 1.0.28.
     * @deprecated Use named construction via {@link #builder()}.
     * @param plugin The newly installed plugin's metadata
     * @param skillsInstalled Number of skills discovered and installed from the plugin
     * @param stagingMode Where the completed plugin tree was staged before atomic promotion
     * @param postInstallMessage Optional post-install message provided by the plugin (e.g. setup instructions)
     * @param deprecationWarning Set when the install path is deprecated (e.g. direct repo / URL / local installs). Callers should surface this to end users.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionPluginsInstallResult(
        InstalledPluginInfo plugin,
        Long skillsInstalled,
        PluginInstallStagingMode stagingMode,
        String postInstallMessage,
        String deprecationWarning
    ) {
        this(plugin, skillsInstalled, stagingMode, postInstallMessage, deprecationWarning, null);
    }
}
