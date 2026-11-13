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
}
