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
 * Evaluation inputs for the bound session's feature service, not authentication claims.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SessionFeatureFlagsConfigureParams.$NullableDeserializer.class)
public record SessionFeatureFlagsConfigureParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Static staff availability; this does not establish staff authentication. */
    @JsonProperty("isStaff") Boolean isStaff,
    /** GitHub staff population, when the host recorded split populations. */
    @JsonProperty("isStaffGithub") Boolean isStaffGitHub,
    /** Microsoft staff population, when the host recorded split populations. */
    @JsonProperty("isStaffMicrosoft") Boolean isStaffMicrosoft,
    /** Whether experimental features are enabled. */
    @JsonProperty("isExperimental") Boolean isExperimental,
    /** Static team availability; this does not select the authenticated account. */
    @JsonProperty("isTeam") Boolean isTeam,
    /** The retained user-settings snapshot. */
    @JsonProperty("config") Object config,
    /** Explicit static overrides, after environment and user settings. */
    @JsonProperty("flagOverrides") Map<String, Boolean> flagOverrides,
    /** Constructor scalar seed; omission retains the existing constructor snapshot. */
    @JsonProperty("initialSessionFlags") Map<String, Boolean> initialSessionFlags,
    /** Explicit experiment overrides, after delivered assignments. */
    @JsonInclude(value = JsonInclude.Include.NON_NULL, content = JsonInclude.Include.ALWAYS)
    @JsonProperty("expFlagOverrides") Map<String, Object> expFlagOverrides,
    /** Host environment snapshot; omit to use the live process environment. */
    @JsonProperty("environment") Map<String, String> environment,
    /** Install the existing online assignment source unless assignments are explicit. */
    @JsonProperty("online") Boolean online,
    /** Explicit assignment replacement or clear; omission retains current assignments. */
    @JsonProperty("assignments") FeatureFlagAssignmentsUpdate assignments,
    /** Whether online segmentation withholds audience populations. */
    @JsonProperty("streamerMode") Boolean streamerMode,
    /** Installation first-launch time in Unix milliseconds, when known. */
    @JsonProperty("firstLaunchAtMillis") Double firstLaunchAtMillis,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionFeatureFlagsConfigureParams { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionFeatureFlagsConfigureParams(String sessionId, Boolean isStaff, Boolean isStaffGitHub, Boolean isStaffMicrosoft, Boolean isExperimental, Boolean isTeam, Object config, Map<String, Boolean> flagOverrides, Map<String, Boolean> initialSessionFlags, Map<String, Object> expFlagOverrides, Map<String, String> environment, Boolean online, FeatureFlagAssignmentsUpdate assignments, Boolean streamerMode, Double firstLaunchAtMillis) {
        this(sessionId, isStaff, isStaffGitHub, isStaffMicrosoft, isExperimental, isTeam, config, flagOverrides, initialSessionFlags, expFlagOverrides, environment, online, assignments, streamerMode, firstLaunchAtMillis, java.util.Set.of());
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
        private String sessionId;
        private Boolean isStaff;
        private Boolean isStaffGitHub;
        private Boolean isStaffMicrosoft;
        private Boolean isExperimental;
        private Boolean isTeam;
        private Object config;
        private boolean $presence6;
        private Map<String, Boolean> flagOverrides;
        private Map<String, Boolean> initialSessionFlags;
        private Map<String, Object> expFlagOverrides;
        private Map<String, String> environment;
        private Boolean online;
        private FeatureFlagAssignmentsUpdate assignments;
        private Boolean streamerMode;
        private Double firstLaunchAtMillis;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code isStaff} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder isStaff(Boolean value) {
            this.isStaff = value;
            return this;
        }

        /**
         * Sets the {@code isStaffGithub} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
        public Builder isStaffMicrosoft(Boolean value) {
            this.isStaffMicrosoft = value;
            return this;
        }

        /**
         * Sets the {@code isExperimental} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
        public Builder isTeam(Boolean value) {
            this.isTeam = value;
            return this;
        }

        /**
         * Sets the {@code config} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder config(Object value) {
            this.config = value;
            this.$presence6 = true;
            return this;
        }

        /**
         * Sets the {@code flagOverrides} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder flagOverrides(Map<String, Boolean> value) {
            this.flagOverrides = value;
            return this;
        }

        /**
         * Sets the {@code initialSessionFlags} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder initialSessionFlags(Map<String, Boolean> value) {
            this.initialSessionFlags = value;
            return this;
        }

        /**
         * Sets the {@code expFlagOverrides} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder expFlagOverrides(Map<String, Object> value) {
            this.expFlagOverrides = value;
            return this;
        }

        /**
         * Sets the {@code environment} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder environment(Map<String, String> value) {
            this.environment = value;
            return this;
        }

        /**
         * Sets the {@code online} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder online(Boolean value) {
            this.online = value;
            return this;
        }

        /**
         * Sets the {@code assignments} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder assignments(FeatureFlagAssignmentsUpdate value) {
            this.assignments = value;
            return this;
        }

        /**
         * Sets the {@code streamerMode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
        public Builder firstLaunchAtMillis(Double value) {
            this.firstLaunchAtMillis = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionFeatureFlagsConfigureParams build() {
            if (isStaff == null) throw new IllegalStateException("Missing required property: isStaff");
            if (isExperimental == null) throw new IllegalStateException("Missing required property: isExperimental");
            if (isTeam == null) throw new IllegalStateException("Missing required property: isTeam");
            if (online == null) throw new IllegalStateException("Missing required property: online");
            if (streamerMode == null) throw new IllegalStateException("Missing required property: streamerMode");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence6 && config == null) $explicitNulls.add("config");
            return new SessionFeatureFlagsConfigureParams(sessionId, isStaff, isStaffGitHub, isStaffMicrosoft, isExperimental, isTeam, config, flagOverrides, initialSessionFlags, expFlagOverrides, environment, online, assignments, streamerMode, firstLaunchAtMillis, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("config") && config == null) values.put("config", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<SessionFeatureFlagsConfigureParams> {
        @Override
        public SessionFeatureFlagsConfigureParams deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(SessionFeatureFlagsConfigureParams.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("config") && tree.get("config").isNull()) $explicitNulls.add("config");
            return new SessionFeatureFlagsConfigureParams(readProperty(context, tree, "sessionId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "isStaff", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "isStaffGithub", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "isStaffMicrosoft", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "isExperimental", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "isTeam", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "config", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "flagOverrides", new com.fasterxml.jackson.core.type.TypeReference<Map<String, Boolean>>() {}), readProperty(context, tree, "initialSessionFlags", new com.fasterxml.jackson.core.type.TypeReference<Map<String, Boolean>>() {}), readProperty(context, tree, "expFlagOverrides", new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}), readProperty(context, tree, "environment", new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {}), readProperty(context, tree, "online", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "assignments", new com.fasterxml.jackson.core.type.TypeReference<FeatureFlagAssignmentsUpdate>() {}), readProperty(context, tree, "streamerMode", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "firstLaunchAtMillis", new com.fasterxml.jackson.core.type.TypeReference<Double>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
