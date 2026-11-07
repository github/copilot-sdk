/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Internal durable terminal request staged by a HydraFusion phase until an idempotent final commit selects it.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FusionStagedTerminal(
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("assistantMessage") Object assistantMessage,
    @JsonProperty("toolName") String toolName,
    @JsonProperty("toolCallId") String toolCallId,
    @JsonProperty("arguments") String arguments,
    @JsonProperty("phaseId") String phaseId
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public FusionStagedTerminal {}

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
        private Object assistantMessage;
        private boolean $presence0;
        private String toolName;
        private String toolCallId;
        private String arguments;
        private String phaseId;

        /**
         * Sets the {@code assistantMessage} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder assistantMessage(Object value) {
            this.assistantMessage = value;
            this.$presence0 = true;
            return this;
        }

        /**
         * Sets the {@code toolName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder toolName(String value) {
            this.toolName = value;
            return this;
        }

        /**
         * Sets the {@code toolCallId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder toolCallId(String value) {
            this.toolCallId = value;
            return this;
        }

        /**
         * Sets the {@code arguments} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder arguments(String value) {
            this.arguments = value;
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
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public FusionStagedTerminal build() {
            if (!$presence0) throw new IllegalStateException("Missing required property: assistantMessage");
            if (toolName == null) throw new IllegalStateException("Missing required property: toolName");
            if (toolCallId == null) throw new IllegalStateException("Missing required property: toolCallId");
            if (arguments == null) throw new IllegalStateException("Missing required property: arguments");
            if (phaseId == null) throw new IllegalStateException("Missing required property: phaseId");
            return new FusionStagedTerminal(assistantMessage, toolName, toolCallId, arguments, phaseId);
        }
    }
}
