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
 * Durable workflow resource consumption.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowRunConsumed(
    /** Accumulated active execution time in milliseconds. */
    @JsonProperty("activeMs") Long activeMs,
    /** Total subagents spawned by the run. */
    @JsonProperty("subagents") Long subagents,
    /** AI usage consumed by the run in nano-AIU. */
    @JsonProperty("nanoAiu") Long nanoAiu
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public WorkflowRunConsumed {}

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
        private Long activeMs;
        private Long subagents;
        private Long nanoAiu;

        /**
         * Sets the {@code activeMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder activeMs(Long value) {
            this.activeMs = value;
            return this;
        }

        /**
         * Sets the {@code subagents} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder subagents(Long value) {
            this.subagents = value;
            return this;
        }

        /**
         * Sets the {@code nanoAiu} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder nanoAiu(Long value) {
            this.nanoAiu = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public WorkflowRunConsumed build() {
            if (activeMs == null) throw new IllegalStateException("Missing required property: activeMs");
            if (subagents == null) throw new IllegalStateException("Missing required property: subagents");
            if (nanoAiu == null) throw new IllegalStateException("Missing required property: nanoAiu");
            return new WorkflowRunConsumed(activeMs, subagents, nanoAiu);
        }
    }
}
