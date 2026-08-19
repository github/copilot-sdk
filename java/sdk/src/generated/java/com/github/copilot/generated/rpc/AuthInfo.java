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
 * Authentication credentials accepted only at native protocol ingress. Runtime outputs use credential-free `AuthIdentity` metadata.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = HMACAuthInfo.class, name = "hmac"),
    @JsonSubTypes.Type(value = EnvAuthInfo.class, name = "env"),
    @JsonSubTypes.Type(value = TokenAuthInfo.class, name = "token"),
    @JsonSubTypes.Type(value = CopilotApiTokenAuthInfo.class, name = "copilot-api-token"),
    @JsonSubTypes.Type(value = UserAuthInfo.class, name = "user"),
    @JsonSubTypes.Type(value = GhCliAuthInfo.class, name = "gh-cli"),
    @JsonSubTypes.Type(value = ApiKeyAuthInfo.class, name = "api-key")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class AuthInfo {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the type discriminator
     */
    public abstract String getType();
}
