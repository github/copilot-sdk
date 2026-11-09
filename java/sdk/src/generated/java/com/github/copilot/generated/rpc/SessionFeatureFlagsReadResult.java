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
 * One authoritative snapshot and the requested evaluations.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SessionFeatureFlagsReadResult.$NullableDeserializer.class)
public record SessionFeatureFlagsReadResult(
    /** The state used by these evaluations. */
    @JsonProperty("snapshot") FeatureFlagServiceSnapshot snapshot,
    /** Requested feature values. Unknown or unset values are absent. */
    @JsonProperty("flags") Map<String, Boolean> flags,
    /** Explicit override results; null means neither source supplies a boolean. */
    @JsonProperty("overrides") List<Boolean> overrides,
    /** Redacted runtime decisions for UI presentation, present only for an available display read. */
    @JsonProperty("displayState") Object displayState,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionFeatureFlagsReadResult { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionFeatureFlagsReadResult(FeatureFlagServiceSnapshot snapshot, Map<String, Boolean> flags, List<Boolean> overrides, Object displayState) {
        this(snapshot, flags, overrides, displayState, java.util.Set.of());
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
        private FeatureFlagServiceSnapshot snapshot;
        private Map<String, Boolean> flags;
        private List<Boolean> overrides;
        private Object displayState;
        private boolean $presence3;

        /**
         * Sets the {@code snapshot} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder snapshot(FeatureFlagServiceSnapshot value) {
            this.snapshot = value;
            return this;
        }

        /**
         * Sets the {@code flags} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder flags(Map<String, Boolean> value) {
            this.flags = value;
            return this;
        }

        /**
         * Sets the {@code overrides} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder overrides(List<Boolean> value) {
            this.overrides = value;
            return this;
        }

        /**
         * Sets the {@code displayState} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder displayState(Object value) {
            this.displayState = value;
            this.$presence3 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionFeatureFlagsReadResult build() {
            if (snapshot == null) throw new IllegalStateException("Missing required property: snapshot");
            if (flags == null) throw new IllegalStateException("Missing required property: flags");
            if (overrides == null) throw new IllegalStateException("Missing required property: overrides");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence3 && displayState == null) $explicitNulls.add("displayState");
            return new SessionFeatureFlagsReadResult(snapshot, flags, overrides, displayState, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("displayState") && displayState == null) values.put("displayState", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<SessionFeatureFlagsReadResult> {
        @Override
        public SessionFeatureFlagsReadResult deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(SessionFeatureFlagsReadResult.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("displayState") && tree.get("displayState").isNull()) $explicitNulls.add("displayState");
            return new SessionFeatureFlagsReadResult(readProperty(context, tree, "snapshot", new com.fasterxml.jackson.core.type.TypeReference<FeatureFlagServiceSnapshot>() {}), readProperty(context, tree, "flags", new com.fasterxml.jackson.core.type.TypeReference<Map<String, Boolean>>() {}), readProperty(context, tree, "overrides", new com.fasterxml.jackson.core.type.TypeReference<List<Boolean>>() {}), readProperty(context, tree, "displayState", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }

    /**
     * Creates a record with the components it had before later optional fields were added.
     * @deprecated Use named construction via {@link #builder()}.
     * @param snapshot The state used by these evaluations.
     * @param flags Requested feature values. Unknown or unset values are absent.
     * @param overrides Explicit override results; null means neither source supplies a boolean.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionFeatureFlagsReadResult(
        FeatureFlagServiceSnapshot snapshot,
        Map<String, Boolean> flags,
        List<Boolean> overrides
    ) {
        this(snapshot, flags, overrides, null);
    }
}
