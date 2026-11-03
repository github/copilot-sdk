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
 * Identifies one cloud coding-agent task whose sharing status is to be read, and the credential to read it under.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record CloudTasksGetSharingStatusParams(
    /** Explicit Mission Control endpoint selected by the host. Omission derives it from the identity. */
    @JsonProperty("baseUrl") String baseUrl,
    /** The credential the read is performed under, carried opaquely because its shape is the host's own and the runtime only resolves a token and an API endpoint from it. The token itself is never carried across the door. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("authInfo") Object authInfo,
    /** Selects which stored credential to resolve when the host holds more than one. Omit it to resolve the default for the supplied `authInfo`. */
    @JsonProperty("authContextId") String authContextId,
    /** Name of the embedding client, reported to the service so it can attribute the request. */
    @JsonProperty("integrationId") String integrationId,
    /** Identifier of the cloud task whose sharing status is read. */
    @JsonProperty("taskId") String taskId
) {
}
