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
 * Owner and name of a GitHub repository. Deliberately distinct from `GitHubRepositoryIdentity`, which also carries a host: a by-id lookup takes the host as an input and the API's answer reports none, so there is no host here to report faithfully.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubRepositoryOwnerName(
    /** Repository owner login (user or organization). */
    @JsonProperty("owner") String owner,
    /** Repository name, without the owner prefix or the `.git` suffix. */
    @JsonProperty("name") String name
) {
}
