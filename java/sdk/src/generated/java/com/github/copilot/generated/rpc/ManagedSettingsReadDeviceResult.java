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
 * The device-managed settings layer, with the sandbox floor and the load outcome a host needs to compose it into an effective policy.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ManagedSettingsReadDeviceResult.$NullableDeserializer.class)
record ManagedSettingsReadDeviceResult(
    /** Validated, canonical device-managed settings. Omitted when no device policy was discovered, when the discovered document failed validation, or when it validated to an empty object — an empty document carries no policy, and reporting it as present would outrank layers that do. */
    @JsonProperty("settings") Object settings,
    /** Sandbox floor read from the discovered document before validation, so a document that is otherwise rejected can still raise the floor it declared. */
    @JsonProperty("sandboxFloor") Object sandboxFloor,
    /** Whether discovery or validation failed. A host must treat this as "policy unknown" rather than "no policy": absent settings with this set false mean the device genuinely has none. */
    @JsonProperty("loadFailed") Boolean loadFailed,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    ManagedSettingsReadDeviceResult { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    ManagedSettingsReadDeviceResult(Object settings, Object sandboxFloor, Boolean loadFailed) {
        this(settings, sandboxFloor, loadFailed, java.util.Set.of());
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
        private Object settings;
        private boolean $presence0;
        private Object sandboxFloor;
        private boolean $presence1;
        private Boolean loadFailed;

        /**
         * Sets the {@code settings} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder settings(Object value) {
            this.settings = value;
            this.$presence0 = true;
            return this;
        }

        /**
         * Sets the {@code sandboxFloor} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sandboxFloor(Object value) {
            this.sandboxFloor = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Sets the {@code loadFailed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder loadFailed(Boolean value) {
            this.loadFailed = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ManagedSettingsReadDeviceResult build() {
            if (loadFailed == null) throw new IllegalStateException("Missing required property: loadFailed");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence0 && settings == null) $explicitNulls.add("settings");
            if ($presence1 && sandboxFloor == null) $explicitNulls.add("sandboxFloor");
            return new ManagedSettingsReadDeviceResult(settings, sandboxFloor, loadFailed, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("settings") && settings == null) values.put("settings", null);
        if ($explicitNulls.contains("sandboxFloor") && sandboxFloor == null) values.put("sandboxFloor", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<ManagedSettingsReadDeviceResult> {
        @Override
        public ManagedSettingsReadDeviceResult deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(ManagedSettingsReadDeviceResult.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("settings") && tree.get("settings").isNull()) $explicitNulls.add("settings");
            if (tree.has("sandboxFloor") && tree.get("sandboxFloor").isNull()) $explicitNulls.add("sandboxFloor");
            return new ManagedSettingsReadDeviceResult(readProperty(context, tree, "settings", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "sandboxFloor", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "loadFailed", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
