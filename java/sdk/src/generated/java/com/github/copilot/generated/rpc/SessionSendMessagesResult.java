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
 * Result of sending zero or more user messages
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionSendMessagesResult(
    /** Unique identifiers assigned to the messages, one per provided message in order. For a batch that starts a run, assistant messages use the final ID as originatingMessageId throughout that run, including tool iterations and stop-hook corrections. Immediate steering does not replace the active run's origin. Empty when no messages were provided; that run has no originatingMessageId. */
    @JsonProperty("messageIds") List<String> messageIds
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionSendMessagesResult {}

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
        private List<String> messageIds;

        /**
         * Sets the {@code messageIds} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder messageIds(List<String> value) {
            this.messageIds = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionSendMessagesResult build() {
            if (messageIds == null) throw new IllegalStateException("Missing required property: messageIds");
            return new SessionSendMessagesResult(messageIds);
        }
    }
}
