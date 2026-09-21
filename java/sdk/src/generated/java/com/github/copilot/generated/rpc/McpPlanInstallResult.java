/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.annotation.processing.Generated;

/**
 * Outcome of an mcp.planInstall call: either a normalised plan, or one typed refusal. Nothing is written in either case.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = McpPlanInstallPlanned.class, name = "planned"),
    @JsonSubTypes.Type(value = McpPlanInstallResultCatalogNegotiationRefusedError.class, name = "negotiation-refused"),
    @JsonSubTypes.Type(value = CatalogHandleRejectedError.class, name = "handle-rejected"),
    @JsonSubTypes.Type(value = McpPlanInstallResultCatalogInvalidRequestError.class, name = "invalid-request"),
    @JsonSubTypes.Type(value = McpPlanInstallResultCatalogAuthenticationRequiredError.class, name = "authentication-required"),
    @JsonSubTypes.Type(value = McpPlanInstallResultCatalogPolicyRejectedError.class, name = "policy-rejected"),
    @JsonSubTypes.Type(value = McpPlanInstallResultCatalogNetworkFailureError.class, name = "network-failure"),
    @JsonSubTypes.Type(value = McpPlanInstallResultCatalogUnsafeRetrievalError.class, name = "unsafe-retrieval"),
    @JsonSubTypes.Type(value = McpPlanInstallResultCatalogMalformedCardError.class, name = "malformed-card"),
    @JsonSubTypes.Type(value = McpPlanInstallResultCatalogContractViolationError.class, name = "contract-violation"),
    @JsonSubTypes.Type(value = CatalogUnavailableTransportError.class, name = "unavailable-transport"),
    @JsonSubTypes.Type(value = CatalogNotInstallableError.class, name = "not-installable"),
    @JsonSubTypes.Type(value = McpPlanInstallResultCatalogUnavailableError.class, name = "unavailable")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class McpPlanInstallResult {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the kind discriminator
     */
    public abstract String getKind();
}
