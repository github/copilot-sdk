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
 * Describes the cloud coding-agent task to start and the credential to start it under. Target it either by repository, supplying both `repositoryOwner` and `repositoryName`, or by `owner` alone for an account-level run.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record CloudTasksCreateParams(
    /** Explicit Mission Control endpoint selected by the host. Omission derives it from the identity. */
    @JsonProperty("baseUrl") String baseUrl,
    /** The credential the task is created under, carried opaquely because its shape is the host's own and the runtime only resolves a token and an API endpoint from it. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("authInfo") Object authInfo,
    /** Selects which stored credential to resolve when the host holds more than one. Omit it to resolve the default for the supplied `authInfo`. */
    @JsonProperty("authContextId") String authContextId,
    /** Name of the embedding client, reported to the service so it can attribute the request. */
    @JsonProperty("integrationId") String integrationId,
    /** Account or organization owning the repository the task runs against. Supply it together with `repositoryName`. */
    @JsonProperty("repositoryOwner") String repositoryOwner,
    /** Name of the repository the task runs against. Supply it together with `repositoryOwner`. */
    @JsonProperty("repositoryName") String repositoryName,
    /** Account the task is created for when it is not tied to a repository. Supply it only when `repositoryOwner` and `repositoryName` are omitted. */
    @JsonProperty("owner") String owner
) {
}
