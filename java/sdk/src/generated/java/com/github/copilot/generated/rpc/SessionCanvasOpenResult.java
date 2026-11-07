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
 * Open canvas instance snapshot.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SessionCanvasOpenResult.$NullableDeserializer.class)
public record SessionCanvasOpenResult(
    /** Stable caller-supplied canvas instance identifier */
    @JsonProperty("instanceId") String instanceId,
    /** Owning provider identifier */
    @JsonProperty("extensionId") String extensionId,
    /** Owning extension display name, when available */
    @JsonProperty("extensionName") String extensionName,
    /** Provider-local canvas identifier */
    @JsonProperty("canvasId") String canvasId,
    /** Host-local PNG path for the canvas icon, when supplied */
    @JsonProperty("icon") String icon,
    /** Rendered title */
    @JsonProperty("title") String title,
    /** Provider-supplied status text */
    @JsonProperty("status") String status,
    /** URL for web-rendered canvases */
    @JsonProperty("url") String url,
    /** Input supplied when the instance was opened */
    @JsonProperty("input") Object input,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionCanvasOpenResult { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionCanvasOpenResult(String instanceId, String extensionId, String extensionName, String canvasId, String icon, String title, String status, String url, Object input) {
        this(instanceId, extensionId, extensionName, canvasId, icon, title, status, url, input, java.util.Set.of());
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
        private String instanceId;
        private String extensionId;
        private String extensionName;
        private String canvasId;
        private String icon;
        private String title;
        private String status;
        private String url;
        private Object input;
        private boolean $presence8;

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
         * Sets the {@code extensionName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder extensionName(String value) {
            this.extensionName = value;
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
         * Sets the {@code icon} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder icon(String value) {
            this.icon = value;
            return this;
        }

        /**
         * Sets the {@code title} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder title(String value) {
            this.title = value;
            return this;
        }

        /**
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder status(String value) {
            this.status = value;
            return this;
        }

        /**
         * Sets the {@code url} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder url(String value) {
            this.url = value;
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
            this.$presence8 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionCanvasOpenResult build() {
            if (instanceId == null) throw new IllegalStateException("Missing required property: instanceId");
            if (extensionId == null) throw new IllegalStateException("Missing required property: extensionId");
            if (canvasId == null) throw new IllegalStateException("Missing required property: canvasId");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence8 && input == null) $explicitNulls.add("input");
            return new SessionCanvasOpenResult(instanceId, extensionId, extensionName, canvasId, icon, title, status, url, input, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("input") && input == null) values.put("input", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<SessionCanvasOpenResult> {
        @Override
        public SessionCanvasOpenResult deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(SessionCanvasOpenResult.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("input") && tree.get("input").isNull()) $explicitNulls.add("input");
            return new SessionCanvasOpenResult(readProperty(context, tree, "instanceId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "extensionId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "extensionName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "canvasId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "icon", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "title", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "status", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "url", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "input", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
