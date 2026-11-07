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
 * Outcome of the tokenless handshake, with the original challenge when available.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "status", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = McpOauthTestConnectionResultConnected.class, name = "connected"),
    @JsonSubTypes.Type(value = McpOauthTestConnectionResultNeedsAuth.class, name = "needs-auth"),
    @JsonSubTypes.Type(value = McpOauthTestConnectionResultFailed.class, name = "failed")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
abstract class McpOauthTestConnectionResult {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the status discriminator
     */
    public abstract String getStatus();
}
