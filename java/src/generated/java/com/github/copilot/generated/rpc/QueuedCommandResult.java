/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.annotation.processing.Generated;

/**
 * Result of the queued command execution.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "handled", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = QueuedCommandHandled.class, name = "true"),
    @JsonSubTypes.Type(value = QueuedCommandNotHandled.class, name = "false")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class QueuedCommandResult {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the handled discriminator
     */
    public abstract String getHandled();
}
