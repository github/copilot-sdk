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
 * Downloads a model version to its configured local library.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AzureMlDownloadParams(
    /** Unique request identifier for progress and explicit cancellation. */
    @JsonProperty("requestId") String requestId,
    /** Registry that owns the model. */
    @JsonProperty("registry") AzureMlRegistry registry,
    /** Exact model-family name. */
    @JsonProperty("name") String name,
    /** Exact published version. */
    @JsonProperty("version") String version,
    /** Version directory under this server's Azure ML library. */
    @JsonProperty("destination") String destination,
    /** Optional connection-owned lease that spans the caller's complete family mutation. */
    @JsonProperty("leaseId") String leaseId
) {
}
