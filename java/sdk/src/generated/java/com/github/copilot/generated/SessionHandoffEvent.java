/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * Session event "session.handoff". Session handoff metadata including source, context, and repository information
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionHandoffEvent extends SessionEvent {

    @Override
    public String getType() { return "session.handoff"; }

    @JsonProperty("data")
    private SessionHandoffEventData data;

    public SessionHandoffEventData getData() { return data; }
    public void setData(SessionHandoffEventData data) { this.data = data; }

    /** Data payload for {@link SessionHandoffEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionHandoffEventData(
        /** ISO 8601 timestamp when the handoff occurred */
        @JsonProperty("handoffTime") OffsetDateTime handoffTime,
        /** Origin type of the session being handed off */
        @JsonProperty("sourceType") HandoffSourceType sourceType,
        /** Repository context for the handed-off session */
        @JsonProperty("repository") HandoffRepository repository,
        /** Additional context information for the handoff */
        @JsonProperty("context") String context,
        /** Summary of the work done in the source session */
        @JsonProperty("summary") String summary,
        /** Session ID of the remote session being handed off */
        @JsonProperty("remoteSessionId") String remoteSessionId,
        /** GitHub host URL for the source session (e.g., https://github.com or https://tenant.ghe.com) */
        @JsonProperty("host") String host
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionHandoffEventData {}

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
            private OffsetDateTime handoffTime;
            private HandoffSourceType sourceType;
            private HandoffRepository repository;
            private String context;
            private String summary;
            private String remoteSessionId;
            private String host;

            /**
             * Sets the {@code handoffTime} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder handoffTime(OffsetDateTime value) {
                this.handoffTime = value;
                return this;
            }

            /**
             * Sets the {@code sourceType} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder sourceType(HandoffSourceType value) {
                this.sourceType = value;
                return this;
            }

            /**
             * Sets the {@code repository} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder repository(HandoffRepository value) {
                this.repository = value;
                return this;
            }

            /**
             * Sets the {@code context} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder context(String value) {
                this.context = value;
                return this;
            }

            /**
             * Sets the {@code summary} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder summary(String value) {
                this.summary = value;
                return this;
            }

            /**
             * Sets the {@code remoteSessionId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder remoteSessionId(String value) {
                this.remoteSessionId = value;
                return this;
            }

            /**
             * Sets the {@code host} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder host(String value) {
                this.host = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionHandoffEventData build() {
                if (handoffTime == null) throw new IllegalStateException("Missing required property: handoffTime");
                if (sourceType == null) throw new IllegalStateException("Missing required property: sourceType");
                return new SessionHandoffEventData(handoffTime, sourceType, repository, context, summary, remoteSessionId, host);
            }
        }
    }
}
