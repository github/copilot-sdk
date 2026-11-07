/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Serializable message fields accepted by queue.insertAt.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record QueueInsertMessage(
    /** The user message text. */
    @JsonProperty("prompt") String prompt,
    /** Optional user-facing display text. */
    @JsonProperty("displayPrompt") String displayPrompt,
    /** Optional attachments for the message. */
    @JsonProperty("attachments") List<Object> attachments,
    /** Optional explicit agent mode. When omitted, the session's current mode is assigned. */
    @JsonProperty("agentMode") SendAgentMode agentMode,
    /** Optional provenance source. `system` is rejected: it would hide the inserted row from `pendingItems` and make it unaddressable while still executing, so inserted items must stay visible. */
    @JsonProperty("source") String source,
    /** Whether the message is billable. */
    @JsonProperty("billable") Boolean billable,
    /** Required tool name for the turn, when any. */
    @JsonProperty("requiredTool") String requiredTool,
    /** Per-turn request headers. */
    @JsonProperty("requestHeaders") Map<String, String> requestHeaders,
    /** Accepted for SendOptions compatibility but ignored; inserted items always use queued delivery semantics. */
    @JsonProperty("mode") SendMode mode,
    /** Accepted for SendOptions compatibility but ignored; the requested public position controls placement. */
    @JsonProperty("prepend") Boolean prepend,
    /** Accepted for SendOptions compatibility but ignored; insertion scheduling is controlled by the queue drain state. */
    @JsonProperty("wait") Boolean wait_,
    /** Accepted for internal SendOptions compatibility but ignored; delivery is derived from current session activity. */
    @JsonProperty("delivery") String delivery
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public QueueInsertMessage {}

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
        private String prompt;
        private String displayPrompt;
        private List<Object> attachments;
        private SendAgentMode agentMode;
        private String source;
        private Boolean billable;
        private String requiredTool;
        private Map<String, String> requestHeaders;
        private SendMode mode;
        private Boolean prepend;
        private Boolean wait_;
        private String delivery;

        /**
         * Sets the {@code prompt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder prompt(String value) {
            this.prompt = value;
            return this;
        }

        /**
         * Sets the {@code displayPrompt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder displayPrompt(String value) {
            this.displayPrompt = value;
            return this;
        }

        /**
         * Sets the {@code attachments} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder attachments(List<Object> value) {
            this.attachments = value;
            return this;
        }

        /**
         * Sets the {@code agentMode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder agentMode(SendAgentMode value) {
            this.agentMode = value;
            return this;
        }

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder source(String value) {
            this.source = value;
            return this;
        }

        /**
         * Sets the {@code billable} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder billable(Boolean value) {
            this.billable = value;
            return this;
        }

        /**
         * Sets the {@code requiredTool} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder requiredTool(String value) {
            this.requiredTool = value;
            return this;
        }

        /**
         * Sets the {@code requestHeaders} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder requestHeaders(Map<String, String> value) {
            this.requestHeaders = value;
            return this;
        }

        /**
         * Sets the {@code mode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mode(SendMode value) {
            this.mode = value;
            return this;
        }

        /**
         * Sets the {@code prepend} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder prepend(Boolean value) {
            this.prepend = value;
            return this;
        }

        /**
         * Sets the {@code wait} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder wait_(Boolean value) {
            this.wait_ = value;
            return this;
        }

        /**
         * Sets the {@code delivery} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder delivery(String value) {
            this.delivery = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public QueueInsertMessage build() {
            if (prompt == null) throw new IllegalStateException("Missing required property: prompt");
            return new QueueInsertMessage(prompt, displayPrompt, attachments, agentMode, source, billable, requiredTool, requestHeaders, mode, prepend, wait_, delivery);
        }
    }
}
