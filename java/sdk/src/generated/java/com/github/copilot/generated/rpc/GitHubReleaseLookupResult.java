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
 * Outcome of a release lookup. `releaseJson` is present only for `found`, and `message` only for the kinds that carry an explanation.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubReleaseLookupResult(
    /** How the lookup ended. */
    @JsonProperty("kind") GitHubReleaseLookupKind kind,
    /** The release as GitHub's own JSON, verbatim. Carried as text rather than a modelled type so the contract does not have to track every field of GitHub's release payload, and so a caller reads the same shape the REST API documents. */
    @JsonProperty("releaseJson") String releaseJson,
    /** Why the lookup produced no release, phrased for a user. */
    @JsonProperty("message") String message
) {
}
