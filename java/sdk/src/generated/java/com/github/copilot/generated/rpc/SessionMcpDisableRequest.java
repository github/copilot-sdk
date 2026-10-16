/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.Objects;
import javax.annotation.processing.Generated;

/**
 * Name of the MCP server to disable for the session.
 * <p>
 * Required inputs are constructor arguments. Optional inputs have fluent setters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SessionMcpDisableRequest {

    /** Name of the MCP server to disable */
    @JsonProperty("serverName")
    private final String serverName;

    /** Required for an owned installation; omission preserves only manual-server behaviour. */
    @JsonProperty("expectedInstallationId")
    private String expectedInstallationId;

    /**
     * Creates a request with its required inputs.
     *
     * @param serverName Name of the MCP server to disable
     */
    public SessionMcpDisableRequest(String serverName) {
        this.serverName = Objects.requireNonNull(serverName, "serverName");
    }

    /**
     * Returns the {@code serverName} property.
     *
     * @return Name of the MCP server to disable
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * Returns the {@code expectedInstallationId} property.
     *
     * @return Required for an owned installation; omission preserves only manual-server behaviour.
     */
    public String getExpectedInstallationId() {
        return expectedInstallationId;
    }

    /**
     * Sets the {@code expectedInstallationId} property.
     *
     * @param value Required for an owned installation; omission preserves only manual-server behaviour.
     * @return this request
     */
    public SessionMcpDisableRequest setExpectedInstallationId(String value) {
        this.expectedInstallationId = value;
        return this;
    }
}
