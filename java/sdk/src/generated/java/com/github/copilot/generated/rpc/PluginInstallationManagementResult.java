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
 * Agent Plugin installation planning result with the honoured contract, or a typed refusal.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PluginInstallationManagementResultOutcome.class, name = "outcome"),
    @JsonSubTypes.Type(value = PluginInstallationManagementResultCatalogNegotiationRefusedError.class, name = "negotiation-refused"),
    @JsonSubTypes.Type(value = PluginInstallationManagementResultCatalogInvalidRequestError.class, name = "invalid-request"),
    @JsonSubTypes.Type(value = PluginInstallationManagementResultCatalogNetworkFailureError.class, name = "network-failure")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class PluginInstallationManagementResult {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the kind discriminator
     */
    public abstract String getKind();
}
