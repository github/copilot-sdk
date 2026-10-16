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
    public McpInstallationReviewInstall {
        action = "install";
    }

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
}
