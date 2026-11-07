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
 * The credential and environment to evaluate the device-channel sign-in pin against.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ManagedSettingsResolveDeviceForceLoginOrgsGateParams.$NullableDeserializer.class)
record ManagedSettingsResolveDeviceForceLoginOrgsGateParams(
    /** The authenticated identity to judge. Omitted when signed out, which is the case the device channel exists to serve: it can refuse a first sign-in before any credential exists. */
    @JsonProperty("authInfo") Object authInfo,
    /** The host's environment variables. Read for the variables that would make an identity's membership snapshot forgeable, so a credential whose provenance cannot be trusted is refused rather than accepted. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("environment") Object environment,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    ManagedSettingsResolveDeviceForceLoginOrgsGateParams { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    ManagedSettingsResolveDeviceForceLoginOrgsGateParams(Object authInfo, Object environment) {
        this(authInfo, environment, java.util.Set.of());
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
        private Object authInfo;
        private boolean $presence0;
        private Object environment;
        private boolean $presence1;

        /**
         * Sets the {@code authInfo} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder authInfo(Object value) {
            this.authInfo = value;
            this.$presence0 = true;
            return this;
        }

        /**
         * Sets the {@code environment} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder environment(Object value) {
            this.environment = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ManagedSettingsResolveDeviceForceLoginOrgsGateParams build() {
            if (!$presence1) throw new IllegalStateException("Missing required property: environment");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence0 && authInfo == null) $explicitNulls.add("authInfo");
            return new ManagedSettingsResolveDeviceForceLoginOrgsGateParams(authInfo, environment, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("authInfo") && authInfo == null) values.put("authInfo", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<ManagedSettingsResolveDeviceForceLoginOrgsGateParams> {
        @Override
        public ManagedSettingsResolveDeviceForceLoginOrgsGateParams deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(ManagedSettingsResolveDeviceForceLoginOrgsGateParams.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("authInfo") && tree.get("authInfo").isNull()) $explicitNulls.add("authInfo");
            return new ManagedSettingsResolveDeviceForceLoginOrgsGateParams(readProperty(context, tree, "authInfo", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "environment", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
