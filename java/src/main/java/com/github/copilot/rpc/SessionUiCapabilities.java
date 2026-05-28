/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * UI-specific capability flags for a session.
 *
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionUiCapabilities {

    @JsonProperty("elicitation")
    private Boolean elicitation;

    @JsonProperty("mcpApps")
    private Boolean mcpApps;

    /**
     * Returns whether the host supports interactive elicitation dialogs.
     *
     * @return an {@link Optional} containing the boolean value, or empty if not set
     */
    @JsonIgnore
    public Optional<Boolean> getElicitation() {
        return Optional.ofNullable(elicitation);
    }

    /**
     * Sets whether the host supports interactive elicitation dialogs.
     *
     * @param elicitation
     *            {@code true} if elicitation is supported
     * @return this instance for method chaining
     */
    public SessionUiCapabilities setElicitation(boolean elicitation) {
        this.elicitation = elicitation;
        return this;
    }

    /**
     * Clears the elicitation setting, reverting to the default behavior.
     *
     * @return this instance for method chaining
     */
    public SessionUiCapabilities clearElicitation() {
        this.elicitation = null;
        return this;
    }

    /**
     * Returns whether the runtime has accepted the session's MCP Apps (SEP-1865)
     * opt-in. Present and {@code true} when the consumer set
     * {@code enableMcpApps=true} on create/resume <b>and</b> the runtime's
     * {@code MCP_APPS} feature flag (or {@code COPILOT_MCP_APPS=true} env override)
     * is on. Otherwise empty or {@code false}, indicating the runtime silently
     * dropped the opt-in.
     *
     * @return an {@link Optional} containing the boolean value, or empty if not set
     */
    @JsonIgnore
    public Optional<Boolean> getMcpApps() {
        return Optional.ofNullable(mcpApps);
    }

    /**
     * Sets whether the runtime has accepted the MCP Apps opt-in.
     *
     * @param mcpApps
     *            {@code true} if MCP Apps is enabled for this session
     * @return this instance for method chaining
     */
    public SessionUiCapabilities setMcpApps(boolean mcpApps) {
        this.mcpApps = mcpApps;
        return this;
    }

    /**
     * Clears the mcpApps setting.
     *
     * @return this instance for method chaining
     */
    public SessionUiCapabilities clearMcpApps() {
        this.mcpApps = null;
        return this;
    }

}
