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
 * The managed (enterprise policy) layer and the user layer to fold together. The field names carry the layer identity: nothing about a settings object identifies which layer produced it, so a generic base/override pair would let a caller transpose them and silently resolve the governance keys in the user's favour.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = UserSettingsMergeManagedAndUserParams.$NullableDeserializer.class)
record UserSettingsMergeManagedAndUserParams(
    /** The managed layer. Its governance keys win, and the per-entry `enabledPlugins` and `extraKnownMarketplaces` control maps are stamped back from it after the union. Omitted when the host is unmanaged. */
    @JsonProperty("managed") Object managed,
    /** The user's own settings layer. Wins on every key the managed layer does not govern. Omitted when the user has no settings yet. */
    @JsonProperty("user") Object user,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    UserSettingsMergeManagedAndUserParams { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    UserSettingsMergeManagedAndUserParams(Object managed, Object user) {
        this(managed, user, java.util.Set.of());
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
        private Object managed;
        private boolean $presence0;
        private Object user;
        private boolean $presence1;

        /**
         * Sets the {@code managed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder managed(Object value) {
            this.managed = value;
            this.$presence0 = true;
            return this;
        }

        /**
         * Sets the {@code user} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder user(Object value) {
            this.user = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public UserSettingsMergeManagedAndUserParams build() {
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence0 && managed == null) $explicitNulls.add("managed");
            if ($presence1 && user == null) $explicitNulls.add("user");
            return new UserSettingsMergeManagedAndUserParams(managed, user, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("managed") && managed == null) values.put("managed", null);
        if ($explicitNulls.contains("user") && user == null) values.put("user", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<UserSettingsMergeManagedAndUserParams> {
        @Override
        public UserSettingsMergeManagedAndUserParams deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(UserSettingsMergeManagedAndUserParams.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("managed") && tree.get("managed").isNull()) $explicitNulls.add("managed");
            if (tree.has("user") && tree.get("user").isNull()) $explicitNulls.add("user");
            return new UserSettingsMergeManagedAndUserParams(readProperty(context, tree, "managed", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "user", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
