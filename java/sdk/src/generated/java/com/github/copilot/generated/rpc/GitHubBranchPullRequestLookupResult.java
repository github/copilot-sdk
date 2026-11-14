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
 * Outcome of a branch pull-request lookup. `status` carries why, because 'not authenticated', 'no pull request' and 'the search failed' are three different answers and only the first two are conclusive.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubBranchPullRequestLookupResult(
    /** Why the lookup reported what it did. */
    @JsonProperty("status") GitHubBranchPullRequestLookupStatus status,
    /** The matching pull request, or null for every status other than `found`. */
    @JsonProperty("pullRequest") GitHubBranchPullRequest pullRequest
) {
}
