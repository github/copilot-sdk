/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Detected files and resource requirements.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = LocalModelMachineView.$NullableDeserializer.class)
public record LocalModelMachineView(
    /** Detected standalone llama-server executable, if available. */
    @JsonProperty("llamaServerPath") String llamaServerPath,
    /** Models directory under the connection's configuration root. */
    @JsonProperty("modelsDirectory") String modelsDirectory,
    /** Catalog models with their current local state. */
    @JsonProperty("rows") List<LocalModelMachineRow> rows,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public LocalModelMachineView { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public LocalModelMachineView(String llamaServerPath, String modelsDirectory, List<LocalModelMachineRow> rows) {
        this(llamaServerPath, modelsDirectory, rows, java.util.Set.of());
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
        private String llamaServerPath;
        private boolean $presence0;
        private String modelsDirectory;
        private List<LocalModelMachineRow> rows;

        /**
         * Sets the {@code llamaServerPath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder llamaServerPath(String value) {
            this.llamaServerPath = value;
            this.$presence0 = true;
            return this;
        }

        /**
         * Sets the {@code modelsDirectory} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder modelsDirectory(String value) {
            this.modelsDirectory = value;
            return this;
        }

        /**
         * Sets the {@code rows} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder rows(List<LocalModelMachineRow> value) {
            this.rows = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public LocalModelMachineView build() {
            if (modelsDirectory == null) throw new IllegalStateException("Missing required property: modelsDirectory");
            if (rows == null) throw new IllegalStateException("Missing required property: rows");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence0 && llamaServerPath == null) $explicitNulls.add("llamaServerPath");
            return new LocalModelMachineView(llamaServerPath, modelsDirectory, rows, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("llamaServerPath") && llamaServerPath == null) values.put("llamaServerPath", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<LocalModelMachineView> {
        @Override
        public LocalModelMachineView deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(LocalModelMachineView.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("llamaServerPath") && tree.get("llamaServerPath").isNull()) $explicitNulls.add("llamaServerPath");
            return new LocalModelMachineView(readProperty(context, tree, "llamaServerPath", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "modelsDirectory", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "rows", new com.fasterxml.jackson.core.type.TypeReference<List<LocalModelMachineRow>>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
