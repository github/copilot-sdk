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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Canvas available in the current session.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = DiscoveredCanvas.$NullableDeserializer.class)
public record DiscoveredCanvas(
    /** Human-readable canvas name */
    @JsonProperty("displayName") String displayName,
    /** Short, single-sentence description shown to the agent in canvas catalogs. */
    @JsonProperty("description") String description,
    /** Host-local PNG path for the canvas icon, when supplied */
    @JsonProperty("icon") String icon,
    /** JSON Schema for canvas open input */
    @JsonProperty("inputSchema") Object inputSchema,
    /** Actions the agent or host may invoke on an open instance */
    @JsonProperty("actions") List<CanvasAction> actions,
    /** Owning provider identifier */
    @JsonProperty("extensionId") String extensionId,
    /** Owning extension display name, when available */
    @JsonProperty("extensionName") String extensionName,
    /** Provider-local canvas identifier */
    @JsonProperty("canvasId") String canvasId,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public DiscoveredCanvas { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public DiscoveredCanvas(String displayName, String description, String icon, Object inputSchema, List<CanvasAction> actions, String extensionId, String extensionName, String canvasId) {
        this(displayName, description, icon, inputSchema, actions, extensionId, extensionName, canvasId, java.util.Set.of());
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
        private String displayName;
        private String description;
        private String icon;
        private Object inputSchema;
        private boolean $presence3;
        private List<CanvasAction> actions;
        private String extensionId;
        private String extensionName;
        private String canvasId;

        /**
         * Sets the {@code displayName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder displayName(String value) {
            this.displayName = value;
            return this;
        }

        /**
         * Sets the {@code description} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder description(String value) {
            this.description = value;
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
         * Sets the {@code inputSchema} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder inputSchema(Object value) {
            this.inputSchema = value;
            this.$presence3 = true;
            return this;
        }

        /**
         * Sets the {@code actions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder actions(List<CanvasAction> value) {
            this.actions = value;
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
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public DiscoveredCanvas build() {
            if (displayName == null) throw new IllegalStateException("Missing required property: displayName");
            if (description == null) throw new IllegalStateException("Missing required property: description");
            if (extensionId == null) throw new IllegalStateException("Missing required property: extensionId");
            if (canvasId == null) throw new IllegalStateException("Missing required property: canvasId");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence3 && inputSchema == null) $explicitNulls.add("inputSchema");
            return new DiscoveredCanvas(displayName, description, icon, inputSchema, actions, extensionId, extensionName, canvasId, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("inputSchema") && inputSchema == null) values.put("inputSchema", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<DiscoveredCanvas> {
        @Override
        public DiscoveredCanvas deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(DiscoveredCanvas.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("inputSchema") && tree.get("inputSchema").isNull()) $explicitNulls.add("inputSchema");
            return new DiscoveredCanvas(readProperty(context, tree, "displayName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "description", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "icon", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "inputSchema", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "actions", new com.fasterxml.jackson.core.type.TypeReference<List<CanvasAction>>() {}), readProperty(context, tree, "extensionId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "extensionName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "canvasId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
