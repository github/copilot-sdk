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
 * A complete JSON snapshot to write under the runtime's per-path file lock.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = PersistenceWriteRawJsonLockedParams.$NullableDeserializer.class)
record PersistenceWriteRawJsonLockedParams(
    /** Absolute path of the file to replace. Parent directories are created when missing. */
    @JsonProperty("path") String path,
    /** The serialized JSON document to write. It replaces the file's contents entirely; nothing is merged. */
    @JsonProperty("dataJson") String dataJson,
    /** Comment lines to emit above the document, for a format that tolerates them. Omit for strict JSON. */
    @JsonProperty("fileHeader") String fileHeader,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    PersistenceWriteRawJsonLockedParams { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    PersistenceWriteRawJsonLockedParams(String path, String dataJson, String fileHeader) {
        this(path, dataJson, fileHeader, java.util.Set.of());
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
        private String path;
        private String dataJson;
        private String fileHeader;
        private boolean $presence2;

        /**
         * Sets the {@code path} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder path(String value) {
            this.path = value;
            return this;
        }

        /**
         * Sets the {@code dataJson} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder dataJson(String value) {
            this.dataJson = value;
            return this;
        }

        /**
         * Sets the {@code fileHeader} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder fileHeader(String value) {
            this.fileHeader = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public PersistenceWriteRawJsonLockedParams build() {
            if (path == null) throw new IllegalStateException("Missing required property: path");
            if (dataJson == null) throw new IllegalStateException("Missing required property: dataJson");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence2 && fileHeader == null) $explicitNulls.add("fileHeader");
            return new PersistenceWriteRawJsonLockedParams(path, dataJson, fileHeader, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("fileHeader") && fileHeader == null) values.put("fileHeader", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<PersistenceWriteRawJsonLockedParams> {
        @Override
        public PersistenceWriteRawJsonLockedParams deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(PersistenceWriteRawJsonLockedParams.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("fileHeader") && tree.get("fileHeader").isNull()) $explicitNulls.add("fileHeader");
            return new PersistenceWriteRawJsonLockedParams(readProperty(context, tree, "path", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "dataJson", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "fileHeader", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
