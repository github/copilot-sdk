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
import javax.annotation.processing.Generated;

/**
 * Number of events that were removed by the truncation.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionHistoryTruncateResult(
    /** Number of events that were removed */
    @JsonProperty("eventsRemoved") Long eventsRemoved,
    /** True when conversation truncation succeeded but post-truncation workspace checkpoint cleanup failed. History is already truncated; callers may still prune snapshots but should report a checkpoint-cleanup rather than a truncation failure. */
    @JsonProperty("checkpointCleanupFailed") Boolean checkpointCleanupFailed,
    /** Failure detail when checkpointCleanupFailed is true. */
    @JsonProperty("checkpointCleanupError") String checkpointCleanupError
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionHistoryTruncateResult {}

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
        private Long eventsRemoved;
        private Boolean checkpointCleanupFailed;
        private String checkpointCleanupError;

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
         * Sets the {@code checkpointCleanupFailed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder checkpointCleanupFailed(Boolean value) {
            this.checkpointCleanupFailed = value;
            return this;
        }

        /**
         * Sets the {@code checkpointCleanupError} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder checkpointCleanupError(String value) {
            this.checkpointCleanupError = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionHistoryTruncateResult build() {
            if (eventsRemoved == null) throw new IllegalStateException("Missing required property: eventsRemoved");
            return new SessionHistoryTruncateResult(eventsRemoved, checkpointCleanupFailed, checkpointCleanupError);
        }
    }
}
