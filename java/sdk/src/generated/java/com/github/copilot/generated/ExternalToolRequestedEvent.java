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
 * Session event "external_tool.requested". External tool invocation request for client-side tool execution
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ExternalToolRequestedEvent extends SessionEvent {

    @Override
    public String getType() { return "external_tool.requested"; }

    @JsonProperty("data")
    private ExternalToolRequestedEventData data;

    public ExternalToolRequestedEventData getData() { return data; }
    public void setData(ExternalToolRequestedEventData data) { this.data = data; }

    /** Data payload for {@link ExternalToolRequestedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ExternalToolRequestedEventData.$NullableDeserializer.class)
    public record ExternalToolRequestedEventData(
        /** Unique identifier for this request; used to respond via session.respondToExternalTool() */
        @JsonProperty("requestId") String requestId,
        /** Session ID that this external tool request belongs to */
        @JsonProperty("sessionId") String sessionId,
        /** Tool call ID assigned to this external tool invocation */
        @JsonProperty("toolCallId") String toolCallId,
        /** Name of the external tool to invoke */
        @JsonProperty("toolName") String toolName,
        /** Stable provider identity captured with an extension-owned tool definition; hosts use it to route the request to the same provider that was offered to the model */
        @JsonProperty("providerId") String providerId,
        /** Arguments to pass to the external tool */
        @JsonProperty("arguments") Object arguments,
        /** Active session working directory, when known. */
        @JsonProperty("workingDirectory") String workingDirectory,
        /** W3C Trace Context traceparent header for the execute_tool span */
        @JsonProperty("traceparent") String traceparent,
        /** W3C Trace Context tracestate header for the execute_tool span */
        @JsonProperty("tracestate") String tracestate,
        @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public ExternalToolRequestedEventData { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

        /**
         * Creates a record with legacy null-as-omission semantics.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public ExternalToolRequestedEventData(String requestId, String sessionId, String toolCallId, String toolName, String providerId, Object arguments, String workingDirectory, String traceparent, String tracestate) {
            this(requestId, sessionId, toolCallId, toolName, providerId, arguments, workingDirectory, traceparent, tracestate, java.util.Set.of());
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
            private String requestId;
            private String sessionId;
            private String toolCallId;
            private String toolName;
            private String providerId;
            private boolean $presence4;
            private Object arguments;
            private boolean $presence5;
            private String workingDirectory;
            private String traceparent;
            private String tracestate;

            /**
             * Sets the {@code requestId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder requestId(String value) {
                this.requestId = value;
                return this;
            }

            /**
             * Sets the {@code sessionId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder sessionId(String value) {
                this.sessionId = value;
                return this;
            }

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
             * Sets the {@code providerId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder providerId(String value) {
                this.providerId = value;
                this.$presence4 = true;
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
                this.$presence5 = true;
                return this;
            }

            /**
             * Sets the {@code workingDirectory} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder workingDirectory(String value) {
                this.workingDirectory = value;
                return this;
            }

            /**
             * Sets the {@code traceparent} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder traceparent(String value) {
                this.traceparent = value;
                return this;
            }

            /**
             * Sets the {@code tracestate} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder tracestate(String value) {
                this.tracestate = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public ExternalToolRequestedEventData build() {
                if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
                if (sessionId == null) throw new IllegalStateException("Missing required property: sessionId");
                if (toolCallId == null) throw new IllegalStateException("Missing required property: toolCallId");
                if (toolName == null) throw new IllegalStateException("Missing required property: toolName");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if ($presence4 && providerId == null) $explicitNulls.add("providerId");
                if ($presence5 && arguments == null) $explicitNulls.add("arguments");
                return new ExternalToolRequestedEventData(requestId, sessionId, toolCallId, toolName, providerId, arguments, workingDirectory, traceparent, tracestate, $explicitNulls);
            }
        }

        @com.fasterxml.jackson.annotation.JsonAnyGetter
        @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
        public java.util.Map<String, Object> $nullableProperties() {
            java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
            if ($explicitNulls.contains("providerId") && providerId == null) values.put("providerId", null);
            if ($explicitNulls.contains("arguments") && arguments == null) values.put("arguments", null);
            return values;
        }

        public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<ExternalToolRequestedEventData> {
            @Override
            public ExternalToolRequestedEventData deserialize(com.fasterxml.jackson.core.JsonParser parser,
                    com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
                com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
                if (!tree.isObject()) return context.reportInputMismatch(ExternalToolRequestedEventData.class, "Expected a JSON object");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if (tree.has("providerId") && tree.get("providerId").isNull()) $explicitNulls.add("providerId");
                if (tree.has("arguments") && tree.get("arguments").isNull()) $explicitNulls.add("arguments");
                return new ExternalToolRequestedEventData(readProperty(context, tree, "requestId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "sessionId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "toolCallId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "toolName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "providerId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "arguments", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "workingDirectory", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "traceparent", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "tracestate", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
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
