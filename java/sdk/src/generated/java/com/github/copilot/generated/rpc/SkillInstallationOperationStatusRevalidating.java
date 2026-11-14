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
 * Original operation progress discriminator.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public record SkillInstallationOperationStatusRevalidating(
    /** Original runtime-issued operation identity. */
    @JsonProperty("operationId") String operationId,
    /** Whether cancellation has been requested. */
    @JsonProperty("cancellationRequested") Boolean cancellationRequested,
    @JsonProperty("phase") String phase
) implements SkillInstallationOperationStatus {
    public SkillInstallationOperationStatusRevalidating {
        phase = "revalidating";
    }

    public SkillInstallationOperationStatusRevalidating(
        String operationId,
        Boolean cancellationRequested
    ) {
        this(operationId, cancellationRequested, "revalidating");
    }
}
