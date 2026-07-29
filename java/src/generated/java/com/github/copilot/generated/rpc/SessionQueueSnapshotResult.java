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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Internal snapshot of native queue state for local session orchestration.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionQueueSnapshotResult(
    /** User-facing pending items in FIFO order. */
    @JsonProperty("items") List<QueuePendingItems> items,
    /** Immediate steering messages waiting for an active turn. */
    @JsonProperty("steeringMessages") List<String> steeringMessages,
    /** Insertion orders for queued items, aligned with `items`. */
    @JsonProperty("itemOrders") List<Long> itemOrders,
    /** Insertion orders for immediate steering messages, aligned with `steeringMessages`. */
    @JsonProperty("steeringMessageOrders") List<Long> steeringMessageOrders
) {
}
