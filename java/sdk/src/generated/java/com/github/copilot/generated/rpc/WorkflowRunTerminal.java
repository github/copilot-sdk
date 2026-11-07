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
 * Prompt-safe terminal workflow outcome.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowRunTerminal(
    /** Human-readable terminal reason. */
    @JsonProperty("reason") String reason,
    /** Machine-readable terminal failure. */
    @JsonProperty("failure") Object failure,
    /** Human-readable terminal error. */
    @JsonProperty("error") String error,
    /** Prompt-safe preview of the completed result. */
    @JsonProperty("resultPreview") String resultPreview,
    /** Pause initiator metadata, or null when the run did not pause. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("pauseInfo") Object pauseInfo
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public WorkflowRunTerminal {}

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
        private String reason;
        private Object failure;
        private String error;
        private String resultPreview;
        private Object pauseInfo;
        private boolean $presence4;

        /**
         * Sets the {@code reason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder reason(String value) {
            this.reason = value;
            return this;
        }

        /**
         * Sets the {@code failure} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder failure(Object value) {
            this.failure = value;
            return this;
        }

        /**
         * Sets the {@code error} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder error(String value) {
            this.error = value;
            return this;
        }

        /**
         * Sets the {@code resultPreview} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder resultPreview(String value) {
            this.resultPreview = value;
            return this;
        }

        /**
         * Sets the {@code pauseInfo} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder pauseInfo(Object value) {
            this.pauseInfo = value;
            this.$presence4 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public WorkflowRunTerminal build() {
            if (!$presence4) throw new IllegalStateException("Missing required property: pauseInfo");
            return new WorkflowRunTerminal(reason, failure, error, resultPreview, pauseInfo);
        }
    }
}
