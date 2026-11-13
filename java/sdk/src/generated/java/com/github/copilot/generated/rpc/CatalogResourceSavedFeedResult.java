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
 * Outcome of catalog.getSavedFeed: the feed with its entries, or one typed refusal. An entry that cannot be presented never fails the feed.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CatalogGetSavedFeedSucceeded.class, name = "succeeded"),
    @JsonSubTypes.Type(value = CatalogResourceSavedFeedResultCatalogNegotiationRefusedError.class, name = "negotiation-refused"),
    @JsonSubTypes.Type(value = CatalogResourceSavedFeedResultCatalogInvalidRequestError.class, name = "invalid-request"),
    @JsonSubTypes.Type(value = CatalogResourceSavedFeedResultCatalogAuthenticationRequiredError.class, name = "authentication-required"),
    @JsonSubTypes.Type(value = CatalogResourceSavedFeedResultCatalogPolicyRejectedError.class, name = "policy-rejected"),
    @JsonSubTypes.Type(value = CatalogResourceSavedFeedResultCatalogNetworkFailureError.class, name = "network-failure"),
    @JsonSubTypes.Type(value = CatalogResourceSavedFeedResultCatalogUnsafeRetrievalError.class, name = "unsafe-retrieval"),
    @JsonSubTypes.Type(value = CatalogResourceSavedFeedResultCatalogMalformedCardError.class, name = "malformed-card"),
    @JsonSubTypes.Type(value = CatalogResourceSavedFeedResultCatalogUnavailableError.class, name = "unavailable")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class CatalogResourceSavedFeedResult {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the kind discriminator
     */
    public abstract String getKind();
}
