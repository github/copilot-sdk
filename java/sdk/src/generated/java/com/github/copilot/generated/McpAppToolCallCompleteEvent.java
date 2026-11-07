/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Session event "mcp_app.tool_call_complete". MCP App view called a tool on a connected MCP server (SEP-1865)
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class McpAppToolCallCompleteEvent extends SessionEvent {

    @Override
    public String getType() { return "mcp_app.tool_call_complete"; }

    @JsonProperty("data")
    private McpAppToolCallCompleteEventData data;

    public McpAppToolCallCompleteEventData getData() { return data; }
    public void setData(McpAppToolCallCompleteEventData data) { this.data = data; }

    /** Data payload for {@link McpAppToolCallCompleteEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record McpAppToolCallCompleteEventData(
        /** Name of the MCP server hosting the tool */
        @JsonProperty("serverName") String serverName,
        /** MCP tool name that was invoked */
        @JsonProperty("toolName") String toolName,
        /** Arguments passed to the tool by the app view, if any */
        @JsonProperty("arguments") Map<String, Object> arguments,
        /** True when the call completed without throwing AND the MCP CallToolResult did not set isError */
        @JsonProperty("success") Boolean success,
        /** Wall-clock duration of the underlying tools/call in milliseconds */
        @JsonProperty("durationMs") Double durationMs,
        /** Standard MCP CallToolResult returned by the server. Present whether or not the call set isError. */
        @JsonProperty("result") Map<String, Object> result,
        /** Set when the underlying tools/call threw an error before returning a CallToolResult */
        @JsonProperty("error") McpAppToolCallCompleteError error,
        /** The tool's `_meta.ui` block at the time of the call, so consumers can decide whether to forward the result to the model without re-listing tools. */
        @JsonProperty("toolMeta") McpAppToolCallCompleteToolMeta toolMeta
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public McpAppToolCallCompleteEventData {}

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
            private String serverName;
            private String toolName;
            private Map<String, Object> arguments;
            private Boolean success;
            private Double durationMs;
            private Map<String, Object> result;
            private McpAppToolCallCompleteError error;
            private McpAppToolCallCompleteToolMeta toolMeta;

            /**
             * Sets the {@code serverName} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder serverName(String value) {
                this.serverName = value;
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
             * Sets the {@code arguments} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder arguments(Map<String, Object> value) {
                this.arguments = value;
                return this;
            }

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
             * Sets the {@code durationMs} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder durationMs(Double value) {
                this.durationMs = value;
                return this;
            }

            /**
             * Sets the {@code result} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder result(Map<String, Object> value) {
                this.result = value;
                return this;
            }

            /**
             * Sets the {@code error} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder error(McpAppToolCallCompleteError value) {
                this.error = value;
                return this;
            }

            /**
             * Sets the {@code toolMeta} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolMeta(McpAppToolCallCompleteToolMeta value) {
                this.toolMeta = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public McpAppToolCallCompleteEventData build() {
                if (serverName == null) throw new IllegalStateException("Missing required property: serverName");
                if (toolName == null) throw new IllegalStateException("Missing required property: toolName");
                if (success == null) throw new IllegalStateException("Missing required property: success");
                if (durationMs == null) throw new IllegalStateException("Missing required property: durationMs");
                return new McpAppToolCallCompleteEventData(serverName, toolName, arguments, success, durationMs, result, error, toolMeta);
            }
        }
    }
}
