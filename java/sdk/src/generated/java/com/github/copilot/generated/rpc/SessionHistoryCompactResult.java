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
 * Compaction outcome with the number of tokens and messages removed, summary text, and the resulting context window breakdown.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionHistoryCompactResult(
    /** Whether compaction completed successfully */
    @JsonProperty("success") Boolean success,
    /** Number of tokens freed by compaction */
    @JsonProperty("tokensRemoved") Long tokensRemoved,
    /** Number of messages removed during compaction */
    @JsonProperty("messagesRemoved") Long messagesRemoved,
    /** Summary text produced by compaction. Omitted when compaction did not produce a summary (e.g. failure path). */
    @JsonProperty("summaryContent") String summaryContent,
    /** Post-compaction context window usage breakdown */
    @JsonProperty("contextWindow") HistoryCompactContextWindow contextWindow
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionHistoryCompactResult {}

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
        private Boolean success;
        private Long tokensRemoved;
        private Long messagesRemoved;
        private String summaryContent;
        private HistoryCompactContextWindow contextWindow;

        /**
         * Sets the {@code success} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder success(Boolean value) {
            this.success = value;
            return this;
        }

        /**
         * Sets the {@code tokensRemoved} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder tokensRemoved(Long value) {
            this.tokensRemoved = value;
            return this;
        }

        /**
         * Sets the {@code messagesRemoved} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder messagesRemoved(Long value) {
            this.messagesRemoved = value;
            return this;
        }

        /**
         * Sets the {@code summaryContent} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder summaryContent(String value) {
            this.summaryContent = value;
            return this;
        }

        /**
         * Sets the {@code contextWindow} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder contextWindow(HistoryCompactContextWindow value) {
            this.contextWindow = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionHistoryCompactResult build() {
            if (success == null) throw new IllegalStateException("Missing required property: success");
            if (tokensRemoved == null) throw new IllegalStateException("Missing required property: tokensRemoved");
            if (messagesRemoved == null) throw new IllegalStateException("Missing required property: messagesRemoved");
            return new SessionHistoryCompactResult(success, tokensRemoved, messagesRemoved, summaryContent, contextWindow);
        }
    }
}
