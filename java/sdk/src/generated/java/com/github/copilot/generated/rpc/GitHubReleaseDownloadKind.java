/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * How an asset download ended. A caller tells a refused response from a transport failure or a cancellation by this alone, without parsing `message`.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
enum GitHubReleaseDownloadKind {
    /** The {@code downloaded} variant. */
    DOWNLOADED("downloaded"),
    /** The {@code http-error} variant. */
    HTTP_ERROR("http-error"),
    /** The {@code digest-mismatch} variant. */
    DIGEST_MISMATCH("digest-mismatch"),
    /** The {@code cancelled} variant. */
    CANCELLED("cancelled"),
    /** The {@code failed} variant. */
    FAILED("failed");

    private final String value;
    GitHubReleaseDownloadKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static GitHubReleaseDownloadKind fromValue(String value) {
        for (GitHubReleaseDownloadKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown GitHubReleaseDownloadKind value: " + value);
    }
}
