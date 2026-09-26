/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;

/**
 * Exact accepted worker input, distinct from a message, event, caller correlation or Turn.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkerInput(
    /** UUID allocated for this queue item by the admitting producer. */
    @JsonProperty("queueItemId") UUID queueItemId,
    /** Actual recipient task, at most 256 UTF-8 bytes. */
    @JsonProperty("agentId") String agentId,
    /** Original invoking occurrence, never replaced by a reported/root alias. */
    @JsonProperty("sender") WorkerEventReference sender,
    /** Exact captured edges in producer order. Requires sender; at most 32 whole pairs. */
    @JsonProperty("senderBridges") List<WorkerBridgeObservation> senderBridges
) {
}
