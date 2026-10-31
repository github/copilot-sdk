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
 * Host response to Entra acquisition. Unavailable, cancellation and errors are terminal: the runtime must not substitute another account or sign-in transport.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "kind", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = EntraTokenAcquireResultIdentity.class, name = "identity"),
    @JsonSubTypes.Type(value = EntraTokenAcquireResultToken.class, name = "token"),
    @JsonSubTypes.Type(value = EntraTokenAcquireResultUnavailable.class, name = "unavailable"),
    @JsonSubTypes.Type(value = EntraTokenAcquireResultCancelled.class, name = "cancelled")
})
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class EntraTokenAcquireResult {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the kind discriminator
     */
    public abstract String getKind();
}
