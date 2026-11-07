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
 * Canonical directory where custom agents can be discovered or created, with scope, preference, and optional project path.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentDiscoveryPath(
    /** Absolute path of the search/create directory (may not exist on disk yet) */
    @JsonProperty("path") String path,
    /** Which tier this directory belongs to */
    @JsonProperty("scope") AgentDiscoveryPathScope scope,
    /** Whether this is the canonical directory to create a new agent in its tier. At most one entry per tier is preferred. */
    @JsonProperty("preferredForCreation") Boolean preferredForCreation,
    /** The input project path this directory was derived from (only for project scope) */
    @JsonProperty("projectPath") String projectPath
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AgentDiscoveryPath {}

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
        private String path;
        private AgentDiscoveryPathScope scope;
        private Boolean preferredForCreation;
        private String projectPath;

        /**
         * Sets the {@code path} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder path(String value) {
            this.path = value;
            return this;
        }

        /**
         * Sets the {@code scope} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder scope(AgentDiscoveryPathScope value) {
            this.scope = value;
            return this;
        }

        /**
         * Sets the {@code preferredForCreation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder preferredForCreation(Boolean value) {
            this.preferredForCreation = value;
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
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AgentDiscoveryPath build() {
            if (path == null) throw new IllegalStateException("Missing required property: path");
            if (scope == null) throw new IllegalStateException("Missing required property: scope");
            if (preferredForCreation == null) throw new IllegalStateException("Missing required property: preferredForCreation");
            return new AgentDiscoveryPath(path, scope, preferredForCreation, projectPath);
        }
    }
}
