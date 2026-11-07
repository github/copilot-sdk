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
 * A bidirectional page of workflow progress.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionWorkflowGetRunProgressResult(
    /** Progress records in sequence order. */
    @JsonProperty("records") List<WorkflowProgressLine> records,
    /** Oldest sequence number in this page, or null when empty. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("oldestSeq") Long oldestSeq,
    /** Newest sequence number in this page, or null when empty. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("newestSeq") Long newestSeq,
    /** Whether progress records older than this page exist. */
    @JsonProperty("hasMoreOlder") Boolean hasMoreOlder,
    /** Whether progress records newer than this page exist. */
    @JsonProperty("hasMoreNewer") Boolean hasMoreNewer,
    /** Run revision reflected by this page. */
    @JsonProperty("revision") Long revision
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionWorkflowGetRunProgressResult {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static final class Builder {
        private Builder() {}
        private List<WorkflowProgressLine> records;
        private Long oldestSeq;
        private boolean $presence1;
        private Long newestSeq;
        private boolean $presence2;
        private Boolean hasMoreOlder;
        private Boolean hasMoreNewer;
        private Long revision;

        /**
         * Sets the {@code records} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder records(List<WorkflowProgressLine> value) {
            this.records = value;
            return this;
        }

        /**
         * Sets the {@code oldestSeq} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder oldestSeq(Long value) {
            this.oldestSeq = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Sets the {@code newestSeq} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder newestSeq(Long value) {
            this.newestSeq = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Sets the {@code hasMoreOlder} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder hasMoreOlder(Boolean value) {
            this.hasMoreOlder = value;
            return this;
        }

        /**
         * Sets the {@code hasMoreNewer} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder hasMoreNewer(Boolean value) {
            this.hasMoreNewer = value;
            return this;
        }

        /**
         * Sets the {@code revision} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder revision(Long value) {
            this.revision = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionWorkflowGetRunProgressResult build() {
            if (records == null) throw new IllegalStateException("Missing required property: records");
            if (!$presence1) throw new IllegalStateException("Missing required property: oldestSeq");
            if (!$presence2) throw new IllegalStateException("Missing required property: newestSeq");
            if (hasMoreOlder == null) throw new IllegalStateException("Missing required property: hasMoreOlder");
            if (hasMoreNewer == null) throw new IllegalStateException("Missing required property: hasMoreNewer");
            if (revision == null) throw new IllegalStateException("Missing required property: revision");
            return new SessionWorkflowGetRunProgressResult(records, oldestSeq, newestSeq, hasMoreOlder, hasMoreNewer, revision);
        }
    }
}
