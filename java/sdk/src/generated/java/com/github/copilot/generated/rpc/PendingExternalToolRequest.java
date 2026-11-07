/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * External tool call of the session or one of its sub-agents that is still waiting for session.tools.handlePendingToolCall.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = PendingExternalToolRequest.$NullableDeserializer.class)
public record PendingExternalToolRequest(
    /** Request ID to pass to session.tools.handlePendingToolCall */
    @JsonProperty("requestId") String requestId,
    /** Tool call ID assigned to this external tool invocation */
    @JsonProperty("toolCallId") String toolCallId,
    /** Name of the external tool to invoke */
    @JsonProperty("toolName") String toolName,
    /** Arguments to pass to the external tool */
    @JsonProperty("arguments") Object arguments,
    /** Stable identity of the provider that offered the tool, for hosts that route extension-owned tools by provider */
    @JsonProperty("providerId") String providerId,
    /** Sub-agent instance identifier (the envelope agentId of its events) of the agent that issued the call; absent for calls issued by the root agent */
    @JsonProperty("agentId") String agentId,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public PendingExternalToolRequest { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public PendingExternalToolRequest(String requestId, String toolCallId, String toolName, Object arguments, String providerId, String agentId) {
        this(requestId, toolCallId, toolName, arguments, providerId, agentId, java.util.Set.of());
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
        private String toolCallId;
        private String toolName;
        private Object arguments;
        private boolean $presence3;
        private String providerId;
        private String agentId;

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
            this.$presence3 = true;
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
            return this;
        }

        /**
         * Sets the {@code agentId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder agentId(String value) {
            this.agentId = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public PendingExternalToolRequest build() {
            if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
            if (toolCallId == null) throw new IllegalStateException("Missing required property: toolCallId");
            if (toolName == null) throw new IllegalStateException("Missing required property: toolName");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence3 && arguments == null) $explicitNulls.add("arguments");
            return new PendingExternalToolRequest(requestId, toolCallId, toolName, arguments, providerId, agentId, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("arguments") && arguments == null) values.put("arguments", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<PendingExternalToolRequest> {
        @Override
        public PendingExternalToolRequest deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(PendingExternalToolRequest.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("arguments") && tree.get("arguments").isNull()) $explicitNulls.add("arguments");
            return new PendingExternalToolRequest(readProperty(context, tree, "requestId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "toolCallId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "toolName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "arguments", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "providerId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "agentId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
