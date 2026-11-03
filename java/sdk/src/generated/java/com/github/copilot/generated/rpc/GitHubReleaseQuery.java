/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Which release to look up. Enumerated rather than free text because the runtime accepts exactly these and rejects anything else.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
enum GitHubReleaseQuery {
    /** The {@code latest-stable} variant. */
    LATEST_STABLE("latest-stable"),
    /** The {@code latest-prerelease} variant. */
    LATEST_PRERELEASE("latest-prerelease"),
    /** The {@code latest-app} variant. */
    LATEST_APP("latest-app"),
    /** The {@code tag} variant. */
    TAG("tag"),
    /** The {@code release-list} variant. */
    RELEASE_LIST("release-list");

    private final String value;
    GitHubReleaseQuery(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static GitHubReleaseQuery fromValue(String value) {
        for (GitHubReleaseQuery v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown GitHubReleaseQuery value: " + value);
    }
}
