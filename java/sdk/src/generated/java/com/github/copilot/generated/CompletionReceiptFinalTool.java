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
 * Final structured tool completion in the covered event range.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CompletionReceiptFinalTool(
    /** Unique identifier of the completed tool call. */
    @JsonProperty("toolCallId") String toolCallId,
    /** Tool name from the matching tool execution start event, when available. */
    @JsonProperty("toolName") String toolName,
    /** Structured success or failure status from the tool completion event. */
    @JsonProperty("status") CompletionReceiptToolStatus status,
    /** Process exit code from a structured shell result, when available. */
    @JsonProperty("exitCode") Long exitCode
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CompletionReceiptFinalTool {}

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
        private String toolCallId;
        private String toolName;
        private CompletionReceiptToolStatus status;
        private Long exitCode;

        /**
         * Sets the {@code toolCallId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder toolCallId(String value) {
            this.toolCallId = value;
            return this;
        }

        /**
         * Sets the {@code toolName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder toolName(String value) {
            this.toolName = value;
            return this;
        }

        /**
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder status(CompletionReceiptToolStatus value) {
            this.status = value;
            return this;
        }

        /**
         * Sets the {@code exitCode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder exitCode(Long value) {
            this.exitCode = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CompletionReceiptFinalTool build() {
            if (toolCallId == null) throw new IllegalStateException("Missing required property: toolCallId");
            if (status == null) throw new IllegalStateException("Missing required property: status");
            return new CompletionReceiptFinalTool(toolCallId, toolName, status, exitCode);
        }
    }
}
