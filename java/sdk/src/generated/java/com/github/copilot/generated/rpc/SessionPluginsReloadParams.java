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
 * Request parameters for the {@code session.plugins.reload} RPC method.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionPluginsReloadParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Reload MCP server connections after refreshing plugins. Defaults to true. */
    @JsonProperty("reloadMcp") Boolean reloadMcp,
    /** Re-run custom-agent discovery after refreshing plugins. Defaults to true. */
    @JsonProperty("reloadCustomAgents") Boolean reloadCustomAgents,
    /** Re-load user, plugin, and (subject to `deferRepoHooks`) repo hooks. Defaults to true. Has no effect when the host has not registered a hook reloader (e.g. remote sessions). */
    @JsonProperty("reloadHooks") Boolean reloadHooks,
    /** Re-discover and relaunch subprocess extensions (including plugin-shipped extensions) after refreshing plugins. Defaults to true. Has no effect when the session has no active extension controller (e.g. extensions were not requested for the session). */
    @JsonProperty("reloadExtensions") Boolean reloadExtensions,
    /** When true, skip repo-level hooks during the hook reload. Use before folder trust is confirmed; load them post-trust via `sessions.loadDeferredRepoHooks`. */
    @JsonProperty("deferRepoHooks") Boolean deferRepoHooks
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionPluginsReloadParams {}

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
        private String sessionId;
        private Boolean reloadMcp;
        private Boolean reloadCustomAgents;
        private Boolean reloadHooks;
        private Boolean reloadExtensions;
        private Boolean deferRepoHooks;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code reloadMcp} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reloadMcp(Boolean value) {
            this.reloadMcp = value;
            return this;
        }

        /**
         * Sets the {@code reloadCustomAgents} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reloadCustomAgents(Boolean value) {
            this.reloadCustomAgents = value;
            return this;
        }

        /**
         * Sets the {@code reloadHooks} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reloadHooks(Boolean value) {
            this.reloadHooks = value;
            return this;
        }

        /**
         * Sets the {@code reloadExtensions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reloadExtensions(Boolean value) {
            this.reloadExtensions = value;
            return this;
        }

        /**
         * Sets the {@code deferRepoHooks} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder deferRepoHooks(Boolean value) {
            this.deferRepoHooks = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionPluginsReloadParams build() {
            return new SessionPluginsReloadParams(sessionId, reloadMcp, reloadCustomAgents, reloadHooks, reloadExtensions, deferRepoHooks);
        }
    }
}
