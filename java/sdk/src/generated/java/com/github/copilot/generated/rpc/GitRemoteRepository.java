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
 * A GitHub repository one of a working tree's remotes points at.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitRemoteRepository(
    /** Account or organization owning the repository. */
    @JsonProperty("owner") String owner,
    /** Repository name, without the owner. */
    @JsonProperty("name") String name,
    /** GitHub host serving the repository, which is not `github.com` for a GitHub Enterprise remote. */
    @JsonProperty("host") String host,
    /** Name of the first remote that produced this distinct repository entry, such as `origin` or `upstream`. */
    @JsonProperty("remoteName") String remoteName
) {
}
