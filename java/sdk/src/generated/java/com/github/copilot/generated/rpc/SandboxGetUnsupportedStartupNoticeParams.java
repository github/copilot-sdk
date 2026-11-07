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
 * The managed sandbox policy in force, which selects which remedy the notice offers.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SandboxGetUnsupportedStartupNoticeParams.$NullableDeserializer.class)
record SandboxGetUnsupportedStartupNoticeParams(
    /** The managed `sandbox` slice, when a policy supplies one. The notice treats the sandbox as enforced only when this both enables it and sets `failIfUnavailable`, because a managed sandbox that tolerates being unavailable still leaves the user able to turn it off. */
    @JsonProperty("managedSandbox") Object managedSandbox,
    /** True when the sandbox stayed on only because the managed policy could not be determined. Distinguishes a policy that positively enforces the sandbox from one that merely failed to resolve, which is the difference between telling the user to contact an administrator and telling them to wait. Defaults to false. */
    @JsonProperty("enabledByUndeterminedPolicy") Boolean enabledByUndeterminedPolicy,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SandboxGetUnsupportedStartupNoticeParams { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SandboxGetUnsupportedStartupNoticeParams(Object managedSandbox, Boolean enabledByUndeterminedPolicy) {
        this(managedSandbox, enabledByUndeterminedPolicy, java.util.Set.of());
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
        private Object managedSandbox;
        private boolean $presence0;
        private Boolean enabledByUndeterminedPolicy;

        /**
         * Sets the {@code managedSandbox} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder managedSandbox(Object value) {
            this.managedSandbox = value;
            this.$presence0 = true;
            return this;
        }

        /**
         * Sets the {@code enabledByUndeterminedPolicy} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder enabledByUndeterminedPolicy(Boolean value) {
            this.enabledByUndeterminedPolicy = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SandboxGetUnsupportedStartupNoticeParams build() {
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence0 && managedSandbox == null) $explicitNulls.add("managedSandbox");
            return new SandboxGetUnsupportedStartupNoticeParams(managedSandbox, enabledByUndeterminedPolicy, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("managedSandbox") && managedSandbox == null) values.put("managedSandbox", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<SandboxGetUnsupportedStartupNoticeParams> {
        @Override
        public SandboxGetUnsupportedStartupNoticeParams deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(SandboxGetUnsupportedStartupNoticeParams.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("managedSandbox") && tree.get("managedSandbox").isNull()) $explicitNulls.add("managedSandbox");
            return new SandboxGetUnsupportedStartupNoticeParams(readProperty(context, tree, "managedSandbox", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "enabledByUndeterminedPolicy", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
