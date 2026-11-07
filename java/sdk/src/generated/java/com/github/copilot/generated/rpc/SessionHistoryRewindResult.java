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
 * Structured outcome of a rewind request.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionHistoryRewindResult(
    /** Overall rewind outcome. This discriminates the result: it governs which of the remaining fields are populated, so consumers must switch on it before reading `eventsRemoved`, `restoredFiles`, `skippedFiles`, or `error`. See each field for the outcomes that populate it. */
    @JsonProperty("outcome") HistoryRewindOutcome outcome,
    /** Number of persisted events removed by conversation truncation. Present only when truncation succeeded (outcomes `success`, `checkpoint-cleanup-failed`, and `snapshot-prune-failed`); omitted for every unavailable outcome (`session-busy`, `file-change-tracking-disabled`, `unsupported-remote-session`) and for `truncation-failed`, `files-rolled-back`, and `rollback-incomplete`. */
    @JsonProperty("eventsRemoved") Long eventsRemoved,
    /** Absolute paths restored to their captured preimages. Always empty for conversation-only rewinds and for the unavailable outcomes (`session-busy`, `file-change-tracking-disabled`, `unsupported-remote-session`); only conversation-and-files outcomes that reached the file-restore stage populate it. */
    @JsonProperty("restoredFiles") List<String> restoredFiles,
    /** Captured files intentionally left unchanged. Always empty for conversation-only rewinds and for the unavailable outcomes (`session-busy`, `file-change-tracking-disabled`, `unsupported-remote-session`); only conversation-and-files outcomes that reached the file-restore stage populate it. */
    @JsonProperty("skippedFiles") List<HistorySkippedFileRestore> skippedFiles,
    /** Failure detail. Set only for the failure and partial-failure outcomes (`files-rolled-back`, `rollback-incomplete`, `truncation-failed`, `checkpoint-cleanup-failed`, `snapshot-prune-failed`); omitted for `success` and for the unavailable outcomes (`session-busy`, `file-change-tracking-disabled`, `unsupported-remote-session`). */
    @JsonProperty("error") String error
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionHistoryRewindResult {}

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
        private HistoryRewindOutcome outcome;
        private Long eventsRemoved;
        private List<String> restoredFiles;
        private List<HistorySkippedFileRestore> skippedFiles;
        private String error;

        /**
         * Sets the {@code outcome} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder outcome(HistoryRewindOutcome value) {
            this.outcome = value;
            return this;
        }

        /**
         * Sets the {@code eventsRemoved} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder eventsRemoved(Long value) {
            this.eventsRemoved = value;
            return this;
        }

        /**
         * Sets the {@code restoredFiles} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder restoredFiles(List<String> value) {
            this.restoredFiles = value;
            return this;
        }

        /**
         * Sets the {@code skippedFiles} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder skippedFiles(List<HistorySkippedFileRestore> value) {
            this.skippedFiles = value;
            return this;
        }

        /**
         * Sets the {@code error} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder error(String value) {
            this.error = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionHistoryRewindResult build() {
            if (outcome == null) throw new IllegalStateException("Missing required property: outcome");
            if (restoredFiles == null) throw new IllegalStateException("Missing required property: restoredFiles");
            if (skippedFiles == null) throw new IllegalStateException("Missing required property: skippedFiles");
            return new SessionHistoryRewindResult(outcome, eventsRemoved, restoredFiles, skippedFiles, error);
        }
    }
}
