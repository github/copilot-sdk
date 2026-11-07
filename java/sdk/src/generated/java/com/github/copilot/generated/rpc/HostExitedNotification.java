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
 * Reports a supervised listener's hosting-task termination and cleanup outcome.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = HostExitedNotification.$NullableDeserializer.class)
public record HostExitedNotification(
    /** Listener UUID. */
    @JsonProperty("hostId") String hostId,
    /** Cause of termination. */
    @JsonProperty("reason") HostExitReason reason,
    /** Process exit status when available; absent for in-process listener tasks. */
    @JsonProperty("exitCode") Long exitCode,
    /** Explicit startup or teardown failure, when present. */
    @JsonProperty("error") String error,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public HostExitedNotification { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public HostExitedNotification(String hostId, HostExitReason reason, Long exitCode, String error) {
        this(hostId, reason, exitCode, error, java.util.Set.of());
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
        private String hostId;
        private HostExitReason reason;
        private Long exitCode;
        private boolean $presence2;
        private String error;
        private boolean $presence3;

        /**
         * Sets the {@code hostId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder hostId(String value) {
            this.hostId = value;
            return this;
        }

        /**
         * Sets the {@code reason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder reason(HostExitReason value) {
            this.reason = value;
            return this;
        }

        /**
         * Sets the {@code exitCode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder exitCode(Long value) {
            this.exitCode = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Sets the {@code error} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder error(String value) {
            this.error = value;
            this.$presence3 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public HostExitedNotification build() {
            if (hostId == null) throw new IllegalStateException("Missing required property: hostId");
            if (reason == null) throw new IllegalStateException("Missing required property: reason");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence2 && exitCode == null) $explicitNulls.add("exitCode");
            if ($presence3 && error == null) $explicitNulls.add("error");
            return new HostExitedNotification(hostId, reason, exitCode, error, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("exitCode") && exitCode == null) values.put("exitCode", null);
        if ($explicitNulls.contains("error") && error == null) values.put("error", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<HostExitedNotification> {
        @Override
        public HostExitedNotification deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(HostExitedNotification.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("exitCode") && tree.get("exitCode").isNull()) $explicitNulls.add("exitCode");
            if (tree.has("error") && tree.get("error").isNull()) $explicitNulls.add("error");
            return new HostExitedNotification(readProperty(context, tree, "hostId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "reason", new com.fasterxml.jackson.core.type.TypeReference<HostExitReason>() {}), readProperty(context, tree, "exitCode", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "error", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
