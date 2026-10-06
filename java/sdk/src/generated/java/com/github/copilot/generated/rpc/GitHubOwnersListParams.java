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
 * Credential to list owners under, and the request id that makes the listing cancellable.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubOwnersListParams(
    /** Request id from `gitHubOwners.nextRequestId`. An id that was never registered, canceled before use, released after being abandoned, or already used is refused rather than silently running uncancellable. */
    @JsonProperty("requestId") Long requestId,
    /** The credential the listing runs under, carried opaquely because its shape is the host's own and the runtime only resolves a token and a GitHub host from it. No credential travels: this selects one the runtime already holds. */
    @JsonProperty("authInfo") Object authInfo
) {
}
