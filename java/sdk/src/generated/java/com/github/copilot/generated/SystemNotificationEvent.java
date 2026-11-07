/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Session event "system.notification". System-generated notification for runtime events like background task completion
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SystemNotificationEvent extends SessionEvent {

    @Override
    public String getType() { return "system.notification"; }

    @JsonProperty("data")
    private SystemNotificationEventData data;

    public SystemNotificationEventData getData() { return data; }
    public void setData(SystemNotificationEventData data) { this.data = data; }

    /** Data payload for {@link SystemNotificationEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SystemNotificationEventData(
        /** The notification text, typically wrapped in <system_notification> XML tags */
        @JsonProperty("content") String content,
        /** Structured metadata identifying what triggered this notification */
        @JsonProperty("kind") Object kind,
        /** IDs of accepted assistant messages whose output this notification consumed, referring to assistant.message.data.messageId across the agent tree. Captured from the delivered snapshot, not the sender's later state. An empty list means no model-produced dependency; absence means provenance is unavailable. */
        @JsonProperty("causalDependencies") List<String> causalDependencies,
        /** Provider reasoning settings anchored before this model-facing message for cache-stable replay; the historical responsesReasoning name is retained for compatibility */
        @JsonProperty("responsesReasoning") ResponsesReasoning responsesReasoning
    ) {

        /**
         * Retains the constructor published in Java SDK 1.0.28.
         * @deprecated Use named construction via {@link #builder()}.
         * @param content The notification text, typically wrapped in <system_notification> XML tags
         * @param kind Structured metadata identifying what triggered this notification
         * @param responsesReasoning Provider reasoning settings anchored before this model-facing message for cache-stable replay; the historical responsesReasoning name is retained for compatibility
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SystemNotificationEventData(
            String content,
            Object kind,
            ResponsesReasoning responsesReasoning
        ) {
            this(content, kind, null, responsesReasoning);
        }

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SystemNotificationEventData {}

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
            private String content;
            private Object kind;
            private List<String> causalDependencies;
            private ResponsesReasoning responsesReasoning;

            /**
             * Sets the {@code content} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder content(String value) {
                this.content = value;
                return this;
            }

            /**
             * Sets the {@code kind} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder kind(Object value) {
                this.kind = value;
                return this;
            }

            /**
             * Sets the {@code causalDependencies} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder causalDependencies(List<String> value) {
                this.causalDependencies = value;
                return this;
            }

            /**
             * Sets the {@code responsesReasoning} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder responsesReasoning(ResponsesReasoning value) {
                this.responsesReasoning = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SystemNotificationEventData build() {
                if (content == null) throw new IllegalStateException("Missing required property: content");
                if (kind == null) throw new IllegalStateException("Missing required property: kind");
                return new SystemNotificationEventData(content, kind, causalDependencies, responsesReasoning);
            }
        }
    }
}
