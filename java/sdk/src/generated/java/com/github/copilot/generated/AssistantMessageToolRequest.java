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
 * A tool invocation request from the assistant
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = AssistantMessageToolRequest.$NullableDeserializer.class)
public record AssistantMessageToolRequest(
    /** Unique identifier for this tool call */
    @JsonProperty("toolCallId") String toolCallId,
    /** Name of the tool being invoked */
    @JsonProperty("name") String name,
    /** Arguments to pass to the tool, format depends on the tool */
    @JsonProperty("arguments") Object arguments,
    /** Tool call type: "function" for standard tool calls, "custom" for grammar-based tool calls. Defaults to "function" when absent. */
    @JsonProperty("type") AssistantMessageToolRequestType type,
    /** Human-readable display title for the tool */
    @JsonProperty("toolTitle") String toolTitle,
    /** Configured key of the MCP server hosting this tool. Legacy events may contain the display name instead. */
    @JsonProperty("mcpServerName") String mcpServerName,
    /** Original tool name on the MCP server, when the tool is an MCP tool */
    @JsonProperty("mcpToolName") String mcpToolName,
    /** Resolved intention summary describing what this specific call does */
    @JsonProperty("intentionSummary") String intentionSummary,
    /** Hosted program that requested this client tool call */
    @JsonProperty("caller") AssistantMessageToolRequestCaller caller,
    /** Explicitly configured MCP server display name. */
    @JsonProperty("mcpServerDisplayName") String mcpServerDisplayName,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AssistantMessageToolRequest { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AssistantMessageToolRequest(String toolCallId, String name, Object arguments, AssistantMessageToolRequestType type, String toolTitle, String mcpServerName, String mcpToolName, String intentionSummary, AssistantMessageToolRequestCaller caller, String mcpServerDisplayName) {
        this(toolCallId, name, arguments, type, toolTitle, mcpServerName, mcpToolName, intentionSummary, caller, mcpServerDisplayName, java.util.Set.of());
    }

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
        private String name;
        private Object arguments;
        private boolean $presence2;
        private AssistantMessageToolRequestType type;
        private String toolTitle;
        private String mcpServerName;
        private String mcpToolName;
        private String intentionSummary;
        private boolean $presence7;
        private AssistantMessageToolRequestCaller caller;
        private String mcpServerDisplayName;

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
         * Sets the {@code name} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder name(String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the {@code arguments} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder arguments(Object value) {
            this.arguments = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Sets the {@code type} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder type(AssistantMessageToolRequestType value) {
            this.type = value;
            return this;
        }

        /**
         * Sets the {@code toolTitle} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder toolTitle(String value) {
            this.toolTitle = value;
            return this;
        }

        /**
         * Sets the {@code mcpServerName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mcpServerName(String value) {
            this.mcpServerName = value;
            return this;
        }

        /**
         * Sets the {@code mcpToolName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mcpToolName(String value) {
            this.mcpToolName = value;
            return this;
        }

        /**
         * Sets the {@code intentionSummary} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder intentionSummary(String value) {
            this.intentionSummary = value;
            this.$presence7 = true;
            return this;
        }

        /**
         * Sets the {@code caller} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder caller(AssistantMessageToolRequestCaller value) {
            this.caller = value;
            return this;
        }

        /**
         * Sets the {@code mcpServerDisplayName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mcpServerDisplayName(String value) {
            this.mcpServerDisplayName = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AssistantMessageToolRequest build() {
            if (toolCallId == null) throw new IllegalStateException("Missing required property: toolCallId");
            if (name == null) throw new IllegalStateException("Missing required property: name");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence2 && arguments == null) $explicitNulls.add("arguments");
            if ($presence7 && intentionSummary == null) $explicitNulls.add("intentionSummary");
            return new AssistantMessageToolRequest(toolCallId, name, arguments, type, toolTitle, mcpServerName, mcpToolName, intentionSummary, caller, mcpServerDisplayName, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("arguments") && arguments == null) values.put("arguments", null);
        if ($explicitNulls.contains("intentionSummary") && intentionSummary == null) values.put("intentionSummary", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<AssistantMessageToolRequest> {
        @Override
        public AssistantMessageToolRequest deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(AssistantMessageToolRequest.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("arguments") && tree.get("arguments").isNull()) $explicitNulls.add("arguments");
            if (tree.has("intentionSummary") && tree.get("intentionSummary").isNull()) $explicitNulls.add("intentionSummary");
            return new AssistantMessageToolRequest(readProperty(context, tree, "toolCallId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "name", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "arguments", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "type", new com.fasterxml.jackson.core.type.TypeReference<AssistantMessageToolRequestType>() {}), readProperty(context, tree, "toolTitle", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "mcpServerName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "mcpToolName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "intentionSummary", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "caller", new com.fasterxml.jackson.core.type.TypeReference<AssistantMessageToolRequestCaller>() {}), readProperty(context, tree, "mcpServerDisplayName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }

    /**
     * Creates a record with the components it had before later optional fields were added.
     * @deprecated Use named construction via {@link #builder()}.
     * @param toolCallId Unique identifier for this tool call
     * @param name Name of the tool being invoked
     * @param arguments Arguments to pass to the tool, format depends on the tool
     * @param type Tool call type: "function" for standard tool calls, "custom" for grammar-based tool calls. Defaults to "function" when absent.
     * @param toolTitle Human-readable display title for the tool
     * @param mcpServerName Configured key of the MCP server hosting this tool. Legacy events may contain the display name instead.
     * @param mcpToolName Original tool name on the MCP server, when the tool is an MCP tool
     * @param intentionSummary Resolved intention summary describing what this specific call does
     * @param caller Hosted program that requested this client tool call
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AssistantMessageToolRequest(
        String toolCallId,
        String name,
        Object arguments,
        AssistantMessageToolRequestType type,
        String toolTitle,
        String mcpServerName,
        String mcpToolName,
        String intentionSummary,
        AssistantMessageToolRequestCaller caller
    ) {
        this(toolCallId, name, arguments, type, toolTitle, mcpServerName, mcpToolName, intentionSummary, caller, null);
    }
}
