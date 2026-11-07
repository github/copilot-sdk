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
 * The startup policy a resolution composed, the per-layer outcome flags a host needs to decide how much to trust it, and the enterprise sign-in verdict for the credential it ran against. The flags are reported rather than folded into the settings because a host must distinguish an unmanaged device from a managed device whose policy could not be confirmed, and apply its fail-closed gates in the second case.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ManagedSettingsResolveStartupPolicyResult.$NullableDeserializer.class)
record ManagedSettingsResolveStartupPolicyResult(
    /** How the current runtime resolved the server layer. */
    @JsonProperty("serverResolution") ManagedSettingsServerPolicyResolution serverResolution,
    /** Advisory minimum client version from a successful server response. */
    @JsonProperty("clientUpdateRequested") String clientUpdateRequested,
    /** The account's server-delivered policy, when one was resolved. */
    @JsonProperty("serverResponse") Object serverResponse,
    /** The device layer discovered from the machine, when one exists. */
    @JsonProperty("deviceResponse") Object deviceResponse,
    /** The sandbox floor contributed by the device layer. */
    @JsonProperty("deviceSandboxFloor") Object deviceSandboxFloor,
    /** The layer produced by the platform policy helper, when one ran. */
    @JsonProperty("policyHelperResponse") Object policyHelperResponse,
    /** The server fetch failed and no cached entry stood in for it, so the server layer is unconfirmed and fail-closed gates apply. */
    @JsonProperty("serverFetchFailed") Boolean serverFetchFailed,
    /** The device layer could not be read, so it is unconfirmed rather than known-absent. */
    @JsonProperty("deviceLoadFailed") Boolean deviceLoadFailed,
    /** The policy helper was configured but could not be run, or its output could not be read. */
    @JsonProperty("policyHelperLoadFailed") Boolean policyHelperLoadFailed,
    /** Structured latest Policy Helper failure for this startup resolution, when one occurred. */
    @JsonProperty("policyHelperFailure") PolicyHelperFailure policyHelperFailure,
    /** Telemetry describing which layers answered and how, for the host to report alongside its own startup events. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("telemetry") Object telemetry,
    /** The effective sandbox floor composed across every layer, which a host must apply rather than its own default. */
    @JsonProperty("sandboxFloor") Object sandboxFloor,
    /** Some layer of the policy could not be confirmed, so its absence must not be read as permission. Folds in a failed server fetch as well as an unreadable device layer. */
    @JsonProperty("policyUndetermined") Boolean policyUndetermined,
    /** The permissions layer specifically could not be confirmed, so permission gates apply their fail-closed posture. */
    @JsonProperty("permissionPolicyUndetermined") Boolean permissionPolicyUndetermined,
    /** A managed policy is known to apply to this host, from a discovered source or a resolved document. */
    @JsonProperty("managedPolicyPresent") Boolean managedPolicyPresent,
    /** The sandbox was enabled because policy could not be confirmed rather than because an administrator asked for it. Lets a host explain a sandbox the user did not configure. */
    @JsonProperty("sandboxEnabledByUndeterminedPolicy") Boolean sandboxEnabledByUndeterminedPolicy,
    /** The enterprise sign-in verdict for the credential this resolution ran against, evaluated over the effective policy so a server-delivered pin is honored alongside a device one. Computed on every resolution, including one that was not retained, so a host that declines to apply policy still cannot proceed past a pin. */
    @JsonProperty("forceLoginOrgs") ForceLoginOrgsDecision forceLoginOrgs,
    /** The server refused to deliver a policy because this client is too old to enforce it, carrying the user-facing sentence describing the refusal. No retry will help; the remedy is a client update. */
    @JsonProperty("clientUpdateRequired") String clientUpdateRequired,
    /** Warnings raised while resolving, in the order they occurred. */
    @JsonProperty("warnings") List<String> warnings,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    ManagedSettingsResolveStartupPolicyResult { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    ManagedSettingsResolveStartupPolicyResult(ManagedSettingsServerPolicyResolution serverResolution, String clientUpdateRequested, Object serverResponse, Object deviceResponse, Object deviceSandboxFloor, Object policyHelperResponse, Boolean serverFetchFailed, Boolean deviceLoadFailed, Boolean policyHelperLoadFailed, PolicyHelperFailure policyHelperFailure, Object telemetry, Object sandboxFloor, Boolean policyUndetermined, Boolean permissionPolicyUndetermined, Boolean managedPolicyPresent, Boolean sandboxEnabledByUndeterminedPolicy, ForceLoginOrgsDecision forceLoginOrgs, String clientUpdateRequired, List<String> warnings) {
        this(serverResolution, clientUpdateRequested, serverResponse, deviceResponse, deviceSandboxFloor, policyHelperResponse, serverFetchFailed, deviceLoadFailed, policyHelperLoadFailed, policyHelperFailure, telemetry, sandboxFloor, policyUndetermined, permissionPolicyUndetermined, managedPolicyPresent, sandboxEnabledByUndeterminedPolicy, forceLoginOrgs, clientUpdateRequired, warnings, java.util.Set.of());
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
        private ManagedSettingsServerPolicyResolution serverResolution;
        private String clientUpdateRequested;
        private Object serverResponse;
        private boolean $presence2;
        private Object deviceResponse;
        private boolean $presence3;
        private Object deviceSandboxFloor;
        private boolean $presence4;
        private Object policyHelperResponse;
        private boolean $presence5;
        private Boolean serverFetchFailed;
        private Boolean deviceLoadFailed;
        private Boolean policyHelperLoadFailed;
        private PolicyHelperFailure policyHelperFailure;
        private Object telemetry;
        private boolean $presence10;
        private Object sandboxFloor;
        private boolean $presence11;
        private Boolean policyUndetermined;
        private Boolean permissionPolicyUndetermined;
        private Boolean managedPolicyPresent;
        private Boolean sandboxEnabledByUndeterminedPolicy;
        private ForceLoginOrgsDecision forceLoginOrgs;
        private String clientUpdateRequired;
        private List<String> warnings;

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
         * Sets the {@code clientUpdateRequested} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder clientUpdateRequested(String value) {
            this.clientUpdateRequested = value;
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
         * Sets the {@code deviceSandboxFloor} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder deviceSandboxFloor(Object value) {
            this.deviceSandboxFloor = value;
            this.$presence4 = true;
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
            this.$presence5 = true;
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
         * Sets the {@code telemetry} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder telemetry(Object value) {
            this.telemetry = value;
            this.$presence10 = true;
            return this;
        }

        /**
         * Sets the {@code sandboxFloor} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sandboxFloor(Object value) {
            this.sandboxFloor = value;
            this.$presence11 = true;
            return this;
        }

        /**
         * Sets the {@code policyUndetermined} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder policyUndetermined(Boolean value) {
            this.policyUndetermined = value;
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
         * Sets the {@code forceLoginOrgs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder forceLoginOrgs(ForceLoginOrgsDecision value) {
            this.forceLoginOrgs = value;
            return this;
        }

        /**
         * Sets the {@code clientUpdateRequired} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder clientUpdateRequired(String value) {
            this.clientUpdateRequired = value;
            return this;
        }

        /**
         * Sets the {@code warnings} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder warnings(List<String> value) {
            this.warnings = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ManagedSettingsResolveStartupPolicyResult build() {
            if (serverResolution == null) throw new IllegalStateException("Missing required property: serverResolution");
            if (serverFetchFailed == null) throw new IllegalStateException("Missing required property: serverFetchFailed");
            if (deviceLoadFailed == null) throw new IllegalStateException("Missing required property: deviceLoadFailed");
            if (policyHelperLoadFailed == null) throw new IllegalStateException("Missing required property: policyHelperLoadFailed");
            if (!$presence10) throw new IllegalStateException("Missing required property: telemetry");
            if (policyUndetermined == null) throw new IllegalStateException("Missing required property: policyUndetermined");
            if (permissionPolicyUndetermined == null) throw new IllegalStateException("Missing required property: permissionPolicyUndetermined");
            if (managedPolicyPresent == null) throw new IllegalStateException("Missing required property: managedPolicyPresent");
            if (sandboxEnabledByUndeterminedPolicy == null) throw new IllegalStateException("Missing required property: sandboxEnabledByUndeterminedPolicy");
            if (forceLoginOrgs == null) throw new IllegalStateException("Missing required property: forceLoginOrgs");
            if (warnings == null) throw new IllegalStateException("Missing required property: warnings");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence2 && serverResponse == null) $explicitNulls.add("serverResponse");
            if ($presence3 && deviceResponse == null) $explicitNulls.add("deviceResponse");
            if ($presence4 && deviceSandboxFloor == null) $explicitNulls.add("deviceSandboxFloor");
            if ($presence5 && policyHelperResponse == null) $explicitNulls.add("policyHelperResponse");
            if ($presence11 && sandboxFloor == null) $explicitNulls.add("sandboxFloor");
            return new ManagedSettingsResolveStartupPolicyResult(serverResolution, clientUpdateRequested, serverResponse, deviceResponse, deviceSandboxFloor, policyHelperResponse, serverFetchFailed, deviceLoadFailed, policyHelperLoadFailed, policyHelperFailure, telemetry, sandboxFloor, policyUndetermined, permissionPolicyUndetermined, managedPolicyPresent, sandboxEnabledByUndeterminedPolicy, forceLoginOrgs, clientUpdateRequired, warnings, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("serverResponse") && serverResponse == null) values.put("serverResponse", null);
        if ($explicitNulls.contains("deviceResponse") && deviceResponse == null) values.put("deviceResponse", null);
        if ($explicitNulls.contains("deviceSandboxFloor") && deviceSandboxFloor == null) values.put("deviceSandboxFloor", null);
        if ($explicitNulls.contains("policyHelperResponse") && policyHelperResponse == null) values.put("policyHelperResponse", null);
        if ($explicitNulls.contains("sandboxFloor") && sandboxFloor == null) values.put("sandboxFloor", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<ManagedSettingsResolveStartupPolicyResult> {
        @Override
        public ManagedSettingsResolveStartupPolicyResult deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(ManagedSettingsResolveStartupPolicyResult.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("serverResponse") && tree.get("serverResponse").isNull()) $explicitNulls.add("serverResponse");
            if (tree.has("deviceResponse") && tree.get("deviceResponse").isNull()) $explicitNulls.add("deviceResponse");
            if (tree.has("deviceSandboxFloor") && tree.get("deviceSandboxFloor").isNull()) $explicitNulls.add("deviceSandboxFloor");
            if (tree.has("policyHelperResponse") && tree.get("policyHelperResponse").isNull()) $explicitNulls.add("policyHelperResponse");
            if (tree.has("sandboxFloor") && tree.get("sandboxFloor").isNull()) $explicitNulls.add("sandboxFloor");
            return new ManagedSettingsResolveStartupPolicyResult(readProperty(context, tree, "serverResolution", new com.fasterxml.jackson.core.type.TypeReference<ManagedSettingsServerPolicyResolution>() {}), readProperty(context, tree, "clientUpdateRequested", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "serverResponse", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "deviceResponse", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "deviceSandboxFloor", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "policyHelperResponse", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "serverFetchFailed", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "deviceLoadFailed", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "policyHelperLoadFailed", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "policyHelperFailure", new com.fasterxml.jackson.core.type.TypeReference<PolicyHelperFailure>() {}), readProperty(context, tree, "telemetry", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "sandboxFloor", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "policyUndetermined", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "permissionPolicyUndetermined", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "managedPolicyPresent", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "sandboxEnabledByUndeterminedPolicy", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "forceLoginOrgs", new com.fasterxml.jackson.core.type.TypeReference<ForceLoginOrgsDecision>() {}), readProperty(context, tree, "clientUpdateRequired", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "warnings", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
