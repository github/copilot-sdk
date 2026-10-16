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
 * Variant {@code awaiting-confirmation} of {@link McpInstallationOperationStatus}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public record McpInstallationOperationStatusAwaitingConfirmation(
    /** Original runtime-issued operation identity. */
    @JsonProperty("operationId") String operationId,
    /** Whether the pending human callback was asked to cancel. */
    @JsonProperty("cancellationRequested") Boolean cancellationRequested,
    /** Original operation progress discriminator. */
    @JsonProperty("phase") String phase
) implements McpInstallationOperationStatus {
    public McpInstallationOperationStatusAwaitingConfirmation {
        phase = "awaiting-confirmation";
    }

    public McpInstallationOperationStatusAwaitingConfirmation(
        String operationId,
        Boolean cancellationRequested
    ) {
        this(operationId, cancellationRequested, "awaiting-confirmation");
    }
}
