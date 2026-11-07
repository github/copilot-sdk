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
 * A single top-level setting subtree to validate and write.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = UserSettingsApplyTopKeyParams.$NullableDeserializer.class)
record UserSettingsApplyTopKeyParams(
    /** Canonical path of the setting the caller is editing. Currently informational only. */
    @JsonProperty("canonicalPath") List<String> canonicalPath,
    /** Top-level setting name to write. */
    @JsonProperty("topKey") String topKey,
    /** Value to write for the whole top-level key. Omitted when `delete` is true. Explicit null is preserved and validated as null. */
    @JsonProperty("value") Object value,
    /** True to remove the top-level key instead of writing it. */
    @JsonProperty("delete") Boolean delete,
    /** Records that the user chose an update channel explicitly, so the one-time staff channel migration stops reapplying. Honored only for the `autoUpdatesChannel` key. */
    @JsonProperty("completeStaffUpdateChannelMigration") Boolean completeStaffUpdateChannelMigration,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    UserSettingsApplyTopKeyParams { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    UserSettingsApplyTopKeyParams(List<String> canonicalPath, String topKey, Object value, Boolean delete, Boolean completeStaffUpdateChannelMigration) {
        this(canonicalPath, topKey, value, delete, completeStaffUpdateChannelMigration, java.util.Set.of());
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
        private List<String> canonicalPath;
        private String topKey;
        private Object value;
        private boolean $presence2;
        private Boolean delete;
        private Boolean completeStaffUpdateChannelMigration;

        /**
         * Sets the {@code canonicalPath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder canonicalPath(List<String> value) {
            this.canonicalPath = value;
            return this;
        }

        /**
         * Sets the {@code topKey} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder topKey(String value) {
            this.topKey = value;
            return this;
        }

        /**
         * Sets the {@code value} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder value(Object value) {
            this.value = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Sets the {@code delete} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder delete(Boolean value) {
            this.delete = value;
            return this;
        }

        /**
         * Sets the {@code completeStaffUpdateChannelMigration} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder completeStaffUpdateChannelMigration(Boolean value) {
            this.completeStaffUpdateChannelMigration = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public UserSettingsApplyTopKeyParams build() {
            if (canonicalPath == null) throw new IllegalStateException("Missing required property: canonicalPath");
            if (topKey == null) throw new IllegalStateException("Missing required property: topKey");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence2 && value == null) $explicitNulls.add("value");
            return new UserSettingsApplyTopKeyParams(canonicalPath, topKey, value, delete, completeStaffUpdateChannelMigration, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("value") && value == null) values.put("value", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<UserSettingsApplyTopKeyParams> {
        @Override
        public UserSettingsApplyTopKeyParams deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(UserSettingsApplyTopKeyParams.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("value") && tree.get("value").isNull()) $explicitNulls.add("value");
            return new UserSettingsApplyTopKeyParams(readProperty(context, tree, "canonicalPath", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "topKey", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "value", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "delete", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "completeStaffUpdateChannelMigration", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
