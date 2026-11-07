/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * A normalised, inert description of what installing an MCP server would involve. Carries no raw card, no install specification, and no secret value.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpInstallPlan(
    /** Opaque, runtime-instance scoped, TTL-bound, single-use handle for this plan. Rejected when stale, replayed, or presented to a different runtime instance. Never logged. */
    @JsonProperty("planHandle") String planHandle,
    /** ISO 8601 timestamp after which the plan handle is stale and will be rejected. Abandoning a plan needs no call: an unused handle simply expires, so cancellation before commit is side-effect free. */
    @JsonProperty("planHandleExpiresAt") String planHandleExpiresAt,
    /** Normalised identity of the server the plan would install. */
    @JsonProperty("identity") McpPlanResourceIdentity identity,
    /** Origin and semantic digest of the exact validated JSON MCP card content bound to this plan. */
    @JsonProperty("provenance") McpPlanProvenance provenance,
    /** Every eligible transport, so a host can present an explicit choice. A completed plan always has at least one; when none is eligible, planning returns `CatalogUnavailableTransportError` instead. */
    @JsonProperty("transportChoices") List<McpPlanTransportChoice> transportChoices,
    /** Identifier of the choice the runtime would pick by default. Omitted when there is no eligible transport, or when the runtime expresses no preference. */
    @JsonProperty("recommendedTransportChoiceId") String recommendedTransportChoiceId,
    /** Configuration scope and key the plan would write to. */
    @JsonProperty("target") McpPlanTarget target,
    /** Outcome of evaluating the server against registry and enterprise policy. */
    @JsonProperty("policy") McpPlanPolicyResult policy,
    /** Alternative configuration changes, with exactly one entry for each transportChoices entry in the same order. Only the entry for the subsequently selected transport applies; these are not cumulative writes. Payloads remain behind the runtime boundary. */
    @JsonProperty("configurationChanges") List<McpPlanConfigurationChange> configurationChanges,
    /** Whether applying this plan would require an MCP reload to take effect. Planning itself never reloads. */
    @JsonProperty("reloadRequired") Boolean reloadRequired,
    /** True only when every eligible transport choice needs additional values or secrets. False means at least one choice needs no additional configuration, not that every choice is ready. A later apply operation must validate the selected choice's own inputs, secrets and policy after explicit confirmation. */
    @JsonProperty("requiresInteractiveConfiguration") Boolean requiresInteractiveConfiguration
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpInstallPlan {}

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
        private String planHandle;
        private String planHandleExpiresAt;
        private McpPlanResourceIdentity identity;
        private McpPlanProvenance provenance;
        private List<McpPlanTransportChoice> transportChoices;
        private String recommendedTransportChoiceId;
        private McpPlanTarget target;
        private McpPlanPolicyResult policy;
        private List<McpPlanConfigurationChange> configurationChanges;
        private Boolean reloadRequired;
        private Boolean requiresInteractiveConfiguration;

        /**
         * Sets the {@code planHandle} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder planHandle(String value) {
            this.planHandle = value;
            return this;
        }

        /**
         * Sets the {@code planHandleExpiresAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder planHandleExpiresAt(String value) {
            this.planHandleExpiresAt = value;
            return this;
        }

        /**
         * Sets the {@code identity} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder identity(McpPlanResourceIdentity value) {
            this.identity = value;
            return this;
        }

        /**
         * Sets the {@code provenance} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder provenance(McpPlanProvenance value) {
            this.provenance = value;
            return this;
        }

        /**
         * Sets the {@code transportChoices} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder transportChoices(List<McpPlanTransportChoice> value) {
            this.transportChoices = value;
            return this;
        }

        /**
         * Sets the {@code recommendedTransportChoiceId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder recommendedTransportChoiceId(String value) {
            this.recommendedTransportChoiceId = value;
            return this;
        }

        /**
         * Sets the {@code target} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder target(McpPlanTarget value) {
            this.target = value;
            return this;
        }

        /**
         * Sets the {@code policy} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder policy(McpPlanPolicyResult value) {
            this.policy = value;
            return this;
        }

        /**
         * Sets the {@code configurationChanges} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder configurationChanges(List<McpPlanConfigurationChange> value) {
            this.configurationChanges = value;
            return this;
        }

        /**
         * Sets the {@code reloadRequired} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reloadRequired(Boolean value) {
            this.reloadRequired = value;
            return this;
        }

        /**
         * Sets the {@code requiresInteractiveConfiguration} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder requiresInteractiveConfiguration(Boolean value) {
            this.requiresInteractiveConfiguration = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpInstallPlan build() {
            if (planHandle == null) throw new IllegalStateException("Missing required property: planHandle");
            if (planHandleExpiresAt == null) throw new IllegalStateException("Missing required property: planHandleExpiresAt");
            if (identity == null) throw new IllegalStateException("Missing required property: identity");
            if (provenance == null) throw new IllegalStateException("Missing required property: provenance");
            if (transportChoices == null) throw new IllegalStateException("Missing required property: transportChoices");
            if (target == null) throw new IllegalStateException("Missing required property: target");
            if (policy == null) throw new IllegalStateException("Missing required property: policy");
            if (configurationChanges == null) throw new IllegalStateException("Missing required property: configurationChanges");
            if (reloadRequired == null) throw new IllegalStateException("Missing required property: reloadRequired");
            if (requiresInteractiveConfiguration == null) throw new IllegalStateException("Missing required property: requiresInteractiveConfiguration");
            return new McpInstallPlan(planHandle, planHandleExpiresAt, identity, provenance, transportChoices, recommendedTransportChoiceId, target, policy, configurationChanges, reloadRequired, requiresInteractiveConfiguration);
        }
    }
}
