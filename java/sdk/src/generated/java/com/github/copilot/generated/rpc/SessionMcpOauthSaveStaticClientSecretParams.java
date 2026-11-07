/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Persist a confidential client's static secret without exposing token-store CRUD.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionMcpOauthSaveStaticClientSecretParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Runtime OAuth credential key. */
    @JsonProperty("storeKey") String storeKey,
    /** Preserve hostless credential and account selection. */
    @JsonProperty("sessionless") Boolean sessionless,
    /** Secret to persist in the selected credential owner. */
    @JsonProperty("secret") String secret,
    /** Credential owner. Omission uses the authoritative session store. */
    @JsonProperty("store") Object store
) {
}
