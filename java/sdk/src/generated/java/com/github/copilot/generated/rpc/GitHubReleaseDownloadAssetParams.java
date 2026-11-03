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
 * Which release asset to download, the credential to download it under, and the request id that makes the download cancellable.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubReleaseDownloadAssetParams(
    /** Request id from `gitHubRelease.allocateRequestId`. An id that was never registered, or one already used, is refused rather than silently running uncancellable. */
    @JsonProperty("requestId") Long requestId,
    /** The asset's `browser_download_url` from the release JSON: an HTTPS release download URL of a repository that `gitHubRelease.lookup` serves. */
    @JsonProperty("assetUrl") String assetUrl,
    /** The credential the download runs under, carried opaquely because its shape is the host's own and the runtime only resolves a token from it. No credential travels: this selects one the runtime already holds. Omitted when the caller has no credential, which still succeeds against the public release repository at a lower rate limit. */
    @JsonProperty("authInfo") Object authInfo,
    /** Expected SHA-256 of the asset: 64 hex digits in either case, or the release asset's `digest` value as is (`sha256:` and the hex digits). When given and the downloaded bytes differ, the file is removed and the result is `digest-mismatch`. */
    @JsonProperty("expectedSha256") String expectedSha256,
    /** Snapshot request id from this connection. The download retains that snapshot's original credential source instead of authInfo. Missing, foreign, canceled, or revoked references fail without fallback. Omission preserves ordinary downloads. The download still uses its own requestId for progress and cancellation. */
    @JsonProperty("snapshotRequestId") Long snapshotRequestId
) {
}
