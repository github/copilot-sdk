/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Rolled-up state of one pull-request check. GitHub's own conclusions are collapsed to these three so a caller renders a check without enumerating every API conclusion.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
enum GitHubPullRequestCheckStatus {
    /** The {@code pass} variant. */
    PASS("pass"),
    /** The {@code fail} variant. */
    FAIL("fail"),
    /** The {@code pending} variant. */
    PENDING("pending");

    private final String value;
    GitHubPullRequestCheckStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static GitHubPullRequestCheckStatus fromValue(String value) {
        for (GitHubPullRequestCheckStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown GitHubPullRequestCheckStatus value: " + value);
    }
}
