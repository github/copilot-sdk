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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Outcome of an owner listing. Exactly one of `owners` and `message` is present, except that `throwError` reports a failure the caller is expected to raise rather than render.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubOwnersListResult(
    /** The owners, on success: the authenticated user first, then the organizations they belong to. */
    @JsonProperty("owners") List<GitHubOwnerOption> owners,
    /** Why no owners could be listed, phrased for a user. Present when the listing failed in a way the caller should render rather than raise. */
    @JsonProperty("message") String message,
    /** A line the caller should log. Present only alongside `message`, and only for failures worth recording. */
    @JsonProperty("warning") String warning,
    /** A malformed request or an unreadable credential, which the caller raises instead of rendering. Kept a field rather than a dispatch error so it stays distinct from `message`, which the caller renders. */
    @JsonProperty("throwError") String throwError
) {
}
