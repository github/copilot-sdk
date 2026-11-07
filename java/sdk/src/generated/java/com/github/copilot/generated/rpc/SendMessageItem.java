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
import javax.annotation.processing.Generated;

/**
 * A single user message to append to the session as part of a `session.sendMessages` turn
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SendMessageItem(
    /** The user message text */
    @JsonProperty("prompt") String prompt,
    /** If provided, this is shown in the timeline instead of `prompt` */
    @JsonProperty("displayPrompt") String displayPrompt,
    /** Optional attachments (files, directories, selections, blobs, GitHub references) to include with this message */
    @JsonProperty("attachments") List<Object> attachments,
    /** If false, this message will not trigger a Premium Request Unit charge. User messages default to billable. */
    @JsonProperty("billable") Boolean billable,
    /** If set, the request will fail if the named tool is not available when this message is among the user messages at the start of the current exchange */
    @JsonProperty("requiredTool") String requiredTool,
    /** Optional provenance tag copied to the resulting user.message event. Must be `user`, `system`, `command-<command-id>` for command-originated messages, `schedule-<numeric-id>` for scheduled prompts, or `agent-<agent-id>` for prompts sent by another agent. */
    @JsonProperty("source") String source
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SendMessageItem {}

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
        private Boolean billable;
        private String requiredTool;
        private String source;

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
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SendMessageItem build() {
            if (prompt == null) throw new IllegalStateException("Missing required property: prompt");
            return new SendMessageItem(prompt, displayPrompt, attachments, billable, requiredTool, source);
        }
    }
}
