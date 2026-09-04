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
 * One eligible way to run the server, represented as a tagged package or remote variant so package identity and endpoint states cannot contradict the install method.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "installMethod", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = McpPlanTransportChoicePackage.class, name = "package"),
    @JsonSubTypes.Type(value = McpPlanTransportChoiceRemote.class, name = "remote")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class McpPlanTransportChoice {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the installMethod discriminator
     */
    public abstract String getInstallMethod();
}
