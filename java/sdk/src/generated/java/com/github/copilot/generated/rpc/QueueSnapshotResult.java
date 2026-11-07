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
import javax.annotation.processing.Generated;

/**
 * Internal snapshot of native queue state for local session orchestration.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record QueueSnapshotResult(
    /** Queue item identifier of a model switch that has been dequeued but not yet applied. */
    @JsonProperty("inFlightModelChangeId") String inFlightModelChangeId,
    /** User-facing pending items in FIFO order. */
    @JsonProperty("items") List<QueuePendingItems> items,
    /** Immediate steering messages waiting for an active turn. */
    @JsonProperty("steeringMessages") List<String> steeringMessages,
    /** Insertion orders for queued items, aligned with `items`. */
    @JsonProperty("itemOrders") List<Long> itemOrders,
    /** Insertion orders for immediate steering messages, aligned with `steeringMessages`. */
    @JsonProperty("steeringMessageOrders") List<Long> steeringMessageOrders
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    QueueSnapshotResult {}

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
        private String inFlightModelChangeId;
        private List<QueuePendingItems> items;
        private List<String> steeringMessages;
        private List<Long> itemOrders;
        private List<Long> steeringMessageOrders;

        /**
         * Sets the {@code inFlightModelChangeId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder inFlightModelChangeId(String value) {
            this.inFlightModelChangeId = value;
            return this;
        }

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
         * Sets the {@code itemOrders} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder itemOrders(List<Long> value) {
            this.itemOrders = value;
            return this;
        }

        /**
         * Sets the {@code steeringMessageOrders} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder steeringMessageOrders(List<Long> value) {
            this.steeringMessageOrders = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public QueueSnapshotResult build() {
            if (items == null) throw new IllegalStateException("Missing required property: items");
            if (steeringMessages == null) throw new IllegalStateException("Missing required property: steeringMessages");
            return new QueueSnapshotResult(inFlightModelChangeId, items, steeringMessages, itemOrders, steeringMessageOrders);
        }
    }
}
