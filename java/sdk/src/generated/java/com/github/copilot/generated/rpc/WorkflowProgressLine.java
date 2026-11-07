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
 * One durable workflow progress record.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowProgressLine(
    /** Global monotonic sequence number within the run. */
    @JsonProperty("seq") Long seq,
    /** Resume attempt that emitted this record. */
    @JsonProperty("attempt") Long attempt,
    /** Phase active when the record was emitted, or null before any phase. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("phaseId") String phaseId,
    /** Epoch milliseconds when the record was persisted. */
    @JsonProperty("recordedAt") Long recordedAt,
    /** Progress record kind. */
    @JsonProperty("kind") WorkflowLogLineKind kind,
    /** Prompt-safe progress text. */
    @JsonProperty("text") String text
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public WorkflowProgressLine {}

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
        private Long seq;
        private Long attempt;
        private String phaseId;
        private boolean $presence2;
        private Long recordedAt;
        private WorkflowLogLineKind kind;
        private String text;

        /**
         * Sets the {@code seq} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder seq(Long value) {
            this.seq = value;
            return this;
        }

        /**
         * Sets the {@code attempt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder attempt(Long value) {
            this.attempt = value;
            return this;
        }

        /**
         * Sets the {@code phaseId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder phaseId(String value) {
            this.phaseId = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Sets the {@code recordedAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder recordedAt(Long value) {
            this.recordedAt = value;
            return this;
        }

        /**
         * Sets the {@code kind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder kind(WorkflowLogLineKind value) {
            this.kind = value;
            return this;
        }

        /**
         * Sets the {@code text} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder text(String value) {
            this.text = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public WorkflowProgressLine build() {
            if (seq == null) throw new IllegalStateException("Missing required property: seq");
            if (attempt == null) throw new IllegalStateException("Missing required property: attempt");
            if (!$presence2) throw new IllegalStateException("Missing required property: phaseId");
            if (recordedAt == null) throw new IllegalStateException("Missing required property: recordedAt");
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            if (text == null) throw new IllegalStateException("Missing required property: text");
            return new WorkflowProgressLine(seq, attempt, phaseId, recordedAt, kind, text);
        }
    }
}
