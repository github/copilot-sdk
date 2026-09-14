/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Session event "session.completion_receipt". Behavior-neutral record of structured runtime facts present when an agent completion decision is accepted.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionCompletionReceiptEvent extends SessionEvent {

    @Override
    public String getType() { return "session.completion_receipt"; }

    @JsonProperty("data")
    private SessionCompletionReceiptEventData data;

    public SessionCompletionReceiptEventData getData() { return data; }
    public void setData(SessionCompletionReceiptEventData data) { this.data = data; }

    /** Data payload for {@link SessionCompletionReceiptEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionCompletionReceiptEventData(
        /** Version of the completion receipt payload. */
        @JsonProperty("schemaVersion") Long schemaVersion,
        /** One-based accepted completion receipt ordinal in the durable session history. */
        @JsonProperty("attempt") Long attempt,
        /** Identifier of the assistant turn-end event that supplied the accepted completion boundary. This is the receipt's idempotency key, and always equals eventRange.endEventId. */
        @JsonProperty("sourceEventId") String sourceEventId,
        /** Inclusive durable event range summarized by this receipt. */
        @JsonProperty("eventRange") CompletionReceiptEventRange eventRange,
        /** Runtime reason the completion decision was accepted. */
        @JsonProperty("stopReason") CompletionReceiptStopReason stopReason,
        /** Final structured tool completion in the covered range, when one exists. */
        @JsonProperty("finalTool") CompletionReceiptFinalTool finalTool,
        /** Number of successful structured tool completions in the covered range. */
        @JsonProperty("successfulToolCount") Long successfulToolCount,
        /** Number of failed structured tool completions in the covered range. */
        @JsonProperty("failedToolCount") Long failedToolCount
    ) {
    }
}
