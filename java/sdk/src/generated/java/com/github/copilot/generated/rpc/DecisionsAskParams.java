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
 * One request to a decision model: shared context plus the questions to answer about it.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DecisionsAskParams(
    /** Opaque account identifier returned by `account.getAllUsers`. When omitted, the current account is used. */
    @JsonProperty("selectionId") String selectionId,
    /** GitHub token to ask as. When provided, it is used instead of the current account or `selectionId`. */
    @JsonProperty("gitHubToken") String gitHubToken,
    /** Decision model id from `models.list` `decisionModels`. When omitted, the runtime uses its default decision model for the account. */
    @JsonProperty("model") String model,
    /** Shared context every question sees. The runtime never truncates it; an oversized request fails with `tooLarge`. */
    @JsonProperty("context") Object context,
    /** Questions to answer about the context. Names must be unique; answers come back in the same order. */
    @JsonProperty("questions") List<Object> questions,
    /** End-to-end deadline in milliseconds, covering credential resolution, queueing, the model call and parsing. Defaults to 5000; allowed range 250-30000. */
    @JsonProperty("timeoutMs") Long timeoutMs,
    /** Static identifier of the calling feature, used only for telemetry and never sent to the model, for example `vscode.chat.route`. Unrecognized values are reported as `other`. */
    @JsonProperty("purpose") String purpose
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public DecisionsAskParams {}

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
        private String selectionId;
        private String gitHubToken;
        private String model;
        private Object context;
        private List<Object> questions;
        private Long timeoutMs;
        private String purpose;

        /**
         * Sets the {@code selectionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder selectionId(String value) {
            this.selectionId = value;
            return this;
        }

        /**
         * Sets the {@code gitHubToken} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder gitHubToken(String value) {
            this.gitHubToken = value;
            return this;
        }

        /**
         * Sets the {@code model} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder model(String value) {
            this.model = value;
            return this;
        }

        /**
         * Sets the {@code context} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder context(Object value) {
            this.context = value;
            return this;
        }

        /**
         * Sets the {@code questions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder questions(List<Object> value) {
            this.questions = value;
            return this;
        }

        /**
         * Sets the {@code timeoutMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder timeoutMs(Long value) {
            this.timeoutMs = value;
            return this;
        }

        /**
         * Sets the {@code purpose} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder purpose(String value) {
            this.purpose = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public DecisionsAskParams build() {
            if (context == null) throw new IllegalStateException("Missing required property: context");
            if (questions == null) throw new IllegalStateException("Missing required property: questions");
            return new DecisionsAskParams(selectionId, gitHubToken, model, context, questions, timeoutMs, purpose);
        }
    }
}
