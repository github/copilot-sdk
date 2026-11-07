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
 * Inputs describing the host and account an ingestion should resolve managed policy for.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ManagedSettingsIngestParams.$NullableDeserializer.class)
record ManagedSettingsIngestParams(
    /** The policy-helper invocation reason. Defaults to sessionStart. */
    @JsonProperty("policyHelperReason") ManagedSettingsPolicyHelperReason policyHelperReason,
    /** Whether to run ingestion. Omit or set false to return an empty result without discovering or fetching policy. */
    @JsonProperty("selfFetch") Boolean selfFetch,
    /** The authenticated identity to resolve policy for. Omitted before sign-in, in which case no server layer is fetched. */
    @JsonProperty("authInfo") Object authInfo,
    /** The session whose credential store and working directory the policy helper should use. Omit it when no session exists yet and the process-wide store applies. */
    @JsonProperty("sessionId") String sessionId,
    /** A device layer supplied by the host instead of discovered from the machine. Pair it with `hasDeviceResponse` so an explicit absence is distinguishable from an omitted override. */
    @JsonProperty("deviceResponse") Object deviceResponse,
    /** Whether `deviceResponse` is an override at all. True with a null `deviceResponse` asserts the device layer is empty; false discovers it from the machine. */
    @JsonProperty("hasDeviceResponse") Boolean hasDeviceResponse,
    /** Whether to ignore a cached server policy when reading, forcing the resolution to come from a live fetch. */
    @JsonProperty("bypassCacheRead") Boolean bypassCacheRead,
    /** How this ingestion may use the on-disk server-policy cache. Defaults to the runtime's own policy when omitted. */
    @JsonProperty("serverPolicyCacheMode") ManagedSettingsServerPolicyCacheMode serverPolicyCacheMode,
    /** Whether a configured platform policy helper may be run to produce an enforced helper layer. */
    @JsonProperty("policyHelperEnabled") Boolean policyHelperEnabled,
    /** Directory the policy helper runs in when this ingestion is not owned by a registered session. A host resolving policy before its session exists must pass this, or a directory-sensitive helper judges the wrong repository. */
    @JsonProperty("policyHelperWorkingDirectory") String policyHelperWorkingDirectory,
    /** User agent to send with the policy fetch. */
    @JsonProperty("userAgent") String userAgent,
    /** Name of the embedding client, reported to the managed-settings service as `client_id` so it can refuse to deliver a policy this client is too old to enforce. Omit it and the runtime reports a generic identity; the runtime version sent alongside is the gate that actually matters. */
    @JsonProperty("clientName") String clientName,
    /** The host's environment variables, read for cache location and policy overrides. Supplied by the host rather than read from the runtime process so a command that selected its own configuration home is honored. */
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
    ManagedSettingsIngestParams { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    ManagedSettingsIngestParams(ManagedSettingsPolicyHelperReason policyHelperReason, Boolean selfFetch, Object authInfo, String sessionId, Object deviceResponse, Boolean hasDeviceResponse, Boolean bypassCacheRead, ManagedSettingsServerPolicyCacheMode serverPolicyCacheMode, Boolean policyHelperEnabled, String policyHelperWorkingDirectory, String userAgent, String clientName, Object environment, String platform, String homeDirectory) {
        this(policyHelperReason, selfFetch, authInfo, sessionId, deviceResponse, hasDeviceResponse, bypassCacheRead, serverPolicyCacheMode, policyHelperEnabled, policyHelperWorkingDirectory, userAgent, clientName, environment, platform, homeDirectory, java.util.Set.of());
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
        private String sessionId;
        private Object deviceResponse;
        private boolean $presence4;
        private Boolean hasDeviceResponse;
        private Boolean bypassCacheRead;
        private ManagedSettingsServerPolicyCacheMode serverPolicyCacheMode;
        private Boolean policyHelperEnabled;
        private String policyHelperWorkingDirectory;
        private String userAgent;
        private String clientName;
        private Object environment;
        private boolean $presence12;
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
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionId(String value) {
            this.sessionId = value;
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
            this.$presence4 = true;
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
         * Sets the {@code bypassCacheRead} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder bypassCacheRead(Boolean value) {
            this.bypassCacheRead = value;
            return this;
        }

        /**
         * Sets the {@code serverPolicyCacheMode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder serverPolicyCacheMode(ManagedSettingsServerPolicyCacheMode value) {
            this.serverPolicyCacheMode = value;
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
         * Sets the {@code policyHelperWorkingDirectory} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder policyHelperWorkingDirectory(String value) {
            this.policyHelperWorkingDirectory = value;
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
            this.$presence12 = true;
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
        public ManagedSettingsIngestParams build() {
            if (userAgent == null) throw new IllegalStateException("Missing required property: userAgent");
            if (!$presence12) throw new IllegalStateException("Missing required property: environment");
            if (platform == null) throw new IllegalStateException("Missing required property: platform");
            if (homeDirectory == null) throw new IllegalStateException("Missing required property: homeDirectory");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence2 && authInfo == null) $explicitNulls.add("authInfo");
            if ($presence4 && deviceResponse == null) $explicitNulls.add("deviceResponse");
            return new ManagedSettingsIngestParams(policyHelperReason, selfFetch, authInfo, sessionId, deviceResponse, hasDeviceResponse, bypassCacheRead, serverPolicyCacheMode, policyHelperEnabled, policyHelperWorkingDirectory, userAgent, clientName, environment, platform, homeDirectory, $explicitNulls);
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

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<ManagedSettingsIngestParams> {
        @Override
        public ManagedSettingsIngestParams deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(ManagedSettingsIngestParams.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("authInfo") && tree.get("authInfo").isNull()) $explicitNulls.add("authInfo");
            if (tree.has("deviceResponse") && tree.get("deviceResponse").isNull()) $explicitNulls.add("deviceResponse");
            return new ManagedSettingsIngestParams(readProperty(context, tree, "policyHelperReason", new com.fasterxml.jackson.core.type.TypeReference<ManagedSettingsPolicyHelperReason>() {}), readProperty(context, tree, "selfFetch", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "authInfo", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "sessionId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "deviceResponse", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "hasDeviceResponse", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "bypassCacheRead", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "serverPolicyCacheMode", new com.fasterxml.jackson.core.type.TypeReference<ManagedSettingsServerPolicyCacheMode>() {}), readProperty(context, tree, "policyHelperEnabled", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "policyHelperWorkingDirectory", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "userAgent", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "clientName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "environment", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "platform", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "homeDirectory", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
