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
 * Observable outcome of a scoped registration change.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = AzureMlRegistrationResult.$NullableDeserializer.class)
public record AzureMlRegistrationResult(
    /** Whether the change succeeded, was refused, or failed. */
    @JsonProperty("kind") AzureMlRegistrationKind kind,
    /** Effective providers file, including the configured environment override. */
    @JsonProperty("path") String path,
    /** Refusal reason or write error, absent after success. */
    @JsonProperty("detail") String detail,
    /** Prior managed rows captured under the write lock, for an upsert. */
    @JsonProperty("before") Object before,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AzureMlRegistrationResult { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AzureMlRegistrationResult(AzureMlRegistrationKind kind, String path, String detail, Object before) {
        this(kind, path, detail, before, java.util.Set.of());
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
        private AzureMlRegistrationKind kind;
        private String path;
        private String detail;
        private Object before;
        private boolean $presence3;

        /**
         * Sets the {@code kind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder kind(AzureMlRegistrationKind value) {
            this.kind = value;
            return this;
        }

        /**
         * Sets the {@code path} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder path(String value) {
            this.path = value;
            return this;
        }

        /**
         * Sets the {@code detail} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder detail(String value) {
            this.detail = value;
            return this;
        }

        /**
         * Sets the {@code before} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder before(Object value) {
            this.before = value;
            this.$presence3 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AzureMlRegistrationResult build() {
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            if (path == null) throw new IllegalStateException("Missing required property: path");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence3 && before == null) $explicitNulls.add("before");
            return new AzureMlRegistrationResult(kind, path, detail, before, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("before") && before == null) values.put("before", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<AzureMlRegistrationResult> {
        @Override
        public AzureMlRegistrationResult deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(AzureMlRegistrationResult.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("before") && tree.get("before").isNull()) $explicitNulls.add("before");
            return new AzureMlRegistrationResult(readProperty(context, tree, "kind", new com.fasterxml.jackson.core.type.TypeReference<AzureMlRegistrationKind>() {}), readProperty(context, tree, "path", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "detail", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "before", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
