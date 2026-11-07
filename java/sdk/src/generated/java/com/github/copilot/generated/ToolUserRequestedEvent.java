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
 * Session event "tool.user_requested". User-initiated tool invocation request with tool name and arguments
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ToolUserRequestedEvent extends SessionEvent {

    @Override
    public String getType() { return "tool.user_requested"; }

    @JsonProperty("data")
    private ToolUserRequestedEventData data;

    public ToolUserRequestedEventData getData() { return data; }
    public void setData(ToolUserRequestedEventData data) { this.data = data; }

    /** Data payload for {@link ToolUserRequestedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ToolUserRequestedEventData.$NullableDeserializer.class)
    public record ToolUserRequestedEventData(
        /** Unique identifier for this tool call */
        @JsonProperty("toolCallId") String toolCallId,
        /** Name of the tool the user wants to invoke */
        @JsonProperty("toolName") String toolName,
        /** Arguments for the tool invocation */
        @JsonProperty("arguments") Object arguments,
        @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public ToolUserRequestedEventData { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

        /**
         * Creates a record with legacy null-as-omission semantics.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public ToolUserRequestedEventData(String toolCallId, String toolName, Object arguments) {
            this(toolCallId, toolName, arguments, java.util.Set.of());
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
            private String toolName;
            private Object arguments;
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
            public Builder arguments(Object value) {
                this.arguments = value;
                this.$presence2 = true;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public ToolUserRequestedEventData build() {
                if (toolCallId == null) throw new IllegalStateException("Missing required property: toolCallId");
                if (toolName == null) throw new IllegalStateException("Missing required property: toolName");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if ($presence2 && arguments == null) $explicitNulls.add("arguments");
                return new ToolUserRequestedEventData(toolCallId, toolName, arguments, $explicitNulls);
            }
        }

        @com.fasterxml.jackson.annotation.JsonAnyGetter
        @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
        public java.util.Map<String, Object> $nullableProperties() {
            java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
            if ($explicitNulls.contains("arguments") && arguments == null) values.put("arguments", null);
            return values;
        }

        public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<ToolUserRequestedEventData> {
            @Override
            public ToolUserRequestedEventData deserialize(com.fasterxml.jackson.core.JsonParser parser,
                    com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
                com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
                if (!tree.isObject()) return context.reportInputMismatch(ToolUserRequestedEventData.class, "Expected a JSON object");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if (tree.has("arguments") && tree.get("arguments").isNull()) $explicitNulls.add("arguments");
                return new ToolUserRequestedEventData(readProperty(context, tree, "toolCallId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "toolName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "arguments", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), $explicitNulls);
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
