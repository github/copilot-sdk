/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * A runtime-owned GitHub or Copilot operation.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
enum GitHubFetchOperation {
    /** The {@code repository-access} variant. */
    REPOSITORY_ACCESS("repository-access"),
    /** The {@code branch-exists} variant. */
    BRANCH_EXISTS("branch-exists"),
    /** The {@code job-create} variant. */
    JOB_CREATE("job-create"),
    /** The {@code job-status} variant. */
    JOB_STATUS("job-status"),
    /** The {@code sessions-list} variant. */
    SESSIONS_LIST("sessions-list"),
    /** The {@code pull-requests-graphql} variant. */
    PULL_REQUESTS_GRAPHQL("pull-requests-graphql"),
    /** The {@code pull-request} variant. */
    PULL_REQUEST("pull-request"),
    /** The {@code pull-request-files} variant. */
    PULL_REQUEST_FILES("pull-request-files");

    private final String value;
    GitHubFetchOperation(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static GitHubFetchOperation fromValue(String value) {
        for (GitHubFetchOperation v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown GitHubFetchOperation value: " + value);
    }
}
