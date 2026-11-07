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
 * The outcome of a locked snapshot write.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = PersistenceWriteRawJsonLockedResult.$NullableDeserializer.class)
record PersistenceWriteRawJsonLockedResult(
    /** Whether the file now holds the requested contents. */
    @JsonProperty("success") Boolean success,
    /** Why the write failed. Absent when `success` is true. A lock the runtime could not acquire in time reports here rather than raising. */
    @JsonProperty("error") String error,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    PersistenceWriteRawJsonLockedResult { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    PersistenceWriteRawJsonLockedResult(Boolean success, String error) {
        this(success, error, java.util.Set.of());
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
        private Boolean success;
        private String error;
        private boolean $presence1;

        /**
         * Sets the {@code success} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder success(Boolean value) {
            this.success = value;
            return this;
        }

        /**
         * Sets the {@code error} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder error(String value) {
            this.error = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public PersistenceWriteRawJsonLockedResult build() {
            if (success == null) throw new IllegalStateException("Missing required property: success");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence1 && error == null) $explicitNulls.add("error");
            return new PersistenceWriteRawJsonLockedResult(success, error, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("error") && error == null) values.put("error", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<PersistenceWriteRawJsonLockedResult> {
        @Override
        public PersistenceWriteRawJsonLockedResult deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(PersistenceWriteRawJsonLockedResult.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("error") && tree.get("error").isNull()) $explicitNulls.add("error");
            return new PersistenceWriteRawJsonLockedResult(readProperty(context, tree, "success", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "error", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
