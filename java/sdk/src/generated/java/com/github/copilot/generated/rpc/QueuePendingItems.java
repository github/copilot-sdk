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
    /** Whether this item is a queued user message or a queued slash command / model change */
    @JsonProperty("kind") QueuePendingItemsKind kind,
    /** Human-readable text to display for this queue entry in the UI */
    @JsonProperty("displayText") String displayText,
    /** Agent mode stored on this queued entry, as stamped when it was enqueued. Items without an explicit mode report interactive. This is not necessarily the mode that will constrain the turn: a plan or autopilot session applies its own write gate, continuation loop and permission posture to every drained item regardless of the mode stored here. */
    @JsonProperty("agentMode") SendAgentMode agentMode,
    /** Optional source tag associated with this pending queue entry. This is an open string, not authenticated authorship. In particular, `user` does not prove that a person typed the message. If the source is absent or unrecognized, consumers must not infer human or agent authorship and should handle the entry neutrally. Consumers should tolerate future source values. */
    @JsonProperty("source") String source
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public QueuePendingItems {}

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
        private String id;
        private String messageId;
        private QueuePendingItemsKind kind;
        private String displayText;
        private SendAgentMode agentMode;
        private String source;

        /**
         * Sets the {@code id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder id(String value) {
            this.id = value;
            return this;
        }

        /**
         * Sets the {@code messageId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder messageId(String value) {
            this.messageId = value;
            return this;
        }

        /**
         * Sets the {@code kind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder kind(QueuePendingItemsKind value) {
            this.kind = value;
            return this;
        }

        /**
         * Sets the {@code displayText} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder displayText(String value) {
            this.displayText = value;
            return this;
        }

        /**
         * Sets the {@code agentMode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder agentMode(SendAgentMode value) {
            this.agentMode = value;
            return this;
        }

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder source(String value) {
            this.source = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public QueuePendingItems build() {
            if (id == null) throw new IllegalStateException("Missing required property: id");
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            if (displayText == null) throw new IllegalStateException("Missing required property: displayText");
            if (agentMode == null) throw new IllegalStateException("Missing required property: agentMode");
            return new QueuePendingItems(id, messageId, kind, displayText, agentMode, source);
        }
    }
}
