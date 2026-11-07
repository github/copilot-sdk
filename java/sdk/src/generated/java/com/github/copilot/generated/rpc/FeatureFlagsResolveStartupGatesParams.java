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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Caller population and startup inputs to resolve named online-experiment gates for. Carries no account: the runtime resolves the assignment for the authentication it already holds.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = FeatureFlagsResolveStartupGatesParams.$NullableDeserializer.class)
record FeatureFlagsResolveStartupGatesParams(
    /** The feature flags to resolve. A name this runtime does not declare is reported as an error rather than as a disabled flag. */
    @JsonProperty("flags") List<String> flags,
    /** Whether the caller is GitHub or Microsoft staff. */
    @JsonProperty("isStaff") Boolean isStaff,
    /** Whether the caller is in an experimental population. */
    @JsonProperty("isExperimental") Boolean isExperimental,
    /** Whether the caller belongs to a team plan. */
    @JsonProperty("isTeam") Boolean isTeam,
    /** GitHub-staff membership. Omit when it was never recorded: a legacy staff account then counts as both staff populations and keeps every staff feature. */
    @JsonProperty("isStaffGithub") Boolean isStaffGitHub,
    /** Microsoft-staff membership. Omitted the same way as `isStaffGitHub`. */
    @JsonProperty("isStaffMicrosoft") Boolean isStaffMicrosoft,
    /** Environment to read flag overrides from. Defaults to this runtime process's own environment. */
    @JsonProperty("environment") Map<String, String> environment,
    /** User settings document whose `featureFlags` block is layered on top of the resolved set. */
    @JsonProperty("config") Object config,
    /** Whether streamer mode is on, which withholds the populations an experiment would otherwise segment on. */
    @JsonProperty("streamerMode") Boolean streamerMode,
    /** When this installation first launched, in milliseconds since the Unix epoch. Experiments that segment on installation age read it. Omit when the first launch was never recorded. */
    @JsonProperty("firstLaunchAtMillis") Double firstLaunchAtMillis,
    /** Whether the runtime may refresh the cached assignment over the network without an active session. The refresh never blocks this call: the answer comes from the cache either way, so a refresh only affects a later launch. */
    @JsonProperty("refreshWithoutSession") Boolean refreshWithoutSession,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    FeatureFlagsResolveStartupGatesParams { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    FeatureFlagsResolveStartupGatesParams(List<String> flags, Boolean isStaff, Boolean isExperimental, Boolean isTeam, Boolean isStaffGitHub, Boolean isStaffMicrosoft, Map<String, String> environment, Object config, Boolean streamerMode, Double firstLaunchAtMillis, Boolean refreshWithoutSession) {
        this(flags, isStaff, isExperimental, isTeam, isStaffGitHub, isStaffMicrosoft, environment, config, streamerMode, firstLaunchAtMillis, refreshWithoutSession, java.util.Set.of());
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
        private List<String> flags;
        private Boolean isStaff;
        private Boolean isExperimental;
        private Boolean isTeam;
        private Boolean isStaffGitHub;
        private Boolean isStaffMicrosoft;
        private Map<String, String> environment;
        private Object config;
        private boolean $presence7;
        private Boolean streamerMode;
        private Double firstLaunchAtMillis;
        private Boolean refreshWithoutSession;

        /**
         * Sets the {@code flags} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder flags(List<String> value) {
            this.flags = value;
            return this;
        }

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
         * Sets the {@code streamerMode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder streamerMode(Boolean value) {
            this.streamerMode = value;
            return this;
        }

        /**
         * Sets the {@code firstLaunchAtMillis} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder firstLaunchAtMillis(Double value) {
            this.firstLaunchAtMillis = value;
            return this;
        }

        /**
         * Sets the {@code refreshWithoutSession} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder refreshWithoutSession(Boolean value) {
            this.refreshWithoutSession = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public FeatureFlagsResolveStartupGatesParams build() {
            if (flags == null) throw new IllegalStateException("Missing required property: flags");
            if (isStaff == null) throw new IllegalStateException("Missing required property: isStaff");
            if (isExperimental == null) throw new IllegalStateException("Missing required property: isExperimental");
            if (isTeam == null) throw new IllegalStateException("Missing required property: isTeam");
            if (streamerMode == null) throw new IllegalStateException("Missing required property: streamerMode");
            if (refreshWithoutSession == null) throw new IllegalStateException("Missing required property: refreshWithoutSession");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence7 && config == null) $explicitNulls.add("config");
            return new FeatureFlagsResolveStartupGatesParams(flags, isStaff, isExperimental, isTeam, isStaffGitHub, isStaffMicrosoft, environment, config, streamerMode, firstLaunchAtMillis, refreshWithoutSession, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("config") && config == null) values.put("config", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<FeatureFlagsResolveStartupGatesParams> {
        @Override
        public FeatureFlagsResolveStartupGatesParams deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(FeatureFlagsResolveStartupGatesParams.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("config") && tree.get("config").isNull()) $explicitNulls.add("config");
            return new FeatureFlagsResolveStartupGatesParams(readProperty(context, tree, "flags", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "isStaff", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "isExperimental", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "isTeam", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "isStaffGithub", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "isStaffMicrosoft", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "environment", new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {}), readProperty(context, tree, "config", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "streamerMode", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "firstLaunchAtMillis", new com.fasterxml.jackson.core.type.TypeReference<Double>() {}), readProperty(context, tree, "refreshWithoutSession", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
