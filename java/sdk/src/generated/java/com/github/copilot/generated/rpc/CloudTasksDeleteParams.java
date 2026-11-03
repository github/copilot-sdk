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
 * Identifies one cloud coding-agent task to delete, and the credential to delete it under.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record CloudTasksDeleteParams(
    /** Explicit Mission Control endpoint selected by the host. Omission derives it from the identity. */
    @JsonProperty("baseUrl") String baseUrl,
    /** The credential the deletion is performed under, carried opaquely because its shape is the host's own and the runtime only resolves a token and an API endpoint from it. Required: the task endpoint is per-account, so there is no unauthenticated form of this call. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("authInfo") Object authInfo,
    /** Selects which stored credential to resolve when the host holds more than one. Omit it to resolve the default for the supplied `authInfo`. */
    @JsonProperty("authContextId") String authContextId,
    /** Name of the embedding client, reported to Mission Control so the service can attribute the request. */
    @JsonProperty("integrationId") String integrationId,
    /** Identifier of the cloud task to delete. */
    @JsonProperty("taskId") String taskId
) {
}
