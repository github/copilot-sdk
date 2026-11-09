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
 * Current transfer progress.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = LocalModelDownloadProgress.$NullableDeserializer.class)
public record LocalModelDownloadProgress(
    /** Bytes received so far. */
    @JsonProperty("receivedBytes") Double receivedBytes,
    /** Total transfer bytes, when known. */
    @JsonProperty("totalBytes") Double totalBytes,
    /** Inference engine installation, when applicable. */
    @JsonProperty("stage") String stage,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public LocalModelDownloadProgress { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public LocalModelDownloadProgress(Double receivedBytes, Double totalBytes, String stage) {
        this(receivedBytes, totalBytes, stage, java.util.Set.of());
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
        private Double receivedBytes;
        private Double totalBytes;
        private boolean $presence1;
        private String stage;
        private boolean $presence2;

        /**
         * Sets the {@code receivedBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder receivedBytes(Double value) {
            this.receivedBytes = value;
            return this;
        }

        /**
         * Sets the {@code totalBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder totalBytes(Double value) {
            this.totalBytes = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Sets the {@code stage} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder stage(String value) {
            this.stage = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public LocalModelDownloadProgress build() {
            if (receivedBytes == null) throw new IllegalStateException("Missing required property: receivedBytes");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence1 && totalBytes == null) $explicitNulls.add("totalBytes");
            if ($presence2 && stage == null) $explicitNulls.add("stage");
            return new LocalModelDownloadProgress(receivedBytes, totalBytes, stage, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("totalBytes") && totalBytes == null) values.put("totalBytes", null);
        if ($explicitNulls.contains("stage") && stage == null) values.put("stage", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<LocalModelDownloadProgress> {
        @Override
        public LocalModelDownloadProgress deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(LocalModelDownloadProgress.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("totalBytes") && tree.get("totalBytes").isNull()) $explicitNulls.add("totalBytes");
            if (tree.has("stage") && tree.get("stage").isNull()) $explicitNulls.add("stage");
            return new LocalModelDownloadProgress(readProperty(context, tree, "receivedBytes", new com.fasterxml.jackson.core.type.TypeReference<Double>() {}), readProperty(context, tree, "totalBytes", new com.fasterxml.jackson.core.type.TypeReference<Double>() {}), readProperty(context, tree, "stage", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
