/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * The release channel used for update selection.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
enum GitHubReleaseUpdateChannel {
    /** The {@code stable} variant. */
    STABLE("stable"),
    /** The {@code prerelease} variant. */
    PRERELEASE("prerelease");

    private final String value;
    GitHubReleaseUpdateChannel(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static GitHubReleaseUpdateChannel fromValue(String value) {
        for (GitHubReleaseUpdateChannel v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown GitHubReleaseUpdateChannel value: " + value);
    }
}
