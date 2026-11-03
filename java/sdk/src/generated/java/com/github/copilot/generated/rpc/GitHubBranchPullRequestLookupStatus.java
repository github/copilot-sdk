/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Why a branch pull-request lookup reported what it did. The three unsuccessful outcomes are kept apart because each one asks the caller for something different.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
enum GitHubBranchPullRequestLookupStatus {
    /** The {@code found} variant. */
    FOUND("found"),
    /** The {@code no-token} variant. */
    NO_TOKEN("no-token"),
    /** The {@code no-pr} variant. */
    NO_PR("no-pr"),
    /** The {@code lookup-failed} variant. */
    LOOKUP_FAILED("lookup-failed");

    private final String value;
    GitHubBranchPullRequestLookupStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static GitHubBranchPullRequestLookupStatus fromValue(String value) {
        for (GitHubBranchPullRequestLookupStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown GitHubBranchPullRequestLookupStatus value: " + value);
    }
}
