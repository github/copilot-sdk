/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Why the runtime is requesting a GitHub credential.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum GitHubTokenAcquireReason {
    /** The {@code initial} variant. */
    INITIAL("initial"),
    /** The {@code refresh} variant. */
    REFRESH("refresh");

    private final String value;
    GitHubTokenAcquireReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static GitHubTokenAcquireReason fromValue(String value) {
        for (GitHubTokenAcquireReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown GitHubTokenAcquireReason value: " + value);
    }
}
