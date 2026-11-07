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
 * Selects the pull request a branch is open against, and the credential to look it up under. The branch is searched across every repository in `baseRepositories`, which lets a fork's branch find the pull request it opened against upstream.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubRepositoryLookupBranchPullRequestParams(
    /** The credential the lookup runs under, carried opaquely because its shape is the host's own and the runtime only resolves a token and a GitHub host from it. Required: the search answers per-account, so a pull request invisible to this credential is reported as absent rather than refused. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("authInfo") Object authInfo,
    /** Login that must own the pull request's head branch. A branch name alone is ambiguous across forks, so a candidate whose head is owned by anyone else is discarded. */
    @JsonProperty("headOwner") String headOwner,
    /** Repositories to search for the pull request, in preference order. A fork supplies both its own repository and its upstream, because the branch may have opened the pull request against either. */
    @JsonProperty("baseRepositories") List<GitHubRepositoryOwnerName> baseRepositories,
    /** Head branch name to find the pull request for, without a `refs/heads/` prefix. */
    @JsonProperty("branch") String branch,
    /** Whether a closed or merged pull request counts as a match. False considers only open ones, so a caller acting on the branch's current pull request does not resurface a merged one. */
    @JsonProperty("includeClosed") Boolean includeClosed
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    GitHubRepositoryLookupBranchPullRequestParams {}

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
        private Object authInfo;
        private boolean $presence0;
        private String headOwner;
        private List<GitHubRepositoryOwnerName> baseRepositories;
        private String branch;
        private Boolean includeClosed;

        /**
         * Sets the {@code authInfo} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder authInfo(Object value) {
            this.authInfo = value;
            this.$presence0 = true;
            return this;
        }

        /**
         * Sets the {@code headOwner} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder headOwner(String value) {
            this.headOwner = value;
            return this;
        }

        /**
         * Sets the {@code baseRepositories} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder baseRepositories(List<GitHubRepositoryOwnerName> value) {
            this.baseRepositories = value;
            return this;
        }

        /**
         * Sets the {@code branch} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder branch(String value) {
            this.branch = value;
            return this;
        }

        /**
         * Sets the {@code includeClosed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder includeClosed(Boolean value) {
            this.includeClosed = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public GitHubRepositoryLookupBranchPullRequestParams build() {
            if (!$presence0) throw new IllegalStateException("Missing required property: authInfo");
            if (headOwner == null) throw new IllegalStateException("Missing required property: headOwner");
            if (baseRepositories == null) throw new IllegalStateException("Missing required property: baseRepositories");
            if (branch == null) throw new IllegalStateException("Missing required property: branch");
            if (includeClosed == null) throw new IllegalStateException("Missing required property: includeClosed");
            return new GitHubRepositoryLookupBranchPullRequestParams(authInfo, headOwner, baseRepositories, branch, includeClosed);
        }
    }
}
