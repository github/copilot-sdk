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
 * Normalised identity of the MCP server a plan targets, independent of how the card spelled it.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpPlanResourceIdentity(
    /** Canonical, normalised name of the server, for example `io.github.owner/server`. */
    @JsonProperty("canonicalName") String canonicalName,
    /** Local configuration key the server would be recorded under. */
    @JsonProperty("serverName") String serverName,
    /** Version advertised by the card, when it declares one. */
    @JsonProperty("version") String version,
    /** Registry identifier of the server, when it came from a registry. */
    @JsonProperty("registryId") String registryId
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpPlanResourceIdentity {}

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
        private String canonicalName;
        private String serverName;
        private String version;
        private String registryId;

        /**
         * Sets the {@code canonicalName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder canonicalName(String value) {
            this.canonicalName = value;
            return this;
        }

        /**
         * Sets the {@code serverName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder serverName(String value) {
            this.serverName = value;
            return this;
        }

        /**
         * Sets the {@code version} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder version(String value) {
            this.version = value;
            return this;
        }

        /**
         * Sets the {@code registryId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder registryId(String value) {
            this.registryId = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpPlanResourceIdentity build() {
            if (canonicalName == null) throw new IllegalStateException("Missing required property: canonicalName");
            if (serverName == null) throw new IllegalStateException("Missing required property: serverName");
            return new McpPlanResourceIdentity(canonicalName, serverName, version, registryId);
        }
    }
}
