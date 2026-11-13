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
 * Variant {@code false} of {@link EnqueueCommandResult}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class EnqueueCommandResultFalse extends EnqueueCommandResult {

    @JsonProperty("queued")
    private final String queued = "false";

    @Override
    public String getQueued() { return queued; }

    /** Legacy null queue ID accepted for compatibility with older runtimes. */
    @JsonProperty("queueId")
    private Object queueId;

    public Object getQueueId() { return queueId; }
    public void setQueueId(Object queueId) { this.queueId = queueId; }
}
