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
 * An account quota reading. Absence of a quantity is unknown, never zero.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ProviderQuotaState.$NullableDeserializer.class)
public record ProviderQuotaState(
    /** Provider/account whose service reported this state. */
    @JsonProperty("provider") ModelProviderRef provider,
    /** Service-owned quota identifier within this provider. */
    @JsonProperty("quotaId") String quotaId,
    /** Unit of entitledQuantity and availableQuantity. */
    @JsonProperty("unit") ProviderQuotaUnit unit,
    /** Whether the service permits access, independently of balance. */
    @JsonProperty("accessState") ProviderQuotaAccessState accessState,
    /** Service capacity state; missing quantities do not imply exhaustion. */
    @JsonProperty("capacityState") ProviderQuotaCapacityState capacityState,
    /** Whether this is a GET account reading or a pre-response admission observation. Observations are never merged across kinds. */
    @JsonProperty("observationKind") ProviderQuotaObservationKind observationKind,
    /** Whether quantities are authoritative budget measurements, advisory balances, or absent. Only authoritative budgets support percentage presentation. */
    @JsonProperty("quantityKind") ProviderQuotaQuantityKind quantityKind,
    /** Acquisition outcome. Unavailable/failed readings have no service verdict; access/capacity unavailable are compatibility placeholders only. */
    @JsonProperty("acquisitionStatus") ProviderQuotaAcquisitionStatus acquisitionStatus,
    /** When the runtime observed this reading, not a charge timestamp or guarantee that consumption has settled. */
    @JsonProperty("observedAt") String observedAt,
    /** HTTP status from acquisition, when available. */
    @JsonProperty("httpStatus") Long httpStatus,
    /** Service error code or client acquisition category, separate from the business-state reason. */
    @JsonProperty("acquisitionError") String acquisitionError,
    /** Optional metadata for an authoritative budget. Advisory balances and admission-only observations do not populate this. */
    @JsonProperty("budgetMetadata") ProviderQuotaBudgetMetadata budgetMetadata,
    /** Service-reported monthly consumption, independent of quota balances and per-call or session cost. Omitted when the service does not report monthly usage. */
    @JsonProperty("monthlyUsage") ProviderMonthlyUsage monthlyUsage,
    /** Key for the backwards-compatible snapshots projection, when the authoritative budget supports that contract. */
    @JsonProperty("compatibilityKey") String compatibilityKey,
    /** Explicit service admission flag, when reported. */
    @JsonProperty("hasQuota") Boolean hasQuota,
    /** Independently reported signed 64-bit entitlement. -1 is an unlimited sentinel, not a capacity-state rewrite; omission and null are preserved. */
    @JsonProperty("entitledQuantity") Long entitledQuantity,
    /** Independently reported signed 64-bit available quantity. Zero does not override the service access/capacity verdict; omission and null are preserved. */
    @JsonProperty("availableQuantity") Long availableQuantity,
    /** Service-reported explanation for the state. */
    @JsonProperty("reason") String reason,
    /** Service-reported quota source. */
    @JsonProperty("source") String source,
    /** Service name owning this reading. */
    @JsonProperty("service") String service,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ProviderQuotaState { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ProviderQuotaState(ModelProviderRef provider, String quotaId, ProviderQuotaUnit unit, ProviderQuotaAccessState accessState, ProviderQuotaCapacityState capacityState, ProviderQuotaObservationKind observationKind, ProviderQuotaQuantityKind quantityKind, ProviderQuotaAcquisitionStatus acquisitionStatus, String observedAt, Long httpStatus, String acquisitionError, ProviderQuotaBudgetMetadata budgetMetadata, ProviderMonthlyUsage monthlyUsage, String compatibilityKey, Boolean hasQuota, Long entitledQuantity, Long availableQuantity, String reason, String source, String service) {
        this(provider, quotaId, unit, accessState, capacityState, observationKind, quantityKind, acquisitionStatus, observedAt, httpStatus, acquisitionError, budgetMetadata, monthlyUsage, compatibilityKey, hasQuota, entitledQuantity, availableQuantity, reason, source, service, java.util.Set.of());
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
        private ModelProviderRef provider;
        private String quotaId;
        private ProviderQuotaUnit unit;
        private ProviderQuotaAccessState accessState;
        private ProviderQuotaCapacityState capacityState;
        private ProviderQuotaObservationKind observationKind;
        private ProviderQuotaQuantityKind quantityKind;
        private ProviderQuotaAcquisitionStatus acquisitionStatus;
        private String observedAt;
        private Long httpStatus;
        private String acquisitionError;
        private ProviderQuotaBudgetMetadata budgetMetadata;
        private ProviderMonthlyUsage monthlyUsage;
        private String compatibilityKey;
        private Boolean hasQuota;
        private boolean $presence14;
        private Long entitledQuantity;
        private boolean $presence15;
        private Long availableQuantity;
        private boolean $presence16;
        private String reason;
        private boolean $presence17;
        private String source;
        private boolean $presence18;
        private String service;
        private boolean $presence19;

        /**
         * Sets the {@code provider} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder provider(ModelProviderRef value) {
            this.provider = value;
            return this;
        }

        /**
         * Sets the {@code quotaId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder quotaId(String value) {
            this.quotaId = value;
            return this;
        }

        /**
         * Sets the {@code unit} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder unit(ProviderQuotaUnit value) {
            this.unit = value;
            return this;
        }

        /**
         * Sets the {@code accessState} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder accessState(ProviderQuotaAccessState value) {
            this.accessState = value;
            return this;
        }

        /**
         * Sets the {@code capacityState} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder capacityState(ProviderQuotaCapacityState value) {
            this.capacityState = value;
            return this;
        }

        /**
         * Sets the {@code observationKind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder observationKind(ProviderQuotaObservationKind value) {
            this.observationKind = value;
            return this;
        }

        /**
         * Sets the {@code quantityKind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder quantityKind(ProviderQuotaQuantityKind value) {
            this.quantityKind = value;
            return this;
        }

        /**
         * Sets the {@code acquisitionStatus} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder acquisitionStatus(ProviderQuotaAcquisitionStatus value) {
            this.acquisitionStatus = value;
            return this;
        }

        /**
         * Sets the {@code observedAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder observedAt(String value) {
            this.observedAt = value;
            return this;
        }

        /**
         * Sets the {@code httpStatus} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder httpStatus(Long value) {
            this.httpStatus = value;
            return this;
        }

        /**
         * Sets the {@code acquisitionError} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder acquisitionError(String value) {
            this.acquisitionError = value;
            return this;
        }

        /**
         * Sets the {@code budgetMetadata} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder budgetMetadata(ProviderQuotaBudgetMetadata value) {
            this.budgetMetadata = value;
            return this;
        }

        /**
         * Sets the {@code monthlyUsage} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder monthlyUsage(ProviderMonthlyUsage value) {
            this.monthlyUsage = value;
            return this;
        }

        /**
         * Sets the {@code compatibilityKey} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder compatibilityKey(String value) {
            this.compatibilityKey = value;
            return this;
        }

        /**
         * Sets the {@code hasQuota} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder hasQuota(Boolean value) {
            this.hasQuota = value;
            this.$presence14 = true;
            return this;
        }

        /**
         * Sets the {@code entitledQuantity} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder entitledQuantity(Long value) {
            this.entitledQuantity = value;
            this.$presence15 = true;
            return this;
        }

        /**
         * Sets the {@code availableQuantity} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder availableQuantity(Long value) {
            this.availableQuantity = value;
            this.$presence16 = true;
            return this;
        }

        /**
         * Sets the {@code reason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder reason(String value) {
            this.reason = value;
            this.$presence17 = true;
            return this;
        }

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder source(String value) {
            this.source = value;
            this.$presence18 = true;
            return this;
        }

        /**
         * Sets the {@code service} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder service(String value) {
            this.service = value;
            this.$presence19 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ProviderQuotaState build() {
            if (provider == null) throw new IllegalStateException("Missing required property: provider");
            if (quotaId == null) throw new IllegalStateException("Missing required property: quotaId");
            if (unit == null) throw new IllegalStateException("Missing required property: unit");
            if (accessState == null) throw new IllegalStateException("Missing required property: accessState");
            if (capacityState == null) throw new IllegalStateException("Missing required property: capacityState");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence14 && hasQuota == null) $explicitNulls.add("hasQuota");
            if ($presence15 && entitledQuantity == null) $explicitNulls.add("entitledQuantity");
            if ($presence16 && availableQuantity == null) $explicitNulls.add("availableQuantity");
            if ($presence17 && reason == null) $explicitNulls.add("reason");
            if ($presence18 && source == null) $explicitNulls.add("source");
            if ($presence19 && service == null) $explicitNulls.add("service");
            return new ProviderQuotaState(provider, quotaId, unit, accessState, capacityState, observationKind, quantityKind, acquisitionStatus, observedAt, httpStatus, acquisitionError, budgetMetadata, monthlyUsage, compatibilityKey, hasQuota, entitledQuantity, availableQuantity, reason, source, service, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("hasQuota") && hasQuota == null) values.put("hasQuota", null);
        if ($explicitNulls.contains("entitledQuantity") && entitledQuantity == null) values.put("entitledQuantity", null);
        if ($explicitNulls.contains("availableQuantity") && availableQuantity == null) values.put("availableQuantity", null);
        if ($explicitNulls.contains("reason") && reason == null) values.put("reason", null);
        if ($explicitNulls.contains("source") && source == null) values.put("source", null);
        if ($explicitNulls.contains("service") && service == null) values.put("service", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<ProviderQuotaState> {
        @Override
        public ProviderQuotaState deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(ProviderQuotaState.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("hasQuota") && tree.get("hasQuota").isNull()) $explicitNulls.add("hasQuota");
            if (tree.has("entitledQuantity") && tree.get("entitledQuantity").isNull()) $explicitNulls.add("entitledQuantity");
            if (tree.has("availableQuantity") && tree.get("availableQuantity").isNull()) $explicitNulls.add("availableQuantity");
            if (tree.has("reason") && tree.get("reason").isNull()) $explicitNulls.add("reason");
            if (tree.has("source") && tree.get("source").isNull()) $explicitNulls.add("source");
            if (tree.has("service") && tree.get("service").isNull()) $explicitNulls.add("service");
            return new ProviderQuotaState(readProperty(context, tree, "provider", new com.fasterxml.jackson.core.type.TypeReference<ModelProviderRef>() {}), readProperty(context, tree, "quotaId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "unit", new com.fasterxml.jackson.core.type.TypeReference<ProviderQuotaUnit>() {}), readProperty(context, tree, "accessState", new com.fasterxml.jackson.core.type.TypeReference<ProviderQuotaAccessState>() {}), readProperty(context, tree, "capacityState", new com.fasterxml.jackson.core.type.TypeReference<ProviderQuotaCapacityState>() {}), readProperty(context, tree, "observationKind", new com.fasterxml.jackson.core.type.TypeReference<ProviderQuotaObservationKind>() {}), readProperty(context, tree, "quantityKind", new com.fasterxml.jackson.core.type.TypeReference<ProviderQuotaQuantityKind>() {}), readProperty(context, tree, "acquisitionStatus", new com.fasterxml.jackson.core.type.TypeReference<ProviderQuotaAcquisitionStatus>() {}), readProperty(context, tree, "observedAt", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "httpStatus", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "acquisitionError", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "budgetMetadata", new com.fasterxml.jackson.core.type.TypeReference<ProviderQuotaBudgetMetadata>() {}), readProperty(context, tree, "monthlyUsage", new com.fasterxml.jackson.core.type.TypeReference<ProviderMonthlyUsage>() {}), readProperty(context, tree, "compatibilityKey", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "hasQuota", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "entitledQuantity", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "availableQuantity", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "reason", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "source", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "service", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
