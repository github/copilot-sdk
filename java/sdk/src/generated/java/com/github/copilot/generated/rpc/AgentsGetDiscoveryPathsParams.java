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
 * Optional project paths to include when enumerating agent discovery directories.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentsGetDiscoveryPathsParams(
    /** Optional list of project directory paths. When omitted or empty, only the user-level directory is returned. */
    @JsonProperty("projectPaths") List<String> projectPaths,
    /** When true, omit the host's user-level agent directory, leaving only project directories. For multitenant deployments (mirrors `discover`'s `excludeHostAgents`). */
    @JsonProperty("excludeHostAgents") Boolean excludeHostAgents
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AgentsGetDiscoveryPathsParams {}

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
        private List<String> projectPaths;
        private Boolean excludeHostAgents;

        /**
         * Sets the {@code projectPaths} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder projectPaths(List<String> value) {
            this.projectPaths = value;
            return this;
        }

        /**
         * Sets the {@code excludeHostAgents} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder excludeHostAgents(Boolean value) {
            this.excludeHostAgents = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AgentsGetDiscoveryPathsParams build() {
            return new AgentsGetDiscoveryPathsParams(projectPaths, excludeHostAgents);
        }
    }
}
