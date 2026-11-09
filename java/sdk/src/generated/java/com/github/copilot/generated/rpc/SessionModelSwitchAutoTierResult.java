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
 * Immediate acknowledgement and Auto preference snapshot after a switch request. This result never implies that a pending preference committed.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SessionModelSwitchAutoTierResult.$NullableDeserializer.class)
public record SessionModelSwitchAutoTierResult(
    /** Immediate request status. `pending` means accepted but not committed. */
    @JsonProperty("status") ModelSwitchAutoTierStatus status,
    /** Auto preference currently committed for the session. */
    @JsonProperty("effectiveAutoTier") AutoTier effectiveAutoTier,
    /** Copilot Fusion tier that effectiveAutoTier serves, resolved from the session's Auto tier metadata. An open string: known values include `standard`, `high`, and `max`; other provider-defined values may appear. Omitted when no Auto preference is committed, it is not a Fusion tier, or Auto metadata has not been discovered (for example after cold resume, before session.model.list). Fusion fields cover only effective and pending preferences; resolve activatingAutoTier and supersededAutoTier against auto.tiers[].fusionTier from session.model.list. */
    @JsonProperty("effectiveFusionTier") String effectiveFusionTier,
    /** Latest unclaimed Auto preference waiting for a future user turn. */
    @JsonProperty("pendingAutoTier") AutoTier pendingAutoTier,
    /** Copilot Fusion tier that pendingAutoTier serves, resolved from the session's Auto tier metadata. An open string: known values include `standard`, `high`, and `max`; other provider-defined values may appear. Omitted when no preference is pending, it is not a Fusion tier, or Auto metadata has not been discovered (for example after cold resume, before session.model.list). */
    @JsonProperty("pendingFusionTier") String pendingFusionTier,
    /** Auto preference currently claimed by an in-progress activation. Null means the activation is returning to provider-default routing. */
    @JsonProperty("activatingAutoTier") AutoTier activatingAutoTier,
    /** Earlier unclaimed preference replaced by this request. This can be present with either status, including when selecting the effective preference cancels pending work. */
    @JsonProperty("supersededAutoTier") AutoTier supersededAutoTier,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionModelSwitchAutoTierResult { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionModelSwitchAutoTierResult(ModelSwitchAutoTierStatus status, AutoTier effectiveAutoTier, String effectiveFusionTier, AutoTier pendingAutoTier, String pendingFusionTier, AutoTier activatingAutoTier, AutoTier supersededAutoTier) {
        this(status, effectiveAutoTier, effectiveFusionTier, pendingAutoTier, pendingFusionTier, activatingAutoTier, supersededAutoTier, java.util.Set.of());
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
        private ModelSwitchAutoTierStatus status;
        private AutoTier effectiveAutoTier;
        private String effectiveFusionTier;
        private AutoTier pendingAutoTier;
        private boolean $presence3;
        private String pendingFusionTier;
        private AutoTier activatingAutoTier;
        private boolean $presence5;
        private AutoTier supersededAutoTier;
        private boolean $presence6;

        /**
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder status(ModelSwitchAutoTierStatus value) {
            this.status = value;
            return this;
        }

        /**
         * Sets the {@code effectiveAutoTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder effectiveAutoTier(AutoTier value) {
            this.effectiveAutoTier = value;
            return this;
        }

        /**
         * Sets the {@code effectiveFusionTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder effectiveFusionTier(String value) {
            this.effectiveFusionTier = value;
            return this;
        }

        /**
         * Sets the {@code pendingAutoTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder pendingAutoTier(AutoTier value) {
            this.pendingAutoTier = value;
            this.$presence3 = true;
            return this;
        }

        /**
         * Sets the {@code pendingFusionTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder pendingFusionTier(String value) {
            this.pendingFusionTier = value;
            return this;
        }

        /**
         * Sets the {@code activatingAutoTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder activatingAutoTier(AutoTier value) {
            this.activatingAutoTier = value;
            this.$presence5 = true;
            return this;
        }

        /**
         * Sets the {@code supersededAutoTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder supersededAutoTier(AutoTier value) {
            this.supersededAutoTier = value;
            this.$presence6 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionModelSwitchAutoTierResult build() {
            if (status == null) throw new IllegalStateException("Missing required property: status");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence3 && pendingAutoTier == null) $explicitNulls.add("pendingAutoTier");
            if ($presence5 && activatingAutoTier == null) $explicitNulls.add("activatingAutoTier");
            if ($presence6 && supersededAutoTier == null) $explicitNulls.add("supersededAutoTier");
            return new SessionModelSwitchAutoTierResult(status, effectiveAutoTier, effectiveFusionTier, pendingAutoTier, pendingFusionTier, activatingAutoTier, supersededAutoTier, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("pendingAutoTier") && pendingAutoTier == null) values.put("pendingAutoTier", null);
        if ($explicitNulls.contains("activatingAutoTier") && activatingAutoTier == null) values.put("activatingAutoTier", null);
        if ($explicitNulls.contains("supersededAutoTier") && supersededAutoTier == null) values.put("supersededAutoTier", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<SessionModelSwitchAutoTierResult> {
        @Override
        public SessionModelSwitchAutoTierResult deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(SessionModelSwitchAutoTierResult.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("pendingAutoTier") && tree.get("pendingAutoTier").isNull()) $explicitNulls.add("pendingAutoTier");
            if (tree.has("activatingAutoTier") && tree.get("activatingAutoTier").isNull()) $explicitNulls.add("activatingAutoTier");
            if (tree.has("supersededAutoTier") && tree.get("supersededAutoTier").isNull()) $explicitNulls.add("supersededAutoTier");
            return new SessionModelSwitchAutoTierResult(readProperty(context, tree, "status", new com.fasterxml.jackson.core.type.TypeReference<ModelSwitchAutoTierStatus>() {}), readProperty(context, tree, "effectiveAutoTier", new com.fasterxml.jackson.core.type.TypeReference<AutoTier>() {}), readProperty(context, tree, "effectiveFusionTier", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "pendingAutoTier", new com.fasterxml.jackson.core.type.TypeReference<AutoTier>() {}), readProperty(context, tree, "pendingFusionTier", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "activatingAutoTier", new com.fasterxml.jackson.core.type.TypeReference<AutoTier>() {}), readProperty(context, tree, "supersededAutoTier", new com.fasterxml.jackson.core.type.TypeReference<AutoTier>() {}), $explicitNulls);
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
     * @param status Immediate request status. `pending` means accepted but not committed.
     * @param effectiveAutoTier Auto preference currently committed for the session.
     * @param pendingAutoTier Latest unclaimed Auto preference waiting for a future user turn.
     * @param activatingAutoTier Auto preference currently claimed by an in-progress activation. Null means the activation is returning to provider-default routing.
     * @param supersededAutoTier Earlier unclaimed preference replaced by this request. This can be present with either status, including when selecting the effective preference cancels pending work.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionModelSwitchAutoTierResult(
        ModelSwitchAutoTierStatus status,
        AutoTier effectiveAutoTier,
        AutoTier pendingAutoTier,
        AutoTier activatingAutoTier,
        AutoTier supersededAutoTier
    ) {
        this(status, effectiveAutoTier, null, pendingAutoTier, null, activatingAutoTier, supersededAutoTier);
    }
}
