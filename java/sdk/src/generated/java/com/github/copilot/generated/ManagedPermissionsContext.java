/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Reusable managed permission evaluation context. Treat permissions as runtime-owned policy data and retain it verbatim; it includes source-aware composition metadata. This is a snapshot, not a capability or approval token; only use contexts obtained from trusted policy sources.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ManagedPermissionsContext.$NullableDeserializer.class)
public record ManagedPermissionsContext(
    /** Runtime-owned composed permissions object. Absent when no permission policy is configured. */
    @JsonProperty("permissions") Object permissions,
    /** Policy could not be determined; evaluation must deny every operation. */
    @JsonProperty("failClosed") Boolean failClosed,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ManagedPermissionsContext { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ManagedPermissionsContext(Object permissions, Boolean failClosed) {
        this(permissions, failClosed, java.util.Set.of());
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
        private Object permissions;
        private boolean $presence0;
        private Boolean failClosed;

        /**
         * Sets the {@code permissions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder permissions(Object value) {
            this.permissions = value;
            this.$presence0 = true;
            return this;
        }

        /**
         * Sets the {@code failClosed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder failClosed(Boolean value) {
            this.failClosed = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ManagedPermissionsContext build() {
            if (failClosed == null) throw new IllegalStateException("Missing required property: failClosed");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence0 && permissions == null) $explicitNulls.add("permissions");
            return new ManagedPermissionsContext(permissions, failClosed, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("permissions") && permissions == null) values.put("permissions", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<ManagedPermissionsContext> {
        @Override
        public ManagedPermissionsContext deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(ManagedPermissionsContext.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("permissions") && tree.get("permissions").isNull()) $explicitNulls.add("permissions");
            return new ManagedPermissionsContext(readProperty(context, tree, "permissions", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "failClosed", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
