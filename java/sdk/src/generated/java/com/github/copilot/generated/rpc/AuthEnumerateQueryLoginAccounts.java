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
 * Variant {@code loginAccounts} of {@link AuthEnumerateQuery}.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AuthEnumerateQueryLoginAccounts extends AuthEnumerateQuery {

    @JsonProperty("kind")
    private final String kind = "loginAccounts";

    @Override
    public String getKind() { return kind; }

    /** The provider whose cached base identities should be listed. */
    @JsonProperty("provider")
    private LoginProviderKind provider;

    public LoginProviderKind getProvider() { return provider; }
    public void setProvider(LoginProviderKind provider) { this.provider = provider; }
}
