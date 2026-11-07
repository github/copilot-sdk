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
 * Content-free structural summary of the failing request for diagnosing malformed 4xx calls
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelCallFailureRequestFingerprint(
    /** Total number of messages in the request */
    @JsonProperty("messageCount") Long messageCount,
    /** Number of "tool" result messages in the request */
    @JsonProperty("toolResultMessageCount") Long toolResultMessageCount,
    /** Total number of tool calls across assistant messages */
    @JsonProperty("toolCallCount") Long toolCallCount,
    /** Tool calls whose name is missing or empty (rejected by strict providers) */
    @JsonProperty("namelessToolCallCount") Long namelessToolCallCount,
    /** Free-form custom tool calls in the request history before request shaping, counted apart from named function calls (rejected by strict providers) */
    @JsonProperty("customToolCallCount") Long customToolCallCount,
    /** Tool result messages in the request history before request shaping that answer no earlier unanswered tool call */
    @JsonProperty("unmatchedToolResultCount") Long unmatchedToolResultCount,
    /** Tool calls in the request history before request shaping that no later tool result message answers */
    @JsonProperty("orphanToolCallCount") Long orphanToolCallCount,
    /** Total number of image content parts */
    @JsonProperty("imagePartCount") Long imagePartCount,
    /** Image parts whose media type cannot be determined (rejected by strict providers) */
    @JsonProperty("imagePartsMissingMediaType") Long imagePartsMissingMediaType,
    /** Summed byte length of inline image payloads (data URLs and base64 sources) */
    @JsonProperty("imageBytes") Long imageBytes,
    /** Number of messages carrying opaque or encrypted reasoning */
    @JsonProperty("reasoningItemCount") Long reasoningItemCount,
    /** Summed byte length of opaque or encrypted reasoning payloads */
    @JsonProperty("encryptedContentBytes") Long encryptedContentBytes,
    /** Role of the final message in the request */
    @JsonProperty("lastMessageRole") String lastMessageRole
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ModelCallFailureRequestFingerprint {}

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
        private Long messageCount;
        private Long toolResultMessageCount;
        private Long toolCallCount;
        private Long namelessToolCallCount;
        private Long customToolCallCount;
        private Long unmatchedToolResultCount;
        private Long orphanToolCallCount;
        private Long imagePartCount;
        private Long imagePartsMissingMediaType;
        private Long imageBytes;
        private Long reasoningItemCount;
        private Long encryptedContentBytes;
        private String lastMessageRole;

        /**
         * Sets the {@code messageCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder messageCount(Long value) {
            this.messageCount = value;
            return this;
        }

        /**
         * Sets the {@code toolResultMessageCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder toolResultMessageCount(Long value) {
            this.toolResultMessageCount = value;
            return this;
        }

        /**
         * Sets the {@code toolCallCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder toolCallCount(Long value) {
            this.toolCallCount = value;
            return this;
        }

        /**
         * Sets the {@code namelessToolCallCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder namelessToolCallCount(Long value) {
            this.namelessToolCallCount = value;
            return this;
        }

        /**
         * Sets the {@code customToolCallCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder customToolCallCount(Long value) {
            this.customToolCallCount = value;
            return this;
        }

        /**
         * Sets the {@code unmatchedToolResultCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder unmatchedToolResultCount(Long value) {
            this.unmatchedToolResultCount = value;
            return this;
        }

        /**
         * Sets the {@code orphanToolCallCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder orphanToolCallCount(Long value) {
            this.orphanToolCallCount = value;
            return this;
        }

        /**
         * Sets the {@code imagePartCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder imagePartCount(Long value) {
            this.imagePartCount = value;
            return this;
        }

        /**
         * Sets the {@code imagePartsMissingMediaType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder imagePartsMissingMediaType(Long value) {
            this.imagePartsMissingMediaType = value;
            return this;
        }

        /**
         * Sets the {@code imageBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder imageBytes(Long value) {
            this.imageBytes = value;
            return this;
        }

        /**
         * Sets the {@code reasoningItemCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reasoningItemCount(Long value) {
            this.reasoningItemCount = value;
            return this;
        }

        /**
         * Sets the {@code encryptedContentBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder encryptedContentBytes(Long value) {
            this.encryptedContentBytes = value;
            return this;
        }

        /**
         * Sets the {@code lastMessageRole} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder lastMessageRole(String value) {
            this.lastMessageRole = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ModelCallFailureRequestFingerprint build() {
            if (messageCount == null) throw new IllegalStateException("Missing required property: messageCount");
            if (toolResultMessageCount == null) throw new IllegalStateException("Missing required property: toolResultMessageCount");
            if (toolCallCount == null) throw new IllegalStateException("Missing required property: toolCallCount");
            if (namelessToolCallCount == null) throw new IllegalStateException("Missing required property: namelessToolCallCount");
            if (imagePartCount == null) throw new IllegalStateException("Missing required property: imagePartCount");
            if (imagePartsMissingMediaType == null) throw new IllegalStateException("Missing required property: imagePartsMissingMediaType");
            return new ModelCallFailureRequestFingerprint(messageCount, toolResultMessageCount, toolCallCount, namelessToolCallCount, customToolCallCount, unmatchedToolResultCount, orphanToolCallCount, imagePartCount, imagePartsMissingMediaType, imageBytes, reasoningItemCount, encryptedContentBytes, lastMessageRole);
        }
    }
}
