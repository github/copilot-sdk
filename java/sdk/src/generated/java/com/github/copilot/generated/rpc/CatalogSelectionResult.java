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
 * Typed outcome of catalog.select. Only the selected host result carries a fresh candidate handle; the model-facing projection removes both that handle and searchId.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CatalogSelectionSelected.class, name = "selected"),
    @JsonSubTypes.Type(value = CatalogSelectionDeclined.class, name = "declined"),
    @JsonSubTypes.Type(value = CatalogSelectionCancelled.class, name = "cancelled"),
    @JsonSubTypes.Type(value = CatalogSelectionTimedOut.class, name = "timed-out"),
    @JsonSubTypes.Type(value = CatalogSelectionInvalid.class, name = "invalid"),
    @JsonSubTypes.Type(value = CatalogSelectionStale.class, name = "stale"),
    @JsonSubTypes.Type(value = CatalogSelectionReplayed.class, name = "replayed"),
    @JsonSubTypes.Type(value = CatalogSelectionForeign.class, name = "foreign"),
    @JsonSubTypes.Type(value = CatalogSelectionWrongKind.class, name = "wrong-kind"),
    @JsonSubTypes.Type(value = CatalogNegotiationRefusedError.class, name = "negotiation-refused"),
    @JsonSubTypes.Type(value = CatalogInvalidRequestError.class, name = "invalid-request"),
    @JsonSubTypes.Type(value = CatalogUnavailableError.class, name = "unavailable")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class CatalogSelectionResult {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the kind discriminator
     */
    public abstract String getKind();
}
