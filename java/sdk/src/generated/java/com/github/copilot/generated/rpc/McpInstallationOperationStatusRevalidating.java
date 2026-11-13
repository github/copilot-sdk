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
 * Variant {@code revalidating} of {@link McpInstallationOperationStatus}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public record McpInstallationOperationStatusRevalidating(
    /** Original runtime-issued operation identity. */
    @JsonProperty("operationId") String operationId,
    /** Whether source or authority revalidation was asked to cancel. */
    @JsonProperty("cancellationRequested") Boolean cancellationRequested,
    /** Original operation progress discriminator. */
    @JsonProperty("phase") String phase
) implements McpInstallationOperationStatus {
    public McpInstallationOperationStatusRevalidating {
        phase = "revalidating";
    }

    public McpInstallationOperationStatusRevalidating(
        String operationId,
        Boolean cancellationRequested
    ) {
        this(operationId, cancellationRequested, "revalidating");
    }
}
