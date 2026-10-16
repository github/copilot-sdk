/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.annotation.processing.Generated;

/**
 * Variant {@code completed} of {@link McpInstallationOperationStatus}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public record McpInstallationOperationStatusCompleted(
    /** Original runtime-issued operation identity. */
    @JsonProperty("operationId") String operationId,
    /** Whether cancellation was requested before the terminal result. */
    @JsonProperty("cancellationRequested") Boolean cancellationRequested,
    /** Immutable terminal receipt. */
    @JsonProperty("outcome") Object outcome,
    /** Original operation progress discriminator. */
    @JsonProperty("phase") String phase
) implements McpInstallationOperationStatus {
    public McpInstallationOperationStatusCompleted {
        phase = "completed";
    }

    public McpInstallationOperationStatusCompleted(
        String operationId,
        Boolean cancellationRequested,
        Object outcome
    ) {
        this(operationId, cancellationRequested, outcome, "completed");
    }
}
