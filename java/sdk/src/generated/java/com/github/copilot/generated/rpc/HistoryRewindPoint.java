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
 * A root user turn that the session can rewind to.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record HistoryRewindPoint(
    /** ID of the user.message event that begins the discarded suffix. */
    @JsonProperty("eventId") String eventId,
    /** User-visible message text for the turn. */
    @JsonProperty("userMessage") String userMessage,
    /** ISO timestamp of the user turn. */
    @JsonProperty("timestamp") String timestamp,
    /** Whether at least one file in this turn or a later turn can be restored. */
    @JsonProperty("canRestoreFiles") Boolean canRestoreFiles,
    /** Number of unique files in this turn and all later turns that have captured changes. */
    @JsonProperty("fileCount") Long fileCount,
    /** Whether this turn itself captured any file changes. */
    @JsonProperty("turnChangedFiles") Boolean turnChangedFiles,
    /** Lines added by this turn's captured file changes. */
    @JsonProperty("linesAdded") Long linesAdded,
    /** Lines removed by this turn's captured file changes. */
    @JsonProperty("linesRemoved") Long linesRemoved,
    /** Whether this turn was an automatically injected autopilot continuation. */
    @JsonProperty("isAutopilotContinuation") Boolean isAutopilotContinuation
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public HistoryRewindPoint {}

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
        private String eventId;
        private String userMessage;
        private String timestamp;
        private Boolean canRestoreFiles;
        private Long fileCount;
        private Boolean turnChangedFiles;
        private Long linesAdded;
        private Long linesRemoved;
        private Boolean isAutopilotContinuation;

        /**
         * Sets the {@code eventId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder eventId(String value) {
            this.eventId = value;
            return this;
        }

        /**
         * Sets the {@code userMessage} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder userMessage(String value) {
            this.userMessage = value;
            return this;
        }

        /**
         * Sets the {@code timestamp} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder timestamp(String value) {
            this.timestamp = value;
            return this;
        }

        /**
         * Sets the {@code canRestoreFiles} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder canRestoreFiles(Boolean value) {
            this.canRestoreFiles = value;
            return this;
        }

        /**
         * Sets the {@code fileCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder fileCount(Long value) {
            this.fileCount = value;
            return this;
        }

        /**
         * Sets the {@code turnChangedFiles} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder turnChangedFiles(Boolean value) {
            this.turnChangedFiles = value;
            return this;
        }

        /**
         * Sets the {@code linesAdded} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder linesAdded(Long value) {
            this.linesAdded = value;
            return this;
        }

        /**
         * Sets the {@code linesRemoved} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder linesRemoved(Long value) {
            this.linesRemoved = value;
            return this;
        }

        /**
         * Sets the {@code isAutopilotContinuation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder isAutopilotContinuation(Boolean value) {
            this.isAutopilotContinuation = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public HistoryRewindPoint build() {
            if (eventId == null) throw new IllegalStateException("Missing required property: eventId");
            if (userMessage == null) throw new IllegalStateException("Missing required property: userMessage");
            if (timestamp == null) throw new IllegalStateException("Missing required property: timestamp");
            if (canRestoreFiles == null) throw new IllegalStateException("Missing required property: canRestoreFiles");
            if (fileCount == null) throw new IllegalStateException("Missing required property: fileCount");
            if (turnChangedFiles == null) throw new IllegalStateException("Missing required property: turnChangedFiles");
            if (linesAdded == null) throw new IllegalStateException("Missing required property: linesAdded");
            if (linesRemoved == null) throw new IllegalStateException("Missing required property: linesRemoved");
            if (isAutopilotContinuation == null) throw new IllegalStateException("Missing required property: isAutopilotContinuation");
            return new HistoryRewindPoint(eventId, userMessage, timestamp, canRestoreFiles, fileCount, turnChangedFiles, linesAdded, linesRemoved, isAutopilotContinuation);
        }
    }
}
