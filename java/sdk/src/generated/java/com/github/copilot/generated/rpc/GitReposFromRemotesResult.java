/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * The GitHub repositories a working tree's remotes point at.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitReposFromRemotesResult(
    /** One entry per distinct GitHub repository, in the order git reports the first remote for each repository. Empty when no remote points at a GitHub host, which a caller should read as `not connected to GitHub`. Failing to read the remotes is an error, not an empty list. */
    @JsonProperty("repositories") List<GitRemoteRepository> repositories
) {
}
