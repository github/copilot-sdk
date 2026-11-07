/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * The GitHub repositories a working tree's remotes point at.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitReposFromRemotesResult(
    /** One entry per distinct GitHub repository, in the order git reports the first remote for each repository. Empty when no remote points at a GitHub host, which a caller should read as `not connected to GitHub`. Failing to read the remotes is an error, not an empty list. */
    @JsonProperty("repositories") List<GitRemoteRepository> repositories
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    GitReposFromRemotesResult {}

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
        private List<GitRemoteRepository> repositories;

        /**
         * Sets the {@code repositories} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder repositories(List<GitRemoteRepository> value) {
            this.repositories = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public GitReposFromRemotesResult build() {
            if (repositories == null) throw new IllegalStateException("Missing required property: repositories");
            return new GitReposFromRemotesResult(repositories);
        }
    }
}
