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
 * Parameters for starting fleet orchestration: an optional user prompt combined with the fleet instructions, plus the send options forwarded to the resulting turn.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionFleetStartParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Optional user prompt to combine with fleet instructions */
    @JsonProperty("prompt") String prompt,
    /** Optional attachments (files, directories, selections, blobs, GitHub references) to include with the fleet request */
    @JsonProperty("attachments") List<Object> attachments,
    /** If false, this request will not trigger a Premium Request Unit charge. User requests default to billable. */
    @JsonProperty("billable") Boolean billable,
    /** If true, await completion of the agentic loop for this fleet request before returning. Defaults to false. */
    @JsonProperty("wait") Boolean wait_
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionFleetStartParams {}

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
        private String sessionId;
        private String prompt;
        private List<Object> attachments;
        private Boolean billable;
        private Boolean wait_;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code prompt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder prompt(String value) {
            this.prompt = value;
            return this;
        }

        /**
         * Sets the {@code attachments} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
        public Builder billable(Boolean value) {
            this.billable = value;
            return this;
        }

        /**
         * Sets the {@code wait} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder wait_(Boolean value) {
            this.wait_ = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionFleetStartParams build() {
            return new SessionFleetStartParams(sessionId, prompt, attachments, billable, wait_);
        }
    }
}
