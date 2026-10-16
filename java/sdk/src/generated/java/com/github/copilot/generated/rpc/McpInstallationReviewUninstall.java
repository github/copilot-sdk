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
import javax.annotation.processing.Generated;

/**
 * Variant {@code uninstall} of {@link McpInstallationReview}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public record McpInstallationReviewUninstall(
    /** Receipt-owned installation being removed. */
    @JsonProperty("installationId") String installationId,
    /** Identity from the installed receipt. */
    @JsonProperty("identity") McpPlanResourceIdentity identity,
    /** Source identity and content commitment retained by the installed receipt. */
    @JsonProperty("provenance") McpPlanProvenance provenance,
    /** Exact destination, checked for intervening changes before mutation. */
    @JsonProperty("target") McpPlanTarget target,
    /** Current removal policy, independent of permission to activate the server. */
    @JsonProperty("policy") McpPlanPolicyResult policy,
    /** Whether uninstall restores a protected pre-install configuration. */
    @JsonProperty("restoresPreviousConfiguration") Boolean restoresPreviousConfiguration,
    /** Exact planner-owned secret slots to remove, excluding shared OAuth grants. */
    @JsonProperty("ownedSecretCount") Long ownedSecretCount,
    /** Shared profile authentication is deliberately retained, not pending cleanup. */
    @JsonProperty("preservesSharedAuthentication") Boolean preservesSharedAuthentication,
    /** Exact reviewed installation action. */
    @JsonProperty("action") String action
) implements McpInstallationReview {
    public McpInstallationReviewUninstall {
        action = "uninstall";
    }

    public McpInstallationReviewUninstall(
        String installationId,
        McpPlanResourceIdentity identity,
        McpPlanProvenance provenance,
        McpPlanTarget target,
        McpPlanPolicyResult policy,
        Boolean restoresPreviousConfiguration,
        Long ownedSecretCount,
        Boolean preservesSharedAuthentication
    ) {
        this(installationId, identity, provenance, target, policy, restoresPreviousConfiguration, ownedSecretCount, preservesSharedAuthentication, "uninstall");
    }
}
