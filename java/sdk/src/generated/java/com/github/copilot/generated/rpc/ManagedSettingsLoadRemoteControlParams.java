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
 * Inputs describing the host and account whose managed remote-control policy should be resolved. Mirrors `managedSettings.ingest`, and deliberately carries no retention decision: reading this gate never becomes this runtime process's startup policy.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ManagedSettingsLoadRemoteControlParams.$NullableDeserializer.class)
record ManagedSettingsLoadRemoteControlParams(
    /** The policy-helper invocation reason. Defaults to sessionStart. */
    @JsonProperty("policyHelperReason") ManagedSettingsPolicyHelperReason policyHelperReason,
    /** Whether to run ingestion. Omit or set false to return an empty result without discovering or fetching policy. */
    @JsonProperty("selfFetch") Boolean selfFetch,
    /** The authenticated identity to resolve policy for. Omitted before sign-in, in which case no server layer is fetched. */
    @JsonProperty("authInfo") Object authInfo,
    /** A device layer supplied by the host instead of discovered from the machine. Pair it with `hasDeviceResponse` so an explicit absence is distinguishable from an omitted override. */
    @JsonProperty("deviceResponse") Object deviceResponse,
    /** Whether `deviceResponse` is an override at all. True with a null `deviceResponse` asserts the device layer is empty; false discovers it from the machine. */
    @JsonProperty("hasDeviceResponse") Boolean hasDeviceResponse,
    /** Whether a configured platform policy helper may be run to produce an enforced helper layer. */
    @JsonProperty("policyHelperEnabled") Boolean policyHelperEnabled,
    /** User agent to send with the policy fetch. */
    @JsonProperty("userAgent") String userAgent,
    /** Name of the embedding client, reported to the managed-settings service as `client_id` so it can refuse to deliver a policy this client is too old to enforce. That refusal is exactly what turns into an explicit `disabled` below, so a host that omits this field forfeits the protection. */
    @JsonProperty("clientName") String clientName,
    /** The host's environment variables, read for cache location and policy overrides. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("environment") Object environment,
    /** The host's platform, in Node's `process.platform` spelling, used to locate platform-specific cache and policy paths. */
    @JsonProperty("platform") String platform,
    /** The host's home directory, used to resolve cache and managed-file locations. */
    @JsonProperty("homeDirectory") String homeDirectory,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    ManagedSettingsLoadRemoteControlParams { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    ManagedSettingsLoadRemoteControlParams(ManagedSettingsPolicyHelperReason policyHelperReason, Boolean selfFetch, Object authInfo, Object deviceResponse, Boolean hasDeviceResponse, Boolean policyHelperEnabled, String userAgent, String clientName, Object environment, String platform, String homeDirectory) {
        this(policyHelperReason, selfFetch, authInfo, deviceResponse, hasDeviceResponse, policyHelperEnabled, userAgent, clientName, environment, platform, homeDirectory, java.util.Set.of());
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
        private ManagedSettingsPolicyHelperReason policyHelperReason;
        private Boolean selfFetch;
        private Object authInfo;
        private boolean $presence2;
        private Object deviceResponse;
        private boolean $presence3;
        private Boolean hasDeviceResponse;
        private Boolean policyHelperEnabled;
        private String userAgent;
        private String clientName;
        private Object environment;
        private boolean $presence8;
        private String platform;
        private String homeDirectory;

        /**
         * Sets the {@code policyHelperReason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder policyHelperReason(ManagedSettingsPolicyHelperReason value) {
            this.policyHelperReason = value;
            return this;
        }

        /**
         * Sets the {@code selfFetch} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder selfFetch(Boolean value) {
            this.selfFetch = value;
            return this;
        }

        /**
         * Sets the {@code authInfo} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder authInfo(Object value) {
            this.authInfo = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Sets the {@code deviceResponse} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder deviceResponse(Object value) {
            this.deviceResponse = value;
            this.$presence3 = true;
            return this;
        }

        /**
         * Sets the {@code hasDeviceResponse} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder hasDeviceResponse(Boolean value) {
            this.hasDeviceResponse = value;
            return this;
        }

        /**
         * Sets the {@code policyHelperEnabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder policyHelperEnabled(Boolean value) {
            this.policyHelperEnabled = value;
            return this;
        }

        /**
         * Sets the {@code userAgent} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder userAgent(String value) {
            this.userAgent = value;
            return this;
        }

        /**
         * Sets the {@code clientName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder clientName(String value) {
            this.clientName = value;
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
            this.$presence8 = true;
            return this;
        }

        /**
         * Sets the {@code platform} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder platform(String value) {
            this.platform = value;
            return this;
        }

        /**
         * Sets the {@code homeDirectory} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder homeDirectory(String value) {
            this.homeDirectory = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ManagedSettingsLoadRemoteControlParams build() {
            if (userAgent == null) throw new IllegalStateException("Missing required property: userAgent");
            if (!$presence8) throw new IllegalStateException("Missing required property: environment");
            if (platform == null) throw new IllegalStateException("Missing required property: platform");
            if (homeDirectory == null) throw new IllegalStateException("Missing required property: homeDirectory");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence2 && authInfo == null) $explicitNulls.add("authInfo");
            if ($presence3 && deviceResponse == null) $explicitNulls.add("deviceResponse");
            return new ManagedSettingsLoadRemoteControlParams(policyHelperReason, selfFetch, authInfo, deviceResponse, hasDeviceResponse, policyHelperEnabled, userAgent, clientName, environment, platform, homeDirectory, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("authInfo") && authInfo == null) values.put("authInfo", null);
        if ($explicitNulls.contains("deviceResponse") && deviceResponse == null) values.put("deviceResponse", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<ManagedSettingsLoadRemoteControlParams> {
        @Override
        public ManagedSettingsLoadRemoteControlParams deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(ManagedSettingsLoadRemoteControlParams.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("authInfo") && tree.get("authInfo").isNull()) $explicitNulls.add("authInfo");
            if (tree.has("deviceResponse") && tree.get("deviceResponse").isNull()) $explicitNulls.add("deviceResponse");
            return new ManagedSettingsLoadRemoteControlParams(readProperty(context, tree, "policyHelperReason", new com.fasterxml.jackson.core.type.TypeReference<ManagedSettingsPolicyHelperReason>() {}), readProperty(context, tree, "selfFetch", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "authInfo", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "deviceResponse", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "hasDeviceResponse", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "policyHelperEnabled", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "userAgent", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "clientName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "environment", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "platform", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "homeDirectory", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
