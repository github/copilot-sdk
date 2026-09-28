/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.annotation.processing.Generated;

/**
 * Status snapshot from the original connection, independent of new-work account availability.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "phase", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = McpInstallationOperationStatusPreparing.class, name = "preparing"),
    @JsonSubTypes.Type(value = McpInstallationOperationStatusPrepared.class, name = "prepared"),
    @JsonSubTypes.Type(value = McpInstallationOperationStatusAwaitingConfirmation.class, name = "awaiting-confirmation"),
    @JsonSubTypes.Type(value = McpInstallationOperationStatusRevalidating.class, name = "revalidating"),
    @JsonSubTypes.Type(value = McpInstallationOperationStatusApplying.class, name = "applying"),
    @JsonSubTypes.Type(value = McpInstallationOperationStatusCompleted.class, name = "completed")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public sealed interface McpInstallationOperationStatus permits McpInstallationOperationStatusPreparing, McpInstallationOperationStatusPrepared, McpInstallationOperationStatusAwaitingConfirmation, McpInstallationOperationStatusRevalidating, McpInstallationOperationStatusApplying, McpInstallationOperationStatusCompleted {
    /**
     * Returns the discriminator value for this variant.
     *
     * @return the phase discriminator
     */
    @JsonProperty("phase")
    String phase();
}
