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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Caller population and overrides for static feature-flag resolution, independent of experiment assignments and active runtime state.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = FeatureFlagsResolveParams.$NullableDeserializer.class)
record FeatureFlagsResolveParams(
    /** Whether the caller is GitHub or Microsoft staff. */
    @JsonProperty("isStaff") Boolean isStaff,
    /** Whether the caller is in an experimental population. */
    @JsonProperty("isExperimental") Boolean isExperimental,
    /** Whether the caller belongs to the team population used by feature availability. */
    @JsonProperty("isTeam") Boolean isTeam,
    /** GitHub-staff membership. Omit when it was never recorded: a legacy staff account then counts as both staff populations and keeps every staff feature. */
    @JsonProperty("isStaffGithub") Boolean isStaffGitHub,
    /** Microsoft-staff membership. Omitted the same way as `isStaffGitHub`. */
    @JsonProperty("isStaffMicrosoft") Boolean isStaffMicrosoft,
    /** Resolve against this already-computed flag set rather than each flag's declared availability. Keyed by flag name. */
    @JsonProperty("baseFlags") Map<String, Boolean> baseFlags,
    /** Environment for direct feature-flag overrides and `COPILOT_CLI_ENABLED_FEATURE_FLAGS`. Does not apply `COPILOT_EXP_*` experiment assignments. Defaults to this runtime process's own environment. */
    @JsonProperty("environment") Map<String, String> environment,
    /** User settings to apply, including `enabledFeatureFlags`, legacy `feature_flags.enabled`, and supported convenience settings. */
    @JsonProperty("config") Object config,
    /** Flags forced to a value after every other layer, keyed by flag name. */
    @JsonProperty("flagOverrides") Map<String, Boolean> flagOverrides,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    FeatureFlagsResolveParams { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    FeatureFlagsResolveParams(Boolean isStaff, Boolean isExperimental, Boolean isTeam, Boolean isStaffGitHub, Boolean isStaffMicrosoft, Map<String, Boolean> baseFlags, Map<String, String> environment, Object config, Map<String, Boolean> flagOverrides) {
        this(isStaff, isExperimental, isTeam, isStaffGitHub, isStaffMicrosoft, baseFlags, environment, config, flagOverrides, java.util.Set.of());
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
        private Boolean isStaff;
        private Boolean isExperimental;
        private Boolean isTeam;
        private Boolean isStaffGitHub;
        private Boolean isStaffMicrosoft;
        private Map<String, Boolean> baseFlags;
        private Map<String, String> environment;
        private Object config;
        private boolean $presence7;
        private Map<String, Boolean> flagOverrides;

        /**
         * Sets the {@code isStaff} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder isStaff(Boolean value) {
            this.isStaff = value;
            return this;
        }

        /**
         * Sets the {@code isExperimental} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder isExperimental(Boolean value) {
            this.isExperimental = value;
            return this;
        }

        /**
         * Sets the {@code isTeam} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder isTeam(Boolean value) {
            this.isTeam = value;
            return this;
        }

        /**
         * Sets the {@code isStaffGithub} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder isStaffGitHub(Boolean value) {
            this.isStaffGitHub = value;
            return this;
        }

        /**
         * Sets the {@code isStaffMicrosoft} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder isStaffMicrosoft(Boolean value) {
            this.isStaffMicrosoft = value;
            return this;
        }

        /**
         * Sets the {@code baseFlags} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder baseFlags(Map<String, Boolean> value) {
            this.baseFlags = value;
            return this;
        }

        /**
         * Sets the {@code environment} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder environment(Map<String, String> value) {
            this.environment = value;
            return this;
        }

        /**
         * Sets the {@code config} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder config(Object value) {
            this.config = value;
            this.$presence7 = true;
            return this;
        }

        /**
         * Sets the {@code flagOverrides} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder flagOverrides(Map<String, Boolean> value) {
            this.flagOverrides = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public FeatureFlagsResolveParams build() {
            if (isStaff == null) throw new IllegalStateException("Missing required property: isStaff");
            if (isExperimental == null) throw new IllegalStateException("Missing required property: isExperimental");
            if (isTeam == null) throw new IllegalStateException("Missing required property: isTeam");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence7 && config == null) $explicitNulls.add("config");
            return new FeatureFlagsResolveParams(isStaff, isExperimental, isTeam, isStaffGitHub, isStaffMicrosoft, baseFlags, environment, config, flagOverrides, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("config") && config == null) values.put("config", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<FeatureFlagsResolveParams> {
        @Override
        public FeatureFlagsResolveParams deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(FeatureFlagsResolveParams.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("config") && tree.get("config").isNull()) $explicitNulls.add("config");
            return new FeatureFlagsResolveParams(readProperty(context, tree, "isStaff", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "isExperimental", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "isTeam", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "isStaffGithub", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "isStaffMicrosoft", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "baseFlags", new com.fasterxml.jackson.core.type.TypeReference<Map<String, Boolean>>() {}), readProperty(context, tree, "environment", new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {}), readProperty(context, tree, "config", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "flagOverrides", new com.fasterxml.jackson.core.type.TypeReference<Map<String, Boolean>>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
