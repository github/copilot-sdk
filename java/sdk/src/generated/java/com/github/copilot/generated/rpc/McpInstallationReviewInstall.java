/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Variant {@code install} of {@link McpInstallationReview}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public record McpInstallationReviewInstall(
    /** Identity from the retained plan, not caller display text. */
    @JsonProperty("identity") McpPlanResourceIdentity identity,
    /** Original source identity and content commitment. */
    @JsonProperty("provenance") McpPlanProvenance provenance,
    /** Original catalogue trust metadata, not a verification claim. */
    @JsonProperty("catalogueTrust") CatalogTrustSnapshot catalogueTrust,
    /** Catalogue identity retained from the bound candidate when available. */
    @JsonProperty("catalogue") InstallationCatalogueIdentity catalogue,
    /** Exact reviewed user-scope destination. */
    @JsonProperty("target") McpPlanTarget target,
    /** Policy decision bound to this plan. */
    @JsonProperty("policy") McpPlanPolicyResult policy,
    /** Only the selected alternative is applied. */
    @JsonProperty("selectedChoice") McpPlanTransportChoice selectedChoice,
    /** The configuration change for the selected alternative only. */
    @JsonProperty("configurationChange") McpPlanConfigurationChange configurationChange,
    /** Non-secret values supplied for this selected alternative. */
    @JsonProperty("inputs") List<McpInstallationInput> inputs,
    /** Exact reviewed placeholders supplied separately. Never secret values. */
    @JsonProperty("suppliedSecrets") List<String> suppliedSecrets,
    /** Explicit reviewed backend selection; no backend is accessed when no secrets are supplied. */
    @JsonProperty("secretStorage") McpInstallationSecretStorage secretStorage,
    /** Complete effective remote configuration for final installation review.
Earlier private selection reviews and package choices omit this field.
The owned remote resource requires it before issuing confirmation. */
    @JsonProperty("effectiveConfiguration") McpInstallationRemoteConfiguration effectiveConfiguration,
    /** Exact reviewed installation action. */
    @JsonProperty("action") String action
) implements McpInstallationReview {
    /**
     * Creates this variant positionally and normalizes its discriminator.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpInstallationReviewInstall {
        action = "install";
    }

    /**
     * Creates this variant without explicitly supplying its discriminator.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpInstallationReviewInstall(
        McpPlanResourceIdentity identity,
        McpPlanProvenance provenance,
        CatalogTrustSnapshot catalogueTrust,
        InstallationCatalogueIdentity catalogue,
        McpPlanTarget target,
        McpPlanPolicyResult policy,
        McpPlanTransportChoice selectedChoice,
        McpPlanConfigurationChange configurationChange,
        List<McpInstallationInput> inputs,
        List<String> suppliedSecrets,
        McpInstallationSecretStorage secretStorage,
        McpInstallationRemoteConfiguration effectiveConfiguration
    ) {
        this(identity, provenance, catalogueTrust, catalogue, target, policy, selectedChoice, configurationChange, inputs, suppliedSecrets, secretStorage, effectiveConfiguration, "install");
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
        private McpPlanResourceIdentity identity;
        private McpPlanProvenance provenance;
        private CatalogTrustSnapshot catalogueTrust;
        private InstallationCatalogueIdentity catalogue;
        private McpPlanTarget target;
        private McpPlanPolicyResult policy;
        private McpPlanTransportChoice selectedChoice;
        private McpPlanConfigurationChange configurationChange;
        private List<McpInstallationInput> inputs;
        private List<String> suppliedSecrets;
        private McpInstallationSecretStorage secretStorage;
        private McpInstallationRemoteConfiguration effectiveConfiguration;

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
         * Sets the {@code catalogueTrust} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder catalogueTrust(CatalogTrustSnapshot value) {
            this.catalogueTrust = value;
            return this;
        }

        /**
         * Sets the {@code catalogue} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder catalogue(InstallationCatalogueIdentity value) {
            this.catalogue = value;
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
         * Sets the {@code selectedChoice} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder selectedChoice(McpPlanTransportChoice value) {
            this.selectedChoice = value;
            return this;
        }

        /**
         * Sets the {@code configurationChange} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder configurationChange(McpPlanConfigurationChange value) {
            this.configurationChange = value;
            return this;
        }

        /**
         * Sets the {@code inputs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder inputs(List<McpInstallationInput> value) {
            this.inputs = value;
            return this;
        }

        /**
         * Sets the {@code suppliedSecrets} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder suppliedSecrets(List<String> value) {
            this.suppliedSecrets = value;
            return this;
        }

        /**
         * Sets the {@code secretStorage} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder secretStorage(McpInstallationSecretStorage value) {
            this.secretStorage = value;
            return this;
        }

        /**
         * Sets the {@code effectiveConfiguration} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder effectiveConfiguration(McpInstallationRemoteConfiguration value) {
            this.effectiveConfiguration = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpInstallationReviewInstall build() {
            if (identity == null) throw new IllegalStateException("Missing required property: identity");
            if (provenance == null) throw new IllegalStateException("Missing required property: provenance");
            if (target == null) throw new IllegalStateException("Missing required property: target");
            if (policy == null) throw new IllegalStateException("Missing required property: policy");
            if (selectedChoice == null) throw new IllegalStateException("Missing required property: selectedChoice");
            if (configurationChange == null) throw new IllegalStateException("Missing required property: configurationChange");
            if (inputs == null) throw new IllegalStateException("Missing required property: inputs");
            if (suppliedSecrets == null) throw new IllegalStateException("Missing required property: suppliedSecrets");
            if (secretStorage == null) throw new IllegalStateException("Missing required property: secretStorage");
            return new McpInstallationReviewInstall(identity, provenance, catalogueTrust, catalogue, target, policy, selectedChoice, configurationChange, inputs, suppliedSecrets, secretStorage, effectiveConfiguration, "install");
        }
    }
}
