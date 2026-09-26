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
 * User-facing pending queue entry, with kind and display text for a queued message, slash command, or model change.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record QueuePendingItems(
    /** Stable opaque id for the canonical queued item. Batch rows share one id. */
    @JsonProperty("id") String id,
    /** Stable identity of the queued user message. Present for message rows and absent for slash commands and model changes. */
    @JsonProperty("messageId") String messageId,
    /** Caller-owned diagnostic UUID from the exact accepted native session.send or sendMessages item, when RUNTIME_ADMISSION_TRACE_CONTEXT is enabled. Omitted for unsupported or identity-less rows, including snapshot-only mirrors. Not an idempotency key, authorization, or permission to retry; repeated values remain ambiguous. */
    @JsonProperty("clientCorrelationId") String clientCorrelationId,
    /** Whether this item is a queued user message or a queued slash command / model change */
    @JsonProperty("kind") QueuePendingItemsKind kind,
    /** Human-readable text to display for this queue entry in the UI */
    @JsonProperty("displayText") String displayText,
    /** Agent mode stored on this queued entry, as stamped when it was enqueued. Items without an explicit mode report interactive. This is not necessarily the mode that will constrain the turn: a plan or autopilot session applies its own write gate, continuation loop and permission posture to every drained item regardless of the mode stored here. */
    @JsonProperty("agentMode") SendAgentMode agentMode,
    /** Optional source tag associated with this pending queue entry. This is an open string, not authenticated authorship. In particular, `user` does not prove that a person typed the message. If the source is absent or unrecognized, consumers must not infer human or agent authorship and should handle the entry neutrally. Consumers should tolerate future source values. */
    @JsonProperty("source") String source
) {
    /** Creates a value without optional admission correlation metadata. */
    public QueuePendingItems(String id, String messageId, QueuePendingItemsKind kind, String displayText, SendAgentMode agentMode, String source) {
        this(id, messageId, null, kind, displayText, agentMode, source);
    }

    /** Creates a value without optional admission correlation metadata. */
    public QueuePendingItems(String id, String messageId, QueuePendingItemsKind kind, String displayText, SendAgentMode agentMode) {
        this(id, messageId, null, kind, displayText, agentMode, null);
    }
}
