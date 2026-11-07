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
 * Session event "session.journal_link". Links a standalone replay head to its complete preceding numbered journal archive.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionJournalLinkEvent extends SessionEvent {

    @Override
    public String getType() { return "session.journal_link"; }

    @JsonProperty("data")
    private SessionJournalLinkEventData data;

    public SessionJournalLinkEventData getData() { return data; }
    public void setData(SessionJournalLinkEventData data) { this.data = data; }

    /** Data payload for {@link SessionJournalLinkEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionJournalLinkEventData(
        /** Journal checkpoint format version. */
        @JsonProperty("version") Long version,
        /** Session owning this journal chain. */
        @JsonProperty("sessionId") String sessionId,
        /** Numbered predecessor filename in the same session directory. */
        @JsonProperty("previousFile") String previousFile
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionJournalLinkEventData {}

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
            private Long version;
            private String sessionId;
            private String previousFile;

            /**
             * Sets the {@code version} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder version(Long value) {
                this.version = value;
                return this;
            }

            /**
             * Sets the {@code sessionId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder sessionId(String value) {
                this.sessionId = value;
                return this;
            }

            /**
             * Sets the {@code previousFile} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder previousFile(String value) {
                this.previousFile = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionJournalLinkEventData build() {
                if (version == null) throw new IllegalStateException("Missing required property: version");
                if (sessionId == null) throw new IllegalStateException("Missing required property: sessionId");
                if (previousFile == null) throw new IllegalStateException("Missing required property: previousFile");
                return new SessionJournalLinkEventData(version, sessionId, previousFile);
            }
        }
    }
}
