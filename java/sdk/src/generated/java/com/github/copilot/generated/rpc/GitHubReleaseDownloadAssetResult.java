/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Outcome of an asset download. Each field is present only for the kinds its description names.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubReleaseDownloadAssetResult(
    /** How the download ended. */
    @JsonProperty("kind") GitHubReleaseDownloadKind kind,
    /** Absolute path of the downloaded file, for `downloaded`. The file is alone in a fresh directory that the caller deletes once it has used the file. */
    @JsonProperty("path") String path,
    /** Size of the downloaded file in bytes, for `downloaded`. */
    @JsonProperty("sizeBytes") Long sizeBytes,
    /** SHA-256 of the downloaded bytes as lowercase hex, for `downloaded` and `digest-mismatch`. */
    @JsonProperty("sha256") String sha256,
    /** HTTP status of the final response, for `http-error`. */
    @JsonProperty("status") Long status,
    /** HTTP status text of the final response, for `http-error`. */
    @JsonProperty("statusText") String statusText,
    /** Why the download produced no file, phrased for a user, for `digest-mismatch` and `failed`. */
    @JsonProperty("message") String message
) {
}
