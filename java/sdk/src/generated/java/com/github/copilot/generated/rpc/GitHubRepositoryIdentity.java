/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Owner, name, and host of a GitHub repository, as resolved from a git remote URL.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubRepositoryIdentity(
    /** Repository owner login (user or organization). */
    @JsonProperty("owner") String owner,
    /** Repository name, without the owner prefix or the `.git` suffix. */
    @JsonProperty("name") String name,
    /** Host the remote points at, for example `github.com` or a GitHub Enterprise hostname. */
    @JsonProperty("host") String host
) {
}
