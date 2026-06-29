/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.github.copilot.CopilotExperimental;

/**
 * Arguments passed to a BYOK bearer-token provider callback.
 * <p>
 * <strong>Experimental.</strong> This managed-identity surface may change or be
 * removed in future SDK or CLI releases.
 *
 * @since 1.0.0
 */
@CopilotExperimental
public class ProviderTokenArgs {

    private final String providerName;

    private final String sessionId;

    /**
     * Creates argument object for the named provider.
     *
     * @param providerName
     *            the name of the BYOK provider needing a token; {@code "default"}
     *            for the singular whole-session provider, otherwise the named
     *            provider's {@code name}
     * @param sessionId
     *            the id of the session that triggered this token request
     */
    public ProviderTokenArgs(String providerName, String sessionId) {
        this.providerName = providerName;
        this.sessionId = sessionId;
    }

    /**
     * Gets the name of the BYOK provider needing a token.
     * <p>
     * The value is {@code "default"} for the singular whole-session provider,
     * otherwise the named provider's {@code name}.
     *
     * @return the provider name
     */
    public String getProviderName() {
        return providerName;
    }

    /**
     * Gets the id of the session that triggered this token request.
     * <p>
     * A client-level shared callback registered for many sessions can use this to
     * resolve the owning session and scope token acquisition or caching per
     * session.
     *
     * @return the session id
     */
    public String getSessionId() {
        return sessionId;
    }
}
