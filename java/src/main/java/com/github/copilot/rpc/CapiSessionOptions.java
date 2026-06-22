/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Provider-scoped session options for the Copilot API (CAPI) provider.
 * <p>
 * WebSocket transport is the default for the CAPI Responses API whenever the
 * model advertises the {@code ws:/responses} endpoint. Setting
 * {@link #setDisableWebSocketResponses(Boolean)} to {@code true} opts out to
 * the HTTP Responses transport instead, which is useful for users behind
 * proxies where WebSockets fail. This is equivalent to setting the
 * {@code COPILOT_CLI_DISABLE_WEBSOCKET_RESPONSES} environment variable.
 * <p>
 * These options are scoped under the {@code capi} namespace because a single
 * session can host multiple providers (for example, CAPI and BYOK), so
 * transport choice is provider-level rather than top-level session state. All
 * setter methods return {@code this} for method chaining.
 *
 * @see SessionConfig#setCapi(CapiSessionOptions)
 * @see ResumeSessionConfig#setCapi(CapiSessionOptions)
 * @since 1.5.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CapiSessionOptions {

    @JsonProperty("disableWebSocketResponses")
    private Boolean disableWebSocketResponses;

    /**
     * Gets whether CAPI Responses API WebSocket transport is disabled.
     *
     * @return {@code true} to opt out of WebSocket Responses transport,
     *         {@code false} to explicitly allow it, or {@code null} to use the
     *         default behavior
     */
    public Boolean getDisableWebSocketResponses() {
        return disableWebSocketResponses;
    }

    /**
     * Sets whether to disable CAPI Responses API WebSocket transport.
     * <p>
     * WebSocket transport is the default for the CAPI Responses API whenever the
     * model advertises the {@code ws:/responses} endpoint. Set this to {@code true}
     * to opt out to the HTTP Responses transport instead, which is useful for users
     * behind proxies where WebSockets fail. This is equivalent to setting the
     * {@code COPILOT_CLI_DISABLE_WEBSOCKET_RESPONSES} environment variable.
     *
     * @param disableWebSocketResponses
     *            {@code true} to opt out of WebSocket Responses transport
     * @return this config for method chaining
     */
    public CapiSessionOptions setDisableWebSocketResponses(Boolean disableWebSocketResponses) {
        this.disableWebSocketResponses = disableWebSocketResponses;
        return this;
    }
}
