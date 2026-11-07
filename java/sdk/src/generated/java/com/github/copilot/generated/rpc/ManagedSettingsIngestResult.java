/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.generated.PolicyHelperFailure;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * The managed policy layers an ingestion resolved, together with the per-layer outcome flags a host needs to decide how much to trust the result. The flags are reported rather than folded into the settings because a host must be able to distinguish an unmanaged device from a managed device whose policy could not be confirmed, and apply its fail-closed gates in the second case.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ManagedSettingsIngestResult.$NullableDeserializer.class)
record ManagedSettingsIngestResult(
    /** The effective managed settings composed from every resolved layer. Omitted when no layer set anything, so an unmanaged host locks nothing. */
    @JsonProperty("settings") Object settings,
    /** The effective managed permissions layer, when the resolved policy carries one. */
    @JsonProperty("permissions") Object permissions,
    /** The server fetch failed and no cached entry stood in for it, so the server layer is unconfirmed and fail-closed gates apply. */
    @JsonProperty("serverFetchFailed") Boolean serverFetchFailed,
    /** No usable token was available, so no server fetch was attempted. */
    @JsonProperty("serverTokenUnavailable") Boolean serverTokenUnavailable,
    /** The error that made the server fetch fail, when one did. */
    @JsonProperty("serverFetchError") String serverFetchError,
    /** How the server layer was resolved. */
    @JsonProperty("serverResolution") ManagedSettingsServerPolicyResolution serverResolution,
    /** The server fetch failed in a way a later retry could succeed at, as opposed to a refusal that no retry will change. */
    @JsonProperty("serverRetryableFailure") Boolean serverRetryableFailure,
    /** A live fetch failed but a recent cached entry was served in its place, so `serverFetchFailed` stays false. Lets a consumer distinguish a clean fetch from a cache-backed fallback. */
    @JsonProperty("serverCacheFallback") Boolean serverCacheFallback,
    /** A server-managed policy is known to exist even though `serverResponse` is intentionally unset. Consumers should OR this with the presence of `serverResponse` when deciding whether the device is managed, so a fresh start during an outage still applies the fail-closed gates instead of treating the device as unmanaged. */
    @JsonProperty("serverSourceFound") Boolean serverSourceFound,
    /** The account's server-delivered policy, when one was resolved. */
    @JsonProperty("serverResponse") Object serverResponse,
    /** The sandbox object from a cached server policy that a forced refresh declined to serve in full. Folded back into the effective sandbox layer so an outage during a forced refresh still synthesizes the fail-closed sandbox floor from the administrator's known intent. */
    @JsonProperty("serverSandboxFloor") Object serverSandboxFloor,
    /** The server fetch failed while a policy demanded a fresh confirmation, which is the strict posture an enterprise opted into rather than an ordinary transient failure. */
    @JsonProperty("serverForcedRefreshFailure") Boolean serverForcedRefreshFailure,
    /** The server refused to deliver a policy because this client is too old to enforce it, carrying the user-facing sentence describing the refusal. No retry will help; the remedy is a client update. Gates that exist only because of managed policy should deny on this rather than falling back to their unmanaged default. */
    @JsonProperty("serverClientUpdateRequired") String serverClientUpdateRequired,
    /** Minimum client version requested by a successful response. Advisory only; the delivered policy remains active. */
    @JsonProperty("serverClientUpdateRequested") String serverClientUpdateRequested,
    /** The synthesized fail-closed permissions layer applied in place of a policy a refused client may not enforce. */
    @JsonProperty("serverPermissionsFloor") Object serverPermissionsFloor,
    /** The device layer discovered from the machine, when one exists. */
    @JsonProperty("deviceResponse") Object deviceResponse,
    /** The device layer could not be read, so it is unconfirmed rather than known-absent. */
    @JsonProperty("deviceLoadFailed") Boolean deviceLoadFailed,
    /** The sandbox floor contributed by the device layer. */
    @JsonProperty("deviceSandboxFloor") Object deviceSandboxFloor,
    /** A device-managed policy is known to exist. */
    @JsonProperty("deviceSourceFound") Boolean deviceSourceFound,
    /** Whether the device layer's sandbox intent could not be determined, so a consumer must not read its absence as permission to run unsandboxed. */
    @JsonProperty("deviceSandboxUndetermined") Boolean deviceSandboxUndetermined,
    /** The layer produced by the platform policy helper, when one ran. */
    @JsonProperty("policyHelperResponse") Object policyHelperResponse,
    /** Which channel registered the policy helper that produced the helper layer. */
    @JsonProperty("policyHelperAuthority") ManagedSettingsPolicyHelperAuthority policyHelperAuthority,
    /** The policy helper was configured but could not be run or its output could not be read. */
    @JsonProperty("policyHelperLoadFailed") Boolean policyHelperLoadFailed,
    /** Structured latest Policy Helper failure for this ingestion, when one occurred. */
    @JsonProperty("policyHelperFailure") PolicyHelperFailure policyHelperFailure,
    /** A policy helper is configured for this host. */
    @JsonProperty("policyHelperConfigured") Boolean policyHelperConfigured,
    /** The policy helper's failure must be treated as fail-closed rather than ignored. */
    @JsonProperty("policyHelperFailClosed") Boolean policyHelperFailClosed,
    /** Diagnostics emitted while resolving the layers, in the order they occurred. */
    @JsonProperty("logMessages") List<ManagedSettingsLogMessage> logMessages,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    ManagedSettingsIngestResult { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    ManagedSettingsIngestResult(Object settings, Object permissions, Boolean serverFetchFailed, Boolean serverTokenUnavailable, String serverFetchError, ManagedSettingsServerPolicyResolution serverResolution, Boolean serverRetryableFailure, Boolean serverCacheFallback, Boolean serverSourceFound, Object serverResponse, Object serverSandboxFloor, Boolean serverForcedRefreshFailure, String serverClientUpdateRequired, String serverClientUpdateRequested, Object serverPermissionsFloor, Object deviceResponse, Boolean deviceLoadFailed, Object deviceSandboxFloor, Boolean deviceSourceFound, Boolean deviceSandboxUndetermined, Object policyHelperResponse, ManagedSettingsPolicyHelperAuthority policyHelperAuthority, Boolean policyHelperLoadFailed, PolicyHelperFailure policyHelperFailure, Boolean policyHelperConfigured, Boolean policyHelperFailClosed, List<ManagedSettingsLogMessage> logMessages) {
        this(settings, permissions, serverFetchFailed, serverTokenUnavailable, serverFetchError, serverResolution, serverRetryableFailure, serverCacheFallback, serverSourceFound, serverResponse, serverSandboxFloor, serverForcedRefreshFailure, serverClientUpdateRequired, serverClientUpdateRequested, serverPermissionsFloor, deviceResponse, deviceLoadFailed, deviceSandboxFloor, deviceSourceFound, deviceSandboxUndetermined, policyHelperResponse, policyHelperAuthority, policyHelperLoadFailed, policyHelperFailure, policyHelperConfigured, policyHelperFailClosed, logMessages, java.util.Set.of());
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
        private Object settings;
        private boolean $presence0;
        private Object permissions;
        private boolean $presence1;
        private Boolean serverFetchFailed;
        private Boolean serverTokenUnavailable;
        private String serverFetchError;
        private ManagedSettingsServerPolicyResolution serverResolution;
        private Boolean serverRetryableFailure;
        private Boolean serverCacheFallback;
        private Boolean serverSourceFound;
        private Object serverResponse;
        private boolean $presence9;
        private Object serverSandboxFloor;
        private boolean $presence10;
        private Boolean serverForcedRefreshFailure;
        private String serverClientUpdateRequired;
        private String serverClientUpdateRequested;
        private Object serverPermissionsFloor;
        private boolean $presence14;
        private Object deviceResponse;
        private boolean $presence15;
        private Boolean deviceLoadFailed;
        private Object deviceSandboxFloor;
        private boolean $presence17;
        private Boolean deviceSourceFound;
        private Boolean deviceSandboxUndetermined;
        private Object policyHelperResponse;
        private boolean $presence20;
        private ManagedSettingsPolicyHelperAuthority policyHelperAuthority;
        private Boolean policyHelperLoadFailed;
        private PolicyHelperFailure policyHelperFailure;
        private Boolean policyHelperConfigured;
        private Boolean policyHelperFailClosed;
        private List<ManagedSettingsLogMessage> logMessages;

        /**
         * Sets the {@code settings} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder settings(Object value) {
            this.settings = value;
            this.$presence0 = true;
            return this;
        }

        /**
         * Sets the {@code permissions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder permissions(Object value) {
            this.permissions = value;
            this.$presence1 = true;
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
         * Sets the {@code serverTokenUnavailable} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder serverTokenUnavailable(Boolean value) {
            this.serverTokenUnavailable = value;
            return this;
        }

        /**
         * Sets the {@code serverFetchError} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder serverFetchError(String value) {
            this.serverFetchError = value;
            return this;
        }

        /**
         * Sets the {@code serverResolution} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder serverResolution(ManagedSettingsServerPolicyResolution value) {
            this.serverResolution = value;
            return this;
        }

        /**
         * Sets the {@code serverRetryableFailure} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder serverRetryableFailure(Boolean value) {
            this.serverRetryableFailure = value;
            return this;
        }

        /**
         * Sets the {@code serverCacheFallback} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder serverCacheFallback(Boolean value) {
            this.serverCacheFallback = value;
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
         * Sets the {@code serverResponse} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder serverResponse(Object value) {
            this.serverResponse = value;
            this.$presence9 = true;
            return this;
        }

        /**
         * Sets the {@code serverSandboxFloor} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder serverSandboxFloor(Object value) {
            this.serverSandboxFloor = value;
            this.$presence10 = true;
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
         * Sets the {@code serverClientUpdateRequired} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder serverClientUpdateRequired(String value) {
            this.serverClientUpdateRequired = value;
            return this;
        }

        /**
         * Sets the {@code serverClientUpdateRequested} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder serverClientUpdateRequested(String value) {
            this.serverClientUpdateRequested = value;
            return this;
        }

        /**
         * Sets the {@code serverPermissionsFloor} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder serverPermissionsFloor(Object value) {
            this.serverPermissionsFloor = value;
            this.$presence14 = true;
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
            this.$presence15 = true;
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
         * Sets the {@code deviceSandboxFloor} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder deviceSandboxFloor(Object value) {
            this.deviceSandboxFloor = value;
            this.$presence17 = true;
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
         * Sets the {@code policyHelperResponse} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder policyHelperResponse(Object value) {
            this.policyHelperResponse = value;
            this.$presence20 = true;
            return this;
        }

        /**
         * Sets the {@code policyHelperAuthority} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder policyHelperAuthority(ManagedSettingsPolicyHelperAuthority value) {
            this.policyHelperAuthority = value;
            return this;
        }

        /**
         * Sets the {@code policyHelperLoadFailed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder policyHelperLoadFailed(Boolean value) {
            this.policyHelperLoadFailed = value;
            return this;
        }

        /**
         * Sets the {@code policyHelperFailure} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder policyHelperFailure(PolicyHelperFailure value) {
            this.policyHelperFailure = value;
            return this;
        }

        /**
         * Sets the {@code policyHelperConfigured} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder policyHelperConfigured(Boolean value) {
            this.policyHelperConfigured = value;
            return this;
        }

        /**
         * Sets the {@code policyHelperFailClosed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder policyHelperFailClosed(Boolean value) {
            this.policyHelperFailClosed = value;
            return this;
        }

        /**
         * Sets the {@code logMessages} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder logMessages(List<ManagedSettingsLogMessage> value) {
            this.logMessages = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ManagedSettingsIngestResult build() {
            if (serverFetchFailed == null) throw new IllegalStateException("Missing required property: serverFetchFailed");
            if (serverTokenUnavailable == null) throw new IllegalStateException("Missing required property: serverTokenUnavailable");
            if (serverResolution == null) throw new IllegalStateException("Missing required property: serverResolution");
            if (serverRetryableFailure == null) throw new IllegalStateException("Missing required property: serverRetryableFailure");
            if (serverCacheFallback == null) throw new IllegalStateException("Missing required property: serverCacheFallback");
            if (serverSourceFound == null) throw new IllegalStateException("Missing required property: serverSourceFound");
            if (serverForcedRefreshFailure == null) throw new IllegalStateException("Missing required property: serverForcedRefreshFailure");
            if (deviceLoadFailed == null) throw new IllegalStateException("Missing required property: deviceLoadFailed");
            if (deviceSourceFound == null) throw new IllegalStateException("Missing required property: deviceSourceFound");
            if (deviceSandboxUndetermined == null) throw new IllegalStateException("Missing required property: deviceSandboxUndetermined");
            if (policyHelperLoadFailed == null) throw new IllegalStateException("Missing required property: policyHelperLoadFailed");
            if (policyHelperConfigured == null) throw new IllegalStateException("Missing required property: policyHelperConfigured");
            if (policyHelperFailClosed == null) throw new IllegalStateException("Missing required property: policyHelperFailClosed");
            if (logMessages == null) throw new IllegalStateException("Missing required property: logMessages");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence0 && settings == null) $explicitNulls.add("settings");
            if ($presence1 && permissions == null) $explicitNulls.add("permissions");
            if ($presence9 && serverResponse == null) $explicitNulls.add("serverResponse");
            if ($presence10 && serverSandboxFloor == null) $explicitNulls.add("serverSandboxFloor");
            if ($presence14 && serverPermissionsFloor == null) $explicitNulls.add("serverPermissionsFloor");
            if ($presence15 && deviceResponse == null) $explicitNulls.add("deviceResponse");
            if ($presence17 && deviceSandboxFloor == null) $explicitNulls.add("deviceSandboxFloor");
            if ($presence20 && policyHelperResponse == null) $explicitNulls.add("policyHelperResponse");
            return new ManagedSettingsIngestResult(settings, permissions, serverFetchFailed, serverTokenUnavailable, serverFetchError, serverResolution, serverRetryableFailure, serverCacheFallback, serverSourceFound, serverResponse, serverSandboxFloor, serverForcedRefreshFailure, serverClientUpdateRequired, serverClientUpdateRequested, serverPermissionsFloor, deviceResponse, deviceLoadFailed, deviceSandboxFloor, deviceSourceFound, deviceSandboxUndetermined, policyHelperResponse, policyHelperAuthority, policyHelperLoadFailed, policyHelperFailure, policyHelperConfigured, policyHelperFailClosed, logMessages, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("settings") && settings == null) values.put("settings", null);
        if ($explicitNulls.contains("permissions") && permissions == null) values.put("permissions", null);
        if ($explicitNulls.contains("serverResponse") && serverResponse == null) values.put("serverResponse", null);
        if ($explicitNulls.contains("serverSandboxFloor") && serverSandboxFloor == null) values.put("serverSandboxFloor", null);
        if ($explicitNulls.contains("serverPermissionsFloor") && serverPermissionsFloor == null) values.put("serverPermissionsFloor", null);
        if ($explicitNulls.contains("deviceResponse") && deviceResponse == null) values.put("deviceResponse", null);
        if ($explicitNulls.contains("deviceSandboxFloor") && deviceSandboxFloor == null) values.put("deviceSandboxFloor", null);
        if ($explicitNulls.contains("policyHelperResponse") && policyHelperResponse == null) values.put("policyHelperResponse", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<ManagedSettingsIngestResult> {
        @Override
        public ManagedSettingsIngestResult deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(ManagedSettingsIngestResult.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("settings") && tree.get("settings").isNull()) $explicitNulls.add("settings");
            if (tree.has("permissions") && tree.get("permissions").isNull()) $explicitNulls.add("permissions");
            if (tree.has("serverResponse") && tree.get("serverResponse").isNull()) $explicitNulls.add("serverResponse");
            if (tree.has("serverSandboxFloor") && tree.get("serverSandboxFloor").isNull()) $explicitNulls.add("serverSandboxFloor");
            if (tree.has("serverPermissionsFloor") && tree.get("serverPermissionsFloor").isNull()) $explicitNulls.add("serverPermissionsFloor");
            if (tree.has("deviceResponse") && tree.get("deviceResponse").isNull()) $explicitNulls.add("deviceResponse");
            if (tree.has("deviceSandboxFloor") && tree.get("deviceSandboxFloor").isNull()) $explicitNulls.add("deviceSandboxFloor");
            if (tree.has("policyHelperResponse") && tree.get("policyHelperResponse").isNull()) $explicitNulls.add("policyHelperResponse");
            return new ManagedSettingsIngestResult(readProperty(context, tree, "settings", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "permissions", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "serverFetchFailed", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "serverTokenUnavailable", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "serverFetchError", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "serverResolution", new com.fasterxml.jackson.core.type.TypeReference<ManagedSettingsServerPolicyResolution>() {}), readProperty(context, tree, "serverRetryableFailure", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "serverCacheFallback", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "serverSourceFound", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "serverResponse", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "serverSandboxFloor", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "serverForcedRefreshFailure", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "serverClientUpdateRequired", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "serverClientUpdateRequested", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "serverPermissionsFloor", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "deviceResponse", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "deviceLoadFailed", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "deviceSandboxFloor", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "deviceSourceFound", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "deviceSandboxUndetermined", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "policyHelperResponse", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "policyHelperAuthority", new com.fasterxml.jackson.core.type.TypeReference<ManagedSettingsPolicyHelperAuthority>() {}), readProperty(context, tree, "policyHelperLoadFailed", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "policyHelperFailure", new com.fasterxml.jackson.core.type.TypeReference<PolicyHelperFailure>() {}), readProperty(context, tree, "policyHelperConfigured", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "policyHelperFailClosed", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "logMessages", new com.fasterxml.jackson.core.type.TypeReference<List<ManagedSettingsLogMessage>>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
