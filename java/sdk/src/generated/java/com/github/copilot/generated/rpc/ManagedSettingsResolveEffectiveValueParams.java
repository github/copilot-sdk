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
 * The managed-settings layers to compose into an effective policy. Every layer is optional; omitting all of them yields no effective policy.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ManagedSettingsResolveEffectiveValueParams.$NullableDeserializer.class)
record ManagedSettingsResolveEffectiveValueParams(
    /** The account's server-delivered org policy, as returned by a managed-settings fetch. Omitted before sign-in or when the fetch has not landed. */
    @JsonProperty("server") Object server,
    /** The device layer: native MDM and managed-file settings describing the machine rather than the account. */
    @JsonProperty("mdm") Object mdm,
    /** The device sandbox floor, folded into the device layer under `sandbox` before composition. */
    @JsonProperty("deviceSandboxFloor") Object deviceSandboxFloor,
    /** The policy-helper layer, an enforced channel sourced from a platform policy helper process. */
    @JsonProperty("policyHelper") Object policyHelper,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    ManagedSettingsResolveEffectiveValueParams { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    ManagedSettingsResolveEffectiveValueParams(Object server, Object mdm, Object deviceSandboxFloor, Object policyHelper) {
        this(server, mdm, deviceSandboxFloor, policyHelper, java.util.Set.of());
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
        private Object server;
        private boolean $presence0;
        private Object mdm;
        private boolean $presence1;
        private Object deviceSandboxFloor;
        private boolean $presence2;
        private Object policyHelper;
        private boolean $presence3;

        /**
         * Sets the {@code server} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder server(Object value) {
            this.server = value;
            this.$presence0 = true;
            return this;
        }

        /**
         * Sets the {@code mdm} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mdm(Object value) {
            this.mdm = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Sets the {@code deviceSandboxFloor} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder deviceSandboxFloor(Object value) {
            this.deviceSandboxFloor = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Sets the {@code policyHelper} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder policyHelper(Object value) {
            this.policyHelper = value;
            this.$presence3 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ManagedSettingsResolveEffectiveValueParams build() {
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence0 && server == null) $explicitNulls.add("server");
            if ($presence1 && mdm == null) $explicitNulls.add("mdm");
            if ($presence2 && deviceSandboxFloor == null) $explicitNulls.add("deviceSandboxFloor");
            if ($presence3 && policyHelper == null) $explicitNulls.add("policyHelper");
            return new ManagedSettingsResolveEffectiveValueParams(server, mdm, deviceSandboxFloor, policyHelper, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("server") && server == null) values.put("server", null);
        if ($explicitNulls.contains("mdm") && mdm == null) values.put("mdm", null);
        if ($explicitNulls.contains("deviceSandboxFloor") && deviceSandboxFloor == null) values.put("deviceSandboxFloor", null);
        if ($explicitNulls.contains("policyHelper") && policyHelper == null) values.put("policyHelper", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<ManagedSettingsResolveEffectiveValueParams> {
        @Override
        public ManagedSettingsResolveEffectiveValueParams deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(ManagedSettingsResolveEffectiveValueParams.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("server") && tree.get("server").isNull()) $explicitNulls.add("server");
            if (tree.has("mdm") && tree.get("mdm").isNull()) $explicitNulls.add("mdm");
            if (tree.has("deviceSandboxFloor") && tree.get("deviceSandboxFloor").isNull()) $explicitNulls.add("deviceSandboxFloor");
            if (tree.has("policyHelper") && tree.get("policyHelper").isNull()) $explicitNulls.add("policyHelper");
            return new ManagedSettingsResolveEffectiveValueParams(readProperty(context, tree, "server", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "mdm", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "deviceSandboxFloor", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "policyHelper", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
