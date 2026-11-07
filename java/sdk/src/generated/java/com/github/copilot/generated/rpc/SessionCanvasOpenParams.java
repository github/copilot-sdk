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
import javax.annotation.processing.Generated;

/**
 * Canvas open parameters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SessionCanvasOpenParams.$NullableDeserializer.class)
public record SessionCanvasOpenParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Owning provider identifier. Optional when the canvasId is unique across providers; required to disambiguate when multiple providers register the same canvasId. */
    @JsonProperty("extensionId") String extensionId,
    /** Provider-local canvas identifier */
    @JsonProperty("canvasId") String canvasId,
    /** Caller-supplied stable instance identifier */
    @JsonProperty("instanceId") String instanceId,
    /** Canvas open input */
    @JsonProperty("input") Object input,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionCanvasOpenParams { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionCanvasOpenParams(String sessionId, String extensionId, String canvasId, String instanceId, Object input) {
        this(sessionId, extensionId, canvasId, instanceId, input, java.util.Set.of());
    }

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
        private String extensionId;
        private String canvasId;
        private String instanceId;
        private Object input;
        private boolean $presence4;

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
         * Sets the {@code extensionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder extensionId(String value) {
            this.extensionId = value;
            return this;
        }

        /**
         * Sets the {@code canvasId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder canvasId(String value) {
            this.canvasId = value;
            return this;
        }

        /**
         * Sets the {@code instanceId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder instanceId(String value) {
            this.instanceId = value;
            return this;
        }

        /**
         * Sets the {@code input} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder input(Object value) {
            this.input = value;
            this.$presence4 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionCanvasOpenParams build() {
            if (canvasId == null) throw new IllegalStateException("Missing required property: canvasId");
            if (instanceId == null) throw new IllegalStateException("Missing required property: instanceId");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence4 && input == null) $explicitNulls.add("input");
            return new SessionCanvasOpenParams(sessionId, extensionId, canvasId, instanceId, input, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("input") && input == null) values.put("input", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<SessionCanvasOpenParams> {
        @Override
        public SessionCanvasOpenParams deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(SessionCanvasOpenParams.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("input") && tree.get("input").isNull()) $explicitNulls.add("input");
            return new SessionCanvasOpenParams(readProperty(context, tree, "sessionId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "extensionId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "canvasId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "instanceId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "input", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
