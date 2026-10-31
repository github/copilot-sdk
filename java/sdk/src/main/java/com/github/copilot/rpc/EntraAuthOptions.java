/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.copilot.CopilotExperimental;

/**
 * Host Entra application and optional host-owned identity and token
 * acquisition.
 */
@CopilotExperimental
public final class EntraAuthOptions {
    private String clientId;
    private EntraTokenProvider tokenProvider;

    /** @return the host application's Entra client ID */
    public String getClientId() {
        return clientId;
    }

    /**
     * @param clientId
     *            the host application's Entra client ID
     * @return this options instance
     */
    public EntraAuthOptions setClientId(String clientId) {
        this.clientId = Objects.requireNonNull(clientId, "clientId must not be null");
        return this;
    }

    /** @return the host callback, or null to use built-in runtime sign-in */
    @JsonIgnore
    public EntraTokenProvider getTokenProvider() {
        return tokenProvider;
    }

    /**
     * @param tokenProvider
     *            the host callback; unavailability never opens runtime sign-in
     * @return this options instance
     */
    public EntraAuthOptions setTokenProvider(EntraTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
        return this;
    }
}
