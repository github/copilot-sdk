/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import com.github.copilot.generated.rpc.FusionTier;

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

    @JsonProperty("fusionTier")
    private FusionTier fusionTier;

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
     * <p>
     * For Copilot Fusion, prefer {@link #setFusionTier(FusionTier)}. The
     * {@code balance}, {@code hydrafusion} and {@code hydrafusion-max} identifiers
     * it replaces remain accepted but are deprecated.
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
    /**
     * Gets the Copilot Fusion tier.
     *
     * @return the Fusion tier, or {@code null} when none was requested
     */
    @CopilotExperimental
    public FusionTier getFusionTier() {
        return fusionTier;
    }

    /**
     * Sets the Copilot Fusion tier for the {@code fusion} or {@code auto} model,
     * matching the CLI's {@code --fusion-tier}.
     * <p>
     * The runtime resolves the tier to the Auto routing preference it stands for
     * and then applies it exactly like {@link #setAutoTier(AutoTier)}, so
     * {@code session.model.getCurrent} and {@code session.model_change} events
     * report that preference as {@code autoTier}; {@code session.model.getCurrent}
     * also reports the resolved tier as {@code fusionTier} once the session's Auto
     * tier metadata is known. It cannot be combined with an Auto tier. To change
     * the tier on a live session, use
     * {@link com.github.copilot.CopilotSession#setFusionTier(FusionTier)}.
     *
     * @param fusionTier
     *            the Fusion tier, or {@code null} to omit it from the request
     * @return this config for method chaining
     */
    @CopilotExperimental
    public CapiSessionOptions setFusionTier(FusionTier fusionTier) {
        this.fusionTier = fusionTier;
        return this;
    }

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
