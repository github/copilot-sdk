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
 * Session event "tool.execution_progress". Tool execution progress notification with status message
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ToolExecutionProgressEvent extends SessionEvent {

    @Override
    public String getType() { return "tool.execution_progress"; }

    @JsonProperty("data")
    private ToolExecutionProgressEventData data;

    public ToolExecutionProgressEventData getData() { return data; }
    public void setData(ToolExecutionProgressEventData data) { this.data = data; }

    /** Data payload for {@link ToolExecutionProgressEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ToolExecutionProgressEventData.$NullableDeserializer.class)
    public record ToolExecutionProgressEventData(
        /** Tool call ID this progress notification belongs to */
        @JsonProperty("toolCallId") String toolCallId,
        /** Human-readable progress status message (e.g., from an MCP server) */
        @JsonProperty("progressMessage") String progressMessage,
        /** Client-only structured progress metadata. Not model-facing tool output. */
        @CopilotExperimental
        @JsonProperty("structuredContent") Object structuredContent,
        @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public ToolExecutionProgressEventData { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

        /**
         * Creates a record with legacy null-as-omission semantics.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public ToolExecutionProgressEventData(String toolCallId, String progressMessage, Object structuredContent) {
            this(toolCallId, progressMessage, structuredContent, java.util.Set.of());
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
            private String progressMessage;
            private Object structuredContent;
            private boolean $presence2;

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
             * Sets the {@code progressMessage} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder progressMessage(String value) {
                this.progressMessage = value;
                return this;
            }

            /**
             * Sets the {@code structuredContent} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder structuredContent(Object value) {
                this.structuredContent = value;
                this.$presence2 = true;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public ToolExecutionProgressEventData build() {
                if (toolCallId == null) throw new IllegalStateException("Missing required property: toolCallId");
                if (progressMessage == null) throw new IllegalStateException("Missing required property: progressMessage");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if ($presence2 && structuredContent == null) $explicitNulls.add("structuredContent");
                return new ToolExecutionProgressEventData(toolCallId, progressMessage, structuredContent, $explicitNulls);
            }
        }

        @com.fasterxml.jackson.annotation.JsonAnyGetter
        @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
        public java.util.Map<String, Object> $nullableProperties() {
            java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
            if ($explicitNulls.contains("structuredContent") && structuredContent == null) values.put("structuredContent", null);
            return values;
        }

        public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<ToolExecutionProgressEventData> {
            @Override
            public ToolExecutionProgressEventData deserialize(com.fasterxml.jackson.core.JsonParser parser,
                    com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
                com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
                if (!tree.isObject()) return context.reportInputMismatch(ToolExecutionProgressEventData.class, "Expected a JSON object");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if (tree.has("structuredContent") && tree.get("structuredContent").isNull()) $explicitNulls.add("structuredContent");
                return new ToolExecutionProgressEventData(readProperty(context, tree, "toolCallId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "progressMessage", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "structuredContent", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), $explicitNulls);
            }

            private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                    com.fasterxml.jackson.databind.JsonNode tree, String name,
                    com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
                if (!tree.has(name)) return null;
                return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
            }
        }
    }
}
