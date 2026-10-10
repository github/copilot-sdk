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
 * Registry search terms, the credential to search under, and the request id that makes the search cancellable.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record McpRegistrySearchParams(
    /** Request ID from `mcp.registry.allocateRequestId`. The search refuses unknown, reclaimed, or canceled IDs and IDs that another search or request already uses. */
    @JsonProperty("requestId") Long requestId,
    /** The credential the search runs under, carried opaquely. Hosts usually send credential-free `AuthIdentity`; a `token` identity can be resolved only when it embeds a token, while `env` and `gh-cli` identities can use a token embedded in the request first. */
    @JsonProperty("authInfo") Object authInfo,
    /** Free-text query. Omitted or empty asks the registry for its top servers rather than searching. A value that is not a string is refused. */
    @JsonProperty("query") String query,
    /** Repository used for the policy lookup, as `owner/name`. The policy selects the registry URL and whether the user token goes to the registry. The registry receives this repository only when the policy entry lists it as required context. A value that is not a string is refused. */
    @JsonProperty("repository") String repository,
    /** Maximum number of servers to return. */
    @JsonProperty("limit") Long limit
) {
}
