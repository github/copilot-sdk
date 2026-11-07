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
 * Session event "session.mode_notice_delivered". Records that a mode transition notice reached the model so cache-stable mode tools can remain offered across resume.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionModeNoticeDeliveredEvent extends SessionEvent {

    @Override
    public String getType() { return "session.mode_notice_delivered"; }

    @JsonProperty("data")
    private SessionModeNoticeDeliveredEventData data;

    public SessionModeNoticeDeliveredEventData getData() { return data; }
    public void setData(SessionModeNoticeDeliveredEventData data) { this.data = data; }

    /** Data payload for {@link SessionModeNoticeDeliveredEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionModeNoticeDeliveredEventData(
        /** Mode established by the delivered transition notice */
        @JsonProperty("mode") SessionMode mode,
        /** Model-visible transition notice persisted for a mid-turn delivery */
        @JsonProperty("content") String content,
        /** Whether the model-visible transition notice was persisted separately as an instruction.update event */
        @JsonProperty("instructionUpdatePersisted") Boolean instructionUpdatePersisted
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionModeNoticeDeliveredEventData {}

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
            private SessionMode mode;
            private String content;
            private Boolean instructionUpdatePersisted;

            /**
             * Sets the {@code mode} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder mode(SessionMode value) {
                this.mode = value;
                return this;
            }

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
             * Sets the {@code instructionUpdatePersisted} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder instructionUpdatePersisted(Boolean value) {
                this.instructionUpdatePersisted = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionModeNoticeDeliveredEventData build() {
                if (mode == null) throw new IllegalStateException("Missing required property: mode");
                return new SessionModeNoticeDeliveredEventData(mode, content, instructionUpdatePersisted);
            }
        }
    }
}
