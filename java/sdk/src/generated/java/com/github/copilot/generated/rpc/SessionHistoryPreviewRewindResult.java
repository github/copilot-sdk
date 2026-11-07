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
 * Files and aggregate changes for a prospective rewind.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionHistoryPreviewRewindResult(
    /** Whether file restore is available for this session. This is authoritative: switch on it and read `reason` only when it is false. */
    @JsonProperty("available") Boolean available,
    /** Why file restore is unavailable, when applicable. Populated only when `available` is false and never set when `available` is true. */
    @JsonProperty("reason") HistoryRewindUnavailableReason reason,
    /** Number of unique files in the preview. */
    @JsonProperty("fileCount") Long fileCount,
    /** Files ordered by path. */
    @JsonProperty("files") List<HistoryRewindFilePreview> files
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionHistoryPreviewRewindResult {}

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
        private Boolean available;
        private HistoryRewindUnavailableReason reason;
        private Long fileCount;
        private List<HistoryRewindFilePreview> files;

        /**
         * Sets the {@code available} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder available(Boolean value) {
            this.available = value;
            return this;
        }

        /**
         * Sets the {@code reason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reason(HistoryRewindUnavailableReason value) {
            this.reason = value;
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
         * Sets the {@code files} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder files(List<HistoryRewindFilePreview> value) {
            this.files = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionHistoryPreviewRewindResult build() {
            if (available == null) throw new IllegalStateException("Missing required property: available");
            if (fileCount == null) throw new IllegalStateException("Missing required property: fileCount");
            if (files == null) throw new IllegalStateException("Missing required property: files");
            return new SessionHistoryPreviewRewindResult(available, reason, fileCount, files);
        }
    }
}
