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
 * A GitHub repository one of a working tree's remotes points at.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitRemoteRepository(
    /** Account or organization owning the repository. */
    @JsonProperty("owner") String owner,
    /** Repository name, without the owner. */
    @JsonProperty("name") String name,
    /** GitHub host serving the repository, which is not `github.com` for a GitHub Enterprise remote. */
    @JsonProperty("host") String host,
    /** Name of the first remote that produced this distinct repository entry, such as `origin` or `upstream`. */
    @JsonProperty("remoteName") String remoteName
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    GitRemoteRepository {}

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
        private String owner;
        private String name;
        private String host;
        private String remoteName;

        /**
         * Sets the {@code owner} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder owner(String value) {
            this.owner = value;
            return this;
        }

        /**
         * Sets the {@code name} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder name(String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the {@code host} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder host(String value) {
            this.host = value;
            return this;
        }

        /**
         * Sets the {@code remoteName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder remoteName(String value) {
            this.remoteName = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public GitRemoteRepository build() {
            if (owner == null) throw new IllegalStateException("Missing required property: owner");
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (host == null) throw new IllegalStateException("Missing required property: host");
            if (remoteName == null) throw new IllegalStateException("Missing required property: remoteName");
            return new GitRemoteRepository(owner, name, host, remoteName);
        }
    }
}
