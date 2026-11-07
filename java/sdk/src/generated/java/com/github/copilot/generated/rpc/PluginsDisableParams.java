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
 * Plugin names (or specs) to disable, plus the optional working directory the repository-controlled guard is evaluated against.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluginsDisableParams(
    /** Plugin names, "plugin@marketplace" specs, or opaque directSourceId values from inventory to disable. Use directSourceId to distinguish same-named direct installations. Unknown targets and session-only plugin-directory mounts are ignored. Direct-install decisions persist on the installation record. Plugin-owned MCP servers are stopped in active sessions immediately; other plugin contributions remain available until each session reloads plugins. */
    @JsonProperty("names") List<String> names,
    /** Working directory whose repository `enabledPlugins` overlay decides whether this mutation is repository-controlled. Hosts that serve sessions across several repositories (the SDK server) should pass the session's directory; otherwise the guard is evaluated against the server process's own working directory, which may belong to a different repository. Defaults to the server's current working directory. */
    @JsonProperty("workingDirectory") String workingDirectory
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public PluginsDisableParams {}

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
        private List<String> names;
        private String workingDirectory;

        /**
         * Sets the {@code names} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder names(List<String> value) {
            this.names = value;
            return this;
        }

        /**
         * Sets the {@code workingDirectory} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder workingDirectory(String value) {
            this.workingDirectory = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public PluginsDisableParams build() {
            if (names == null) throw new IllegalStateException("Missing required property: names");
            return new PluginsDisableParams(names, workingDirectory);
        }
    }
}
