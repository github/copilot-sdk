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
 * The managed layers to retain as this process's startup policy, with the outcome of fetching each one.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ManagedSettingsRetainPolicyParams.$NullableDeserializer.class)
record ManagedSettingsRetainPolicyParams(
    /** The resolved server policy. A known source can lack a response after a forced-refresh failure. */
    @JsonProperty("serverResponse") Object serverResponse,
    /** The device/MDM policy layer. */
    @JsonProperty("deviceResponse") Object deviceResponse,
    /** Sandbox floor declared by the device layer, carried separately so a document that failed validation can still raise the floor. */
    @JsonProperty("deviceSandboxFloor") Object deviceSandboxFloor,
    /** Whether a device policy source demonstrably exists. This, not a read error, is the evidence a device is managed, so a fail-closed policy gates on it. */
    @JsonProperty("deviceSourceFound") Boolean deviceSourceFound,
    /** Whether any managed policy source exists, independently of fetch failures. */
    @JsonProperty("managedPolicyPresent") Boolean managedPolicyPresent,
    /** Whether the server policy source exists, even when its response is unavailable. */
    @JsonProperty("serverSourceFound") Boolean serverSourceFound,
    /** Whether a required fresh server-policy confirmation failed. */
    @JsonProperty("serverForcedRefreshFailure") Boolean serverForcedRefreshFailure,
    /** Preserve the retained server layer during a device-only resolution. */
    @JsonProperty("preserveServerState") Boolean preserveServerState,
    /** Whether the device sandbox intent is unreadable independently of its other settings. */
    @JsonProperty("deviceSandboxUndetermined") Boolean deviceSandboxUndetermined,
    /** Whether the shared sandbox policy is undetermined. Diagnostic errors alone do not imply this flag. */
    @JsonProperty("sandboxFloorUndetermined") Boolean sandboxFloorUndetermined,
    /** Whether permission policy is undetermined. Sandbox-only uncertainty does not imply this flag. */
    @JsonProperty("permissionPolicyUndetermined") Boolean permissionPolicyUndetermined,
    /** Whether uncertainty alone enabled the shared sandbox floor. */
    @JsonProperty("sandboxEnabledByUndeterminedPolicy") Boolean sandboxEnabledByUndeterminedPolicy,
    /** Shared server/device sandbox floor, excluding session-local helper policy. */
    @JsonProperty("sharedSandboxFloor") Object sharedSandboxFloor,
    /** Whether the server fetch failed. Report it honestly: true keeps the already-retained server layer rather than replacing it with an absent one. */
    @JsonProperty("serverFetchFailed") Boolean serverFetchFailed,
    /** Whether the device load failed. Report it honestly, for the same reason as `serverFetchFailed`. */
    @JsonProperty("deviceLoadFailed") Boolean deviceLoadFailed,
    /** The policy helper's response: the third managed layer that a process owning its startup policy resolves. It is retained with the other two so that every ambient read in this process, including the mutation guards and the governed marketplace allowlist that the `plugins.*` handlers derive, sees the whole policy the process fetched. A session-owning process omits it, because it resolves a helper per session against that session's working directory. */
    @JsonProperty("policyHelperResponse") Object policyHelperResponse,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    ManagedSettingsRetainPolicyParams { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    ManagedSettingsRetainPolicyParams(Object serverResponse, Object deviceResponse, Object deviceSandboxFloor, Boolean deviceSourceFound, Boolean managedPolicyPresent, Boolean serverSourceFound, Boolean serverForcedRefreshFailure, Boolean preserveServerState, Boolean deviceSandboxUndetermined, Boolean sandboxFloorUndetermined, Boolean permissionPolicyUndetermined, Boolean sandboxEnabledByUndeterminedPolicy, Object sharedSandboxFloor, Boolean serverFetchFailed, Boolean deviceLoadFailed, Object policyHelperResponse) {
        this(serverResponse, deviceResponse, deviceSandboxFloor, deviceSourceFound, managedPolicyPresent, serverSourceFound, serverForcedRefreshFailure, preserveServerState, deviceSandboxUndetermined, sandboxFloorUndetermined, permissionPolicyUndetermined, sandboxEnabledByUndeterminedPolicy, sharedSandboxFloor, serverFetchFailed, deviceLoadFailed, policyHelperResponse, java.util.Set.of());
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
        private Object serverResponse;
        private boolean $presence0;
        private Object deviceResponse;
        private boolean $presence1;
        private Object deviceSandboxFloor;
        private boolean $presence2;
        private Boolean deviceSourceFound;
        private Boolean managedPolicyPresent;
        private Boolean serverSourceFound;
        private Boolean serverForcedRefreshFailure;
        private Boolean preserveServerState;
        private Boolean deviceSandboxUndetermined;
        private Boolean sandboxFloorUndetermined;
        private Boolean permissionPolicyUndetermined;
        private Boolean sandboxEnabledByUndeterminedPolicy;
        private Object sharedSandboxFloor;
        private boolean $presence12;
        private Boolean serverFetchFailed;
        private Boolean deviceLoadFailed;
        private Object policyHelperResponse;
        private boolean $presence15;

        /**
         * Sets the {@code serverResponse} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder serverResponse(Object value) {
            this.serverResponse = value;
            this.$presence0 = true;
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
         * Sets the {@code deviceSourceFound} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder deviceSourceFound(Boolean value) {
            this.deviceSourceFound = value;
            return this;
        }

        /**
         * Sets the {@code managedPolicyPresent} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder managedPolicyPresent(Boolean value) {
            this.managedPolicyPresent = value;
            return this;
        }

        /**
         * Sets the {@code serverSourceFound} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder serverSourceFound(Boolean value) {
            this.serverSourceFound = value;
            return this;
        }

        /**
         * Sets the {@code serverForcedRefreshFailure} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder serverForcedRefreshFailure(Boolean value) {
            this.serverForcedRefreshFailure = value;
            return this;
        }

        /**
         * Sets the {@code preserveServerState} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder preserveServerState(Boolean value) {
            this.preserveServerState = value;
            return this;
        }

        /**
         * Sets the {@code deviceSandboxUndetermined} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder deviceSandboxUndetermined(Boolean value) {
            this.deviceSandboxUndetermined = value;
            return this;
        }

        /**
         * Sets the {@code sandboxFloorUndetermined} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sandboxFloorUndetermined(Boolean value) {
            this.sandboxFloorUndetermined = value;
            return this;
        }

        /**
         * Sets the {@code permissionPolicyUndetermined} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder permissionPolicyUndetermined(Boolean value) {
            this.permissionPolicyUndetermined = value;
            return this;
        }

        /**
         * Sets the {@code sandboxEnabledByUndeterminedPolicy} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sandboxEnabledByUndeterminedPolicy(Boolean value) {
            this.sandboxEnabledByUndeterminedPolicy = value;
            return this;
        }

        /**
         * Sets the {@code sharedSandboxFloor} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sharedSandboxFloor(Object value) {
            this.sharedSandboxFloor = value;
            this.$presence12 = true;
            return this;
        }

        /**
         * Sets the {@code serverFetchFailed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder serverFetchFailed(Boolean value) {
            this.serverFetchFailed = value;
            return this;
        }

        /**
         * Sets the {@code deviceLoadFailed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder deviceLoadFailed(Boolean value) {
            this.deviceLoadFailed = value;
            return this;
        }

        /**
         * Sets the {@code policyHelperResponse} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder policyHelperResponse(Object value) {
            this.policyHelperResponse = value;
            this.$presence15 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ManagedSettingsRetainPolicyParams build() {
            if (deviceSourceFound == null) throw new IllegalStateException("Missing required property: deviceSourceFound");
            if (managedPolicyPresent == null) throw new IllegalStateException("Missing required property: managedPolicyPresent");
            if (serverSourceFound == null) throw new IllegalStateException("Missing required property: serverSourceFound");
            if (serverForcedRefreshFailure == null) throw new IllegalStateException("Missing required property: serverForcedRefreshFailure");
            if (preserveServerState == null) throw new IllegalStateException("Missing required property: preserveServerState");
            if (deviceSandboxUndetermined == null) throw new IllegalStateException("Missing required property: deviceSandboxUndetermined");
            if (sandboxFloorUndetermined == null) throw new IllegalStateException("Missing required property: sandboxFloorUndetermined");
            if (permissionPolicyUndetermined == null) throw new IllegalStateException("Missing required property: permissionPolicyUndetermined");
            if (sandboxEnabledByUndeterminedPolicy == null) throw new IllegalStateException("Missing required property: sandboxEnabledByUndeterminedPolicy");
            if (serverFetchFailed == null) throw new IllegalStateException("Missing required property: serverFetchFailed");
            if (deviceLoadFailed == null) throw new IllegalStateException("Missing required property: deviceLoadFailed");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence0 && serverResponse == null) $explicitNulls.add("serverResponse");
            if ($presence1 && deviceResponse == null) $explicitNulls.add("deviceResponse");
            if ($presence2 && deviceSandboxFloor == null) $explicitNulls.add("deviceSandboxFloor");
            if ($presence12 && sharedSandboxFloor == null) $explicitNulls.add("sharedSandboxFloor");
            if ($presence15 && policyHelperResponse == null) $explicitNulls.add("policyHelperResponse");
            return new ManagedSettingsRetainPolicyParams(serverResponse, deviceResponse, deviceSandboxFloor, deviceSourceFound, managedPolicyPresent, serverSourceFound, serverForcedRefreshFailure, preserveServerState, deviceSandboxUndetermined, sandboxFloorUndetermined, permissionPolicyUndetermined, sandboxEnabledByUndeterminedPolicy, sharedSandboxFloor, serverFetchFailed, deviceLoadFailed, policyHelperResponse, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("serverResponse") && serverResponse == null) values.put("serverResponse", null);
        if ($explicitNulls.contains("deviceResponse") && deviceResponse == null) values.put("deviceResponse", null);
        if ($explicitNulls.contains("deviceSandboxFloor") && deviceSandboxFloor == null) values.put("deviceSandboxFloor", null);
        if ($explicitNulls.contains("sharedSandboxFloor") && sharedSandboxFloor == null) values.put("sharedSandboxFloor", null);
        if ($explicitNulls.contains("policyHelperResponse") && policyHelperResponse == null) values.put("policyHelperResponse", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<ManagedSettingsRetainPolicyParams> {
        @Override
        public ManagedSettingsRetainPolicyParams deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(ManagedSettingsRetainPolicyParams.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("serverResponse") && tree.get("serverResponse").isNull()) $explicitNulls.add("serverResponse");
            if (tree.has("deviceResponse") && tree.get("deviceResponse").isNull()) $explicitNulls.add("deviceResponse");
            if (tree.has("deviceSandboxFloor") && tree.get("deviceSandboxFloor").isNull()) $explicitNulls.add("deviceSandboxFloor");
            if (tree.has("sharedSandboxFloor") && tree.get("sharedSandboxFloor").isNull()) $explicitNulls.add("sharedSandboxFloor");
            if (tree.has("policyHelperResponse") && tree.get("policyHelperResponse").isNull()) $explicitNulls.add("policyHelperResponse");
            return new ManagedSettingsRetainPolicyParams(readProperty(context, tree, "serverResponse", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "deviceResponse", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "deviceSandboxFloor", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "deviceSourceFound", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "managedPolicyPresent", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "serverSourceFound", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "serverForcedRefreshFailure", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "preserveServerState", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "deviceSandboxUndetermined", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "sandboxFloorUndetermined", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "permissionPolicyUndetermined", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "sandboxEnabledByUndeterminedPolicy", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "sharedSandboxFloor", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "serverFetchFailed", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "deviceLoadFailed", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "policyHelperResponse", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
