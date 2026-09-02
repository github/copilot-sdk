/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Outcome of a catalog.search call: either bounded inert candidates, or one typed refusal. Never a partial success.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CatalogSearchSucceeded.class, name = "succeeded"),
    @JsonSubTypes.Type(value = CatalogNegotiationRefusedError.class, name = "negotiation-refused"),
    @JsonSubTypes.Type(value = CatalogUnsupportedKindError.class, name = "unsupported-kind"),
    @JsonSubTypes.Type(value = CatalogInvalidRequestError.class, name = "invalid-request"),
    @JsonSubTypes.Type(value = CatalogAuthenticationRequiredError.class, name = "authentication-required"),
    @JsonSubTypes.Type(value = CatalogPolicyRejectedError.class, name = "policy-rejected"),
    @JsonSubTypes.Type(value = CatalogNetworkFailureError.class, name = "network-failure"),
    @JsonSubTypes.Type(value = CatalogUnsafeRetrievalError.class, name = "unsafe-retrieval"),
    @JsonSubTypes.Type(value = CatalogMalformedCardError.class, name = "malformed-card"),
    @JsonSubTypes.Type(value = CatalogContractViolationError.class, name = "contract-violation"),
    @JsonSubTypes.Type(value = CatalogUnavailableError.class, name = "unavailable")
})
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class CatalogSearchResult {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the kind discriminator
     */
    public abstract String getKind();
}
