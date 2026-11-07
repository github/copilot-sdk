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
 * The pull request a branch is open against, with the review state a caller needs to summarize it without a second request.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubBranchPullRequest(
    /** Owner login of the repository the pull request was opened against. */
    @JsonProperty("owner") String owner,
    /** Name of the repository the pull request was opened against. */
    @JsonProperty("repo") String repo,
    /** Pull request title. */
    @JsonProperty("title") String title,
    /** Pull request state as GitHub reports it, such as `OPEN`, `CLOSED` or `MERGED`. */
    @JsonProperty("state") String state,
    /** Web URL of the pull request. */
    @JsonProperty("url") String url,
    /** Pull request number within its repository. */
    @JsonProperty("number") Long number,
    /** Pull request description body. Empty when the pull request has none. */
    @JsonProperty("body") String body,
    /** Label names applied to the pull request. */
    @JsonProperty("labels") List<String> labels,
    /** Number of comments on the pull request. */
    @JsonProperty("comments") Long comments,
    /** Logins of the requested reviewers. */
    @JsonProperty("reviewers") List<String> reviewers,
    /** Whether the pull request is a draft. */
    @JsonProperty("isDraft") Boolean isDraft,
    /** Head branch name the pull request was opened from. */
    @JsonProperty("headRefName") String headRefName,
    /** Lines added across the pull request. */
    @JsonProperty("additions") Long additions,
    /** Lines deleted across the pull request. */
    @JsonProperty("deletions") Long deletions,
    /** Number of files the pull request changes. */
    @JsonProperty("changedFiles") Long changedFiles,
    /** Checks reported against the pull request's head commit. */
    @JsonProperty("checks") List<GitHubPullRequestCheck> checks
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    GitHubBranchPullRequest {}

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
        private String repo;
        private String title;
        private String state;
        private String url;
        private Long number;
        private String body;
        private List<String> labels;
        private Long comments;
        private List<String> reviewers;
        private Boolean isDraft;
        private String headRefName;
        private Long additions;
        private Long deletions;
        private Long changedFiles;
        private List<GitHubPullRequestCheck> checks;

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
         * Sets the {@code repo} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder repo(String value) {
            this.repo = value;
            return this;
        }

        /**
         * Sets the {@code title} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder title(String value) {
            this.title = value;
            return this;
        }

        /**
         * Sets the {@code state} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder state(String value) {
            this.state = value;
            return this;
        }

        /**
         * Sets the {@code url} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder url(String value) {
            this.url = value;
            return this;
        }

        /**
         * Sets the {@code number} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder number(Long value) {
            this.number = value;
            return this;
        }

        /**
         * Sets the {@code body} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder body(String value) {
            this.body = value;
            return this;
        }

        /**
         * Sets the {@code labels} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder labels(List<String> value) {
            this.labels = value;
            return this;
        }

        /**
         * Sets the {@code comments} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder comments(Long value) {
            this.comments = value;
            return this;
        }

        /**
         * Sets the {@code reviewers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reviewers(List<String> value) {
            this.reviewers = value;
            return this;
        }

        /**
         * Sets the {@code isDraft} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder isDraft(Boolean value) {
            this.isDraft = value;
            return this;
        }

        /**
         * Sets the {@code headRefName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder headRefName(String value) {
            this.headRefName = value;
            return this;
        }

        /**
         * Sets the {@code additions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder additions(Long value) {
            this.additions = value;
            return this;
        }

        /**
         * Sets the {@code deletions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder deletions(Long value) {
            this.deletions = value;
            return this;
        }

        /**
         * Sets the {@code changedFiles} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder changedFiles(Long value) {
            this.changedFiles = value;
            return this;
        }

        /**
         * Sets the {@code checks} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder checks(List<GitHubPullRequestCheck> value) {
            this.checks = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public GitHubBranchPullRequest build() {
            if (owner == null) throw new IllegalStateException("Missing required property: owner");
            if (repo == null) throw new IllegalStateException("Missing required property: repo");
            if (title == null) throw new IllegalStateException("Missing required property: title");
            if (state == null) throw new IllegalStateException("Missing required property: state");
            if (url == null) throw new IllegalStateException("Missing required property: url");
            if (number == null) throw new IllegalStateException("Missing required property: number");
            if (body == null) throw new IllegalStateException("Missing required property: body");
            if (labels == null) throw new IllegalStateException("Missing required property: labels");
            if (comments == null) throw new IllegalStateException("Missing required property: comments");
            if (reviewers == null) throw new IllegalStateException("Missing required property: reviewers");
            if (isDraft == null) throw new IllegalStateException("Missing required property: isDraft");
            if (headRefName == null) throw new IllegalStateException("Missing required property: headRefName");
            if (additions == null) throw new IllegalStateException("Missing required property: additions");
            if (deletions == null) throw new IllegalStateException("Missing required property: deletions");
            if (changedFiles == null) throw new IllegalStateException("Missing required property: changedFiles");
            if (checks == null) throw new IllegalStateException("Missing required property: checks");
            return new GitHubBranchPullRequest(owner, repo, title, state, url, number, body, labels, comments, reviewers, isDraft, headRefName, additions, deletions, changedFiles, checks);
        }
    }
}
