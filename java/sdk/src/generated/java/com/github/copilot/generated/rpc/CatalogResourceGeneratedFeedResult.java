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
 * Outcome of catalog.getGeneratedFeed: the feed with its entries, or one typed refusal. An entry that cannot be presented never fails the feed.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CatalogGetGeneratedFeedSucceeded.class, name = "succeeded"),
    @JsonSubTypes.Type(value = CatalogResourceGeneratedFeedResultCatalogNegotiationRefusedError.class, name = "negotiation-refused"),
    @JsonSubTypes.Type(value = CatalogResourceGeneratedFeedResultCatalogInvalidRequestError.class, name = "invalid-request"),
    @JsonSubTypes.Type(value = CatalogResourceGeneratedFeedResultCatalogAuthenticationRequiredError.class, name = "authentication-required"),
    @JsonSubTypes.Type(value = CatalogResourceGeneratedFeedResultCatalogPolicyRejectedError.class, name = "policy-rejected"),
    @JsonSubTypes.Type(value = CatalogResourceGeneratedFeedResultCatalogNetworkFailureError.class, name = "network-failure"),
    @JsonSubTypes.Type(value = CatalogResourceGeneratedFeedResultCatalogUnsafeRetrievalError.class, name = "unsafe-retrieval"),
    @JsonSubTypes.Type(value = CatalogResourceGeneratedFeedResultCatalogMalformedCardError.class, name = "malformed-card"),
    @JsonSubTypes.Type(value = CatalogResourceGeneratedFeedResultCatalogUnavailableError.class, name = "unavailable")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class CatalogResourceGeneratedFeedResult {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the kind discriminator
     */
    public abstract String getKind();
}
