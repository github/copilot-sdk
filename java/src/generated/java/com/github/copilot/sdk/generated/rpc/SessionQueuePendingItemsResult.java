/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Snapshot of the session's pending queued items and immediate-steering messages.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionQueuePendingItemsResult(
    /** Pending queued items in submission order. Includes user messages, queued slash commands, and queued model changes; omits internal system items. */
    @JsonProperty("items") List<QueuePendingItems> items,
    /** Display text for messages currently in the immediate steering queue (interjections sent during a running turn). */
    @JsonProperty("steeringMessages") List<String> steeringMessages
) {
}
