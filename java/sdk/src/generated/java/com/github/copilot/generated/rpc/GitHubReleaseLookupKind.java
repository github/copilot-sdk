/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * How a release lookup ended. A caller tells a conclusive absence from a transport error by this alone, without parsing `message`.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
enum GitHubReleaseLookupKind {
    /** The {@code found} variant. */
    FOUND("found"),
    /** The {@code not-found} variant. */
    NOT_FOUND("not-found"),
    /** The {@code no-release} variant. */
    NO_RELEASE("no-release"),
    /** The {@code rate-limited} variant. */
    RATE_LIMITED("rate-limited"),
    /** The {@code failed} variant. */
    FAILED("failed");

    private final String value;
    GitHubReleaseLookupKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static GitHubReleaseLookupKind fromValue(String value) {
        for (GitHubReleaseLookupKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown GitHubReleaseLookupKind value: " + value);
    }
}
