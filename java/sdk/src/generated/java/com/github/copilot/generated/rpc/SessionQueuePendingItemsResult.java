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
 * Snapshot of the session's pending queued items and immediate-steering messages.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionQueuePendingItemsResult(
    /** Pending queued items in submission order. Includes user messages, queued slash commands, and queued model changes; omits internal system items. */
    @JsonProperty("items") List<QueuePendingItems> items,
    /** Display text for messages currently in the immediate steering queue (interjections sent during a running turn). */
    @JsonProperty("steeringMessages") List<String> steeringMessages,
    /** How many leading entries of `steeringMessages` have already been folded into the running turn (and so have an emitted `user.message`), as opposed to still waiting for one. Absent for hosts that do not distinguish the two. */
    @JsonProperty("inFlightSteeringCount") Long inFlightSteeringCount,
    /** ID of the running turn's user message while the model has not answered it, so `withdrawMessage` can still take it back once nothing sent after it is pending. A message leaves `items` when its turn starts, before its `user.message` is recorded; this tells that message apart from one that was removed. Absent when no turn prompt can be taken back. */
    @JsonProperty("withdrawableTurnMessageId") String withdrawableTurnMessageId
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionQueuePendingItemsResult {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    public static final class Builder {
        private Builder() {}
        private List<QueuePendingItems> items;
        private List<String> steeringMessages;
        private Long inFlightSteeringCount;
        private String withdrawableTurnMessageId;

        /**
         * Sets the {@code items} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder items(List<QueuePendingItems> value) {
            this.items = value;
            return this;
        }

        /**
         * Sets the {@code steeringMessages} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder steeringMessages(List<String> value) {
            this.steeringMessages = value;
            return this;
        }

        /**
         * Sets the {@code inFlightSteeringCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder inFlightSteeringCount(Long value) {
            this.inFlightSteeringCount = value;
            return this;
        }

        /**
         * Sets the {@code withdrawableTurnMessageId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder withdrawableTurnMessageId(String value) {
            this.withdrawableTurnMessageId = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionQueuePendingItemsResult build() {
            if (items == null) throw new IllegalStateException("Missing required property: items");
            if (steeringMessages == null) throw new IllegalStateException("Missing required property: steeringMessages");
            return new SessionQueuePendingItemsResult(items, steeringMessages, inFlightSteeringCount, withdrawableTurnMessageId);
        }
    }
}
