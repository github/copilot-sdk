/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Schema for the `QueuedCommandHandled` type.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class QueuedCommandHandled extends QueuedCommandResult {

    @JsonProperty("handled")
    private final String handled = "true";

    @Override
    public String getHandled() { return handled; }

    /** When true, the runtime will not process subsequent queued commands until a new request comes in. */
    @JsonProperty("stopProcessingQueue")
    private Boolean stopProcessingQueue;

    public Boolean getStopProcessingQueue() { return stopProcessingQueue; }
    public void setStopProcessingQueue(Boolean stopProcessingQueue) { this.stopProcessingQueue = stopProcessingQueue; }
}
