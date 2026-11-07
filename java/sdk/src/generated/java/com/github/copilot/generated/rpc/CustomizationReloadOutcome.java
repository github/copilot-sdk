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
 * Result of one customization reload component.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = CustomizationReloadOutcome.$NullableDeserializer.class)
public record CustomizationReloadOutcome(
    /** Component whose reload was attempted or skipped */
    @JsonProperty("subsystem") CustomizationReloadSubsystem subsystem,
    /** Whether the component reloaded, was skipped, or failed */
    @JsonProperty("status") CustomizationReloadStatus status,
    /** Reason for a skipped component or description of a failure, when available */
    @JsonProperty("detail") String detail,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CustomizationReloadOutcome { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CustomizationReloadOutcome(CustomizationReloadSubsystem subsystem, CustomizationReloadStatus status, String detail) {
        this(subsystem, status, detail, java.util.Set.of());
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
        private CustomizationReloadSubsystem subsystem;
        private CustomizationReloadStatus status;
        private String detail;
        private boolean $presence2;

        /**
         * Sets the {@code subsystem} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder subsystem(CustomizationReloadSubsystem value) {
            this.subsystem = value;
            return this;
        }

        /**
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder status(CustomizationReloadStatus value) {
            this.status = value;
            return this;
        }

        /**
         * Sets the {@code detail} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder detail(String value) {
            this.detail = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CustomizationReloadOutcome build() {
            if (subsystem == null) throw new IllegalStateException("Missing required property: subsystem");
            if (status == null) throw new IllegalStateException("Missing required property: status");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence2 && detail == null) $explicitNulls.add("detail");
            return new CustomizationReloadOutcome(subsystem, status, detail, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("detail") && detail == null) values.put("detail", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<CustomizationReloadOutcome> {
        @Override
        public CustomizationReloadOutcome deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(CustomizationReloadOutcome.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("detail") && tree.get("detail").isNull()) $explicitNulls.add("detail");
            return new CustomizationReloadOutcome(readProperty(context, tree, "subsystem", new com.fasterxml.jackson.core.type.TypeReference<CustomizationReloadSubsystem>() {}), readProperty(context, tree, "status", new com.fasterxml.jackson.core.type.TypeReference<CustomizationReloadStatus>() {}), readProperty(context, tree, "detail", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
