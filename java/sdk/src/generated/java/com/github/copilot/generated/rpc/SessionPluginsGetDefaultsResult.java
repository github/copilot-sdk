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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Soft plugin defaults for this session, without persistent user preferences or hard policy.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionPluginsGetDefaultsResult(
    /** Opaque revision of the defaults, assignment reasons, and Team marketplaces. */
    @JsonProperty("generation") String generation,
    /** Plugin specs mapped to soft enablement defaults. Managed defaultEnabled assignments override Team defaults for the same spec. */
    @JsonProperty("defaultEnabledPlugins") Map<String, Boolean> defaultEnabledPlugins,
    /** Team-only defaults for hosts that retain activation inputs. Managed assignments are read live by the runtime resolver and must not be retained as Team defaults. */
    @JsonProperty("teamDefaultEnabledPlugins") Map<String, Boolean> teamDefaultEnabledPlugins,
    /** Team marketplace definitions available only as best-effort installation fallbacks. */
    @JsonProperty("extraKnownMarketplaces") Map<String, TeamPluginMarketplace> extraKnownMarketplaces
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionPluginsGetDefaultsResult {}

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
        private String generation;
        private Map<String, Boolean> defaultEnabledPlugins;
        private Map<String, Boolean> teamDefaultEnabledPlugins;
        private Map<String, TeamPluginMarketplace> extraKnownMarketplaces;

        /**
         * Sets the {@code generation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder generation(String value) {
            this.generation = value;
            return this;
        }

        /**
         * Sets the {@code defaultEnabledPlugins} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder defaultEnabledPlugins(Map<String, Boolean> value) {
            this.defaultEnabledPlugins = value;
            return this;
        }

        /**
         * Sets the {@code teamDefaultEnabledPlugins} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder teamDefaultEnabledPlugins(Map<String, Boolean> value) {
            this.teamDefaultEnabledPlugins = value;
            return this;
        }

        /**
         * Sets the {@code extraKnownMarketplaces} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder extraKnownMarketplaces(Map<String, TeamPluginMarketplace> value) {
            this.extraKnownMarketplaces = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionPluginsGetDefaultsResult build() {
            if (generation == null) throw new IllegalStateException("Missing required property: generation");
            if (defaultEnabledPlugins == null) throw new IllegalStateException("Missing required property: defaultEnabledPlugins");
            if (teamDefaultEnabledPlugins == null) throw new IllegalStateException("Missing required property: teamDefaultEnabledPlugins");
            if (extraKnownMarketplaces == null) throw new IllegalStateException("Missing required property: extraKnownMarketplaces");
            return new SessionPluginsGetDefaultsResult(generation, defaultEnabledPlugins, teamDefaultEnabledPlugins, extraKnownMarketplaces);
        }
    }
}
