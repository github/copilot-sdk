/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Credential-free authentication identity safe to expose to hosts and user interfaces.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record AuthIdentity(
    /** Authentication type */
    @JsonProperty("type") AuthInfoType type,
    /** Authentication host */
    @JsonProperty("host") String host,
    /** Authenticated login, when available */
    @JsonProperty("login") String login,
    /** Name of the environment variable that supplied the credential, when applicable */
    @JsonProperty("envVar") String envVar,
    /** Opaque SDK GitHub credential registration backing this identity. Routing metadata only; never a credential. */
    @JsonProperty("registrationId") String registrationId,
    /** Snapshot of the authenticated user's Copilot subscription info, if known */
    @JsonProperty("copilotUser") CopilotUserResponse copilotUser
) {
}
