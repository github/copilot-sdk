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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Prompt message and JSON schema describing the form fields to elicit from the user.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionUiElicitationParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Elicitation mode. Omitted and form are equivalent for structured elicitation. */
    @JsonProperty("mode") McpElicitationFormMode mode,
    /** Message describing what information is needed from the user */
    @JsonProperty("message") String message,
    /** JSON Schema describing the form fields to present to the user */
    @JsonProperty("requestedSchema") UIElicitationSchema requestedSchema,
    /** MCP request metadata. */
    @JsonProperty("_meta") Map<String, Object> meta,
    /** MCP task metadata. */
    @JsonProperty("task") McpTaskMetadata task
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionUiElicitationParams {}

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
        private McpElicitationFormMode mode;
        private String message;
        private UIElicitationSchema requestedSchema;
        private Map<String, Object> meta;
        private McpTaskMetadata task;

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
         * Sets the {@code mode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder mode(McpElicitationFormMode value) {
            this.mode = value;
            return this;
        }

        /**
         * Sets the {@code message} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder message(String value) {
            this.message = value;
            return this;
        }

        /**
         * Sets the {@code requestedSchema} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder requestedSchema(UIElicitationSchema value) {
            this.requestedSchema = value;
            return this;
        }

        /**
         * Sets the {@code _meta} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder meta(Map<String, Object> value) {
            this.meta = value;
            return this;
        }

        /**
         * Sets the {@code task} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder task(McpTaskMetadata value) {
            this.task = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionUiElicitationParams build() {
            if (message == null) throw new IllegalStateException("Missing required property: message");
            if (requestedSchema == null) throw new IllegalStateException("Missing required property: requestedSchema");
            return new SessionUiElicitationParams(sessionId, mode, message, requestedSchema, meta, task);
        }
    }
}
