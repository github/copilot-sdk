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
import com.github.copilot.generated.ManagedPermissionsContext;
import com.github.copilot.generated.PolicyHelperCapabilities;
import com.github.copilot.generated.PolicyHelperFailure;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Effective enterprise managed settings and contributing channels. Session events report applied policy; sessionless resolve reports an account/device snapshot, and compose reports a non-applying preview of candidate documents. Device values take precedence over server values, then the policy helper, per ordinary key, while permissions compose restrictively. Session-local SDK-client policy is included only in session results. Marked experimental while the managed-settings surface stabilizes.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SessionManagedSettingsGetResult.$NullableDeserializer.class)
public record SessionManagedSettingsGetResult(
    /** Reusable retained managed permission policy for managedSettings.permissions.evaluate, including source composition and session-local injection. Provided by live session snapshots; absent in older events. Refresh the context when managed policy changes. */
    @JsonProperty("permissionsContext") ManagedPermissionsContext permissionsContext,
    /** Channel summary: `server`, `device`, `client`, or `policyHelper` when exactly one channel contributed; `mixed` when multiple channels contributed; otherwise `none`. Consult the per-channel booleans for exact provenance. */
    @JsonProperty("source") ManagedSettingsResolvedSource source,
    /** Whether the server (account/org) managed-settings layer was present */
    @JsonProperty("serverManaged") Boolean serverManaged,
    /** Whether an actual device MDM/plist/registry/file managed-settings layer was present */
    @JsonProperty("deviceManaged") Boolean deviceManaged,
    /** Whether a session-local permissions layer injected by the SDK host was present */
    @JsonProperty("clientManaged") Boolean clientManaged,
    /** Whether the policy-helper managed-settings layer was present. The policy helper is the weakest channel: it fills keys no enterprise source set and can never replace one. */
    @JsonProperty("policyHelperManaged") Boolean policyHelperManaged,
    /** The latest unresolved Policy Helper failure for this exact policy context. It remains present across retained-policy refresh failures and clears only after fresh success or authoritative removal. */
    @JsonProperty("policyHelperFailure") PolicyHelperFailure policyHelperFailure,
    /** Explicit Policy Helper structured-failure and recheck capabilities supported by this runtime. */
    @JsonProperty("policyHelperCapabilities") PolicyHelperCapabilities policyHelperCapabilities,
    /** Whether managed policy could not be determined (e.g. a failed server fetch) and unresolved-policy safeguards remain active. This does not by itself disable bypass-permissions; `bypassPermissionsDisabled` reports only an explicit policy restriction. */
    @JsonProperty("failClosed") Boolean failClosed,
    /** Whether the effective sandbox policy forces the sandbox on *only* because managed policy could not be determined, rather than because the policy requires it. Lets clients tell a user whose `--no-sandbox` was overridden that the sandbox stayed on as a fail-closed fallback, instead of attributing it to an administrator who set no such policy. */
    @JsonProperty("sandboxEnabledByUndeterminedPolicy") Boolean sandboxEnabledByUndeterminedPolicy,
    /** Whether an explicit enterprise policy restriction disables bypass-permissions ("yolo") mode for this session. Deny-wins across layers; an unresolved policy does not force this on by itself. */
    @JsonProperty("bypassPermissionsDisabled") Boolean bypassPermissionsDisabled,
    /** Whether at least two managed sources supplied permission allowlists, so enforcement intersects them and the flattened settings payload omits `permissions.allow`. */
    @JsonProperty("permissionsAllowIntersected") Boolean permissionsAllowIntersected,
    /** The setting keys under enterprise management in the effective managed settings (e.g. `model`, `enabledPlugins`, `permissions`). Empty when no managed settings are in force. */
    @JsonProperty("managedKeys") List<String> managedKeys,
    /** The effective (resolved) managed settings values, so clients can render exactly what is enforced. Absent when no managed policy is in force. */
    @JsonProperty("settings") Object settings,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionManagedSettingsGetResult { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionManagedSettingsGetResult(ManagedPermissionsContext permissionsContext, ManagedSettingsResolvedSource source, Boolean serverManaged, Boolean deviceManaged, Boolean clientManaged, Boolean policyHelperManaged, PolicyHelperFailure policyHelperFailure, PolicyHelperCapabilities policyHelperCapabilities, Boolean failClosed, Boolean sandboxEnabledByUndeterminedPolicy, Boolean bypassPermissionsDisabled, Boolean permissionsAllowIntersected, List<String> managedKeys, Object settings) {
        this(permissionsContext, source, serverManaged, deviceManaged, clientManaged, policyHelperManaged, policyHelperFailure, policyHelperCapabilities, failClosed, sandboxEnabledByUndeterminedPolicy, bypassPermissionsDisabled, permissionsAllowIntersected, managedKeys, settings, java.util.Set.of());
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
        private ManagedPermissionsContext permissionsContext;
        private ManagedSettingsResolvedSource source;
        private Boolean serverManaged;
        private Boolean deviceManaged;
        private Boolean clientManaged;
        private Boolean policyHelperManaged;
        private PolicyHelperFailure policyHelperFailure;
        private PolicyHelperCapabilities policyHelperCapabilities;
        private Boolean failClosed;
        private Boolean sandboxEnabledByUndeterminedPolicy;
        private Boolean bypassPermissionsDisabled;
        private Boolean permissionsAllowIntersected;
        private List<String> managedKeys;
        private Object settings;
        private boolean $presence13;

        /**
         * Sets the {@code permissionsContext} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder permissionsContext(ManagedPermissionsContext value) {
            this.permissionsContext = value;
            return this;
        }

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder source(ManagedSettingsResolvedSource value) {
            this.source = value;
            return this;
        }

        /**
         * Sets the {@code serverManaged} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder serverManaged(Boolean value) {
            this.serverManaged = value;
            return this;
        }

        /**
         * Sets the {@code deviceManaged} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder deviceManaged(Boolean value) {
            this.deviceManaged = value;
            return this;
        }

        /**
         * Sets the {@code clientManaged} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder clientManaged(Boolean value) {
            this.clientManaged = value;
            return this;
        }

        /**
         * Sets the {@code policyHelperManaged} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder policyHelperManaged(Boolean value) {
            this.policyHelperManaged = value;
            return this;
        }

        /**
         * Sets the {@code policyHelperFailure} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder policyHelperFailure(PolicyHelperFailure value) {
            this.policyHelperFailure = value;
            return this;
        }

        /**
         * Sets the {@code policyHelperCapabilities} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder policyHelperCapabilities(PolicyHelperCapabilities value) {
            this.policyHelperCapabilities = value;
            return this;
        }

        /**
         * Sets the {@code failClosed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder failClosed(Boolean value) {
            this.failClosed = value;
            return this;
        }

        /**
         * Sets the {@code sandboxEnabledByUndeterminedPolicy} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sandboxEnabledByUndeterminedPolicy(Boolean value) {
            this.sandboxEnabledByUndeterminedPolicy = value;
            return this;
        }

        /**
         * Sets the {@code bypassPermissionsDisabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder bypassPermissionsDisabled(Boolean value) {
            this.bypassPermissionsDisabled = value;
            return this;
        }

        /**
         * Sets the {@code permissionsAllowIntersected} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder permissionsAllowIntersected(Boolean value) {
            this.permissionsAllowIntersected = value;
            return this;
        }

        /**
         * Sets the {@code managedKeys} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder managedKeys(List<String> value) {
            this.managedKeys = value;
            return this;
        }

        /**
         * Sets the {@code settings} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder settings(Object value) {
            this.settings = value;
            this.$presence13 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionManagedSettingsGetResult build() {
            if (source == null) throw new IllegalStateException("Missing required property: source");
            if (serverManaged == null) throw new IllegalStateException("Missing required property: serverManaged");
            if (deviceManaged == null) throw new IllegalStateException("Missing required property: deviceManaged");
            if (failClosed == null) throw new IllegalStateException("Missing required property: failClosed");
            if (bypassPermissionsDisabled == null) throw new IllegalStateException("Missing required property: bypassPermissionsDisabled");
            if (managedKeys == null) throw new IllegalStateException("Missing required property: managedKeys");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence13 && settings == null) $explicitNulls.add("settings");
            return new SessionManagedSettingsGetResult(permissionsContext, source, serverManaged, deviceManaged, clientManaged, policyHelperManaged, policyHelperFailure, policyHelperCapabilities, failClosed, sandboxEnabledByUndeterminedPolicy, bypassPermissionsDisabled, permissionsAllowIntersected, managedKeys, settings, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("settings") && settings == null) values.put("settings", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<SessionManagedSettingsGetResult> {
        @Override
        public SessionManagedSettingsGetResult deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(SessionManagedSettingsGetResult.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("settings") && tree.get("settings").isNull()) $explicitNulls.add("settings");
            return new SessionManagedSettingsGetResult(readProperty(context, tree, "permissionsContext", new com.fasterxml.jackson.core.type.TypeReference<ManagedPermissionsContext>() {}), readProperty(context, tree, "source", new com.fasterxml.jackson.core.type.TypeReference<ManagedSettingsResolvedSource>() {}), readProperty(context, tree, "serverManaged", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "deviceManaged", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "clientManaged", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "policyHelperManaged", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "policyHelperFailure", new com.fasterxml.jackson.core.type.TypeReference<PolicyHelperFailure>() {}), readProperty(context, tree, "policyHelperCapabilities", new com.fasterxml.jackson.core.type.TypeReference<PolicyHelperCapabilities>() {}), readProperty(context, tree, "failClosed", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "sandboxEnabledByUndeterminedPolicy", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "bypassPermissionsDisabled", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "permissionsAllowIntersected", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "managedKeys", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "settings", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }

    /**
     * Retains the constructor published in Java SDK 1.0.28.
     * @deprecated Use named construction via {@link #builder()}.
     * @param permissionsContext Reusable retained managed permission policy for managedSettings.permissions.evaluate, including source composition and session-local injection. Provided by live session snapshots; absent in older events. Refresh the context when managed policy changes.
     * @param source Channel summary: `server`, `device`, `client`, or `policyHelper` when exactly one channel contributed; `mixed` when multiple channels contributed; otherwise `none`. Consult the per-channel booleans for exact provenance.
     * @param serverManaged Whether the server (account/org) managed-settings layer was present
     * @param deviceManaged Whether an actual device MDM/plist/registry/file managed-settings layer was present
     * @param clientManaged Whether a session-local permissions layer injected by the SDK host was present
     * @param policyHelperManaged Whether the policy-helper managed-settings layer was present. The policy helper is the weakest channel: it fills keys no enterprise source set and can never replace one.
     * @param failClosed Whether managed policy could not be determined (e.g. a failed server fetch) and unresolved-policy safeguards remain active. This does not by itself disable bypass-permissions; `bypassPermissionsDisabled` reports only an explicit policy restriction.
     * @param sandboxEnabledByUndeterminedPolicy Whether the effective sandbox policy forces the sandbox on *only* because managed policy could not be determined, rather than because the policy requires it. Lets clients tell a user whose `--no-sandbox` was overridden that the sandbox stayed on as a fail-closed fallback, instead of attributing it to an administrator who set no such policy.
     * @param bypassPermissionsDisabled Whether an explicit enterprise policy restriction disables bypass-permissions ("yolo") mode for this session. Deny-wins across layers; an unresolved policy does not force this on by itself.
     * @param permissionsAllowIntersected Whether at least two managed sources supplied permission allowlists, so enforcement intersects them and the flattened settings payload omits `permissions.allow`.
     * @param managedKeys The setting keys under enterprise management in the effective managed settings (e.g. `model`, `enabledPlugins`, `permissions`). Empty when no managed settings are in force.
     * @param settings The effective (resolved) managed settings values, so clients can render exactly what is enforced. Absent when no managed policy is in force.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionManagedSettingsGetResult(
        ManagedPermissionsContext permissionsContext,
        ManagedSettingsResolvedSource source,
        Boolean serverManaged,
        Boolean deviceManaged,
        Boolean clientManaged,
        Boolean policyHelperManaged,
        Boolean failClosed,
        Boolean sandboxEnabledByUndeterminedPolicy,
        Boolean bypassPermissionsDisabled,
        Boolean permissionsAllowIntersected,
        List<String> managedKeys,
        Object settings
    ) {
        this(permissionsContext, source, serverManaged, deviceManaged, clientManaged, policyHelperManaged, null, null, failClosed, sandboxEnabledByUndeterminedPolicy, bypassPermissionsDisabled, permissionsAllowIntersected, managedKeys, settings);
    }
}
