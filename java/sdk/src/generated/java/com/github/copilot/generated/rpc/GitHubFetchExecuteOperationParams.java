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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Arguments for a named authenticated operation.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubFetchExecuteOperationParams(
    /** A fresh request id. */
    @JsonProperty("requestId") Long requestId,
    /** Session whose auth manager to use. Omission selects the process manager. */
    @JsonProperty("sessionId") String sessionId,
    /** Opaque selector for a live host-owned credential binding. Only the owning host connection may use it. */
    @JsonProperty("authOwnerId") String authOwnerId,
    /** The operation to execute. */
    @JsonProperty("operation") GitHubFetchOperation operation,
    /** Named operation arguments, such as owner, repo, or jobId. */
    @JsonProperty("params") Map<String, String> params,
    /** JSON request body, preserved as supplied. */
    @JsonProperty("bodyJson") String bodyJson
) {
}
