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
 * {@link #setEnableWebSocketResponses(Boolean)} to {@code false} forces the
 * HTTP Responses transport instead, which is useful for users behind proxies
 * where WebSockets fail. This is equivalent to setting the
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

    @JsonProperty("autoTier")
    private AutoTier autoTier;

    @JsonProperty("enableWebSocketResponses")
    private Boolean enableWebSocketResponses;

    /**
     * Gets the routing tier for the {@code auto} model (Auto mode V2).
     *
     * @return the explicit tier, or {@code null} to leave tier selection to the
     *         runtime
     */
    public AutoTier getAutoTier() {
        return autoTier;
    }

    /**
     * Sets the routing tier, meaningful only with model {@code auto} (Auto mode
     * V2). Requires a runtime version that supports {@code capi.autoTier}.
     * <p>
     * When omitted, the runtime chooses its default on create and preserves the
     * persisted or current tier on resume. An explicit tier overrides the persisted
     * tier on cold resume. On resident resume, a different tier requests a safe
     * switch that the runtime applies after the resume succeeds; it cannot change a
     * turn that is already in flight.
     *
     * @param autoTier
     *            the routing tier, or {@code null} to omit it from the request
     * @return this config for method chaining
     */
    public CapiSessionOptions setAutoTier(AutoTier autoTier) {
        this.autoTier = autoTier;
        return this;
    }

    /**
     * Gets whether CAPI Responses API WebSocket transport is enabled.
     *
     * @return {@code false} to force the HTTP Responses transport, {@code true} to
     *         explicitly use WebSocket transport, or {@code null} to use the
     *         default behavior
     */
    public Boolean getEnableWebSocketResponses() {
        return enableWebSocketResponses;
    }

    /**
     * Sets whether to use CAPI Responses API WebSocket transport.
     * <p>
     * WebSocket transport is the default for the CAPI Responses API whenever the
     * model advertises the {@code ws:/responses} endpoint. Set this to
     * {@code false} to force the HTTP Responses transport instead, which is useful
     * for users behind proxies where WebSockets fail. This is equivalent to setting
     * the {@code COPILOT_CLI_DISABLE_WEBSOCKET_RESPONSES} environment variable.
     *
     * @param enableWebSocketResponses
     *            {@code false} to force the HTTP Responses transport
     * @return this config for method chaining
     */
    public CapiSessionOptions setEnableWebSocketResponses(Boolean enableWebSocketResponses) {
        this.enableWebSocketResponses = enableWebSocketResponses;
        return this;
    }
}
