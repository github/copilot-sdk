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
 * Which release to look up, the credential to look it up under, and the request id that makes the lookup cancellable.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubReleaseLookupParams(
    /** Request id from `gitHubRelease.allocateRequestId`. An id that was never registered, or one already used, is refused rather than silently running uncancellable. */
    @JsonProperty("requestId") Long requestId,
    /** Which release to look up. */
    @JsonProperty("query") GitHubReleaseQuery query,
    /** The credential the lookup runs under, carried opaquely because its shape is the host's own and the runtime only resolves a token and a GitHub host from it. No credential travels: this selects one the runtime already holds. Omitted when the caller has no credential, which still succeeds against the public release repository at a lower rate limit. */
    @JsonProperty("authInfo") Object authInfo,
    /** The tag to look up. Required by the `tag` query and ignored by the others. */
    @JsonProperty("value") String value
) {
}
