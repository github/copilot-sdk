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
 * Agent Plugin installation result with the honoured contract, or a typed request/negotiation refusal.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PluginInstallationResultOutcome.class, name = "outcome"),
    @JsonSubTypes.Type(value = PluginInstallationResultCatalogNegotiationRefusedError.class, name = "negotiation-refused"),
    @JsonSubTypes.Type(value = PluginInstallationResultCatalogInvalidRequestError.class, name = "invalid-request"),
    @JsonSubTypes.Type(value = PluginInstallationResultCatalogNetworkFailureError.class, name = "network-failure")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class PluginInstallationResult {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the kind discriminator
     */
    public abstract String getKind();
}
