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
 * Outcome of the prune operation: deleted IDs, dry-run candidates, skipped IDs, total bytes freed, and the dry-run flag.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionsPruneOldResult(
    /** Session IDs that were deleted (always empty in dry-run mode) */
    @JsonProperty("deleted") List<String> deleted,
    /** Session IDs that would be deleted in dry-run mode (always empty otherwise) */
    @JsonProperty("candidates") List<String> candidates,
    /** Session IDs that were skipped (e.g., named sessions) */
    @JsonProperty("skipped") List<String> skipped,
    /** Total bytes freed (actual when not dry-run, projected when dry-run) */
    @JsonProperty("freedBytes") Long freedBytes,
    /** True when no deletions were actually performed */
    @JsonProperty("dryRun") Boolean dryRun
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionsPruneOldResult {}

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
        private List<String> deleted;
        private List<String> candidates;
        private List<String> skipped;
        private Long freedBytes;
        private Boolean dryRun;

        /**
         * Sets the {@code deleted} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder deleted(List<String> value) {
            this.deleted = value;
            return this;
        }

        /**
         * Sets the {@code candidates} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder candidates(List<String> value) {
            this.candidates = value;
            return this;
        }

        /**
         * Sets the {@code skipped} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder skipped(List<String> value) {
            this.skipped = value;
            return this;
        }

        /**
         * Sets the {@code freedBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder freedBytes(Long value) {
            this.freedBytes = value;
            return this;
        }

        /**
         * Sets the {@code dryRun} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder dryRun(Boolean value) {
            this.dryRun = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionsPruneOldResult build() {
            if (deleted == null) throw new IllegalStateException("Missing required property: deleted");
            if (candidates == null) throw new IllegalStateException("Missing required property: candidates");
            if (skipped == null) throw new IllegalStateException("Missing required property: skipped");
            if (freedBytes == null) throw new IllegalStateException("Missing required property: freedBytes");
            if (dryRun == null) throw new IllegalStateException("Missing required property: dryRun");
            return new SessionsPruneOldResult(deleted, candidates, skipped, freedBytes, dryRun);
        }
    }
}
