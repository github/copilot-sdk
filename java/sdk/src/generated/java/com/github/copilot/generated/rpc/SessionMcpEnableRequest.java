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
 * Name of the MCP server to enable for the session.
 * <p>
 * Required inputs are constructor arguments. Optional inputs have fluent setters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SessionMcpEnableRequest {

    /** Name of the MCP server to enable */
    @JsonProperty("serverName")
    private final String serverName;

    /** Exact receipt identity for explicit owned activation in this session. */
    @JsonProperty("expectedInstallationId")
    private String expectedInstallationId;

    /**
     * Creates a request with its required inputs.
     *
     * @param serverName Name of the MCP server to enable
     */
    public SessionMcpEnableRequest(String serverName) {
        this.serverName = Objects.requireNonNull(serverName, "serverName");
    }

    /**
     * Returns the {@code serverName} property.
     *
     * @return Name of the MCP server to enable
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * Returns the {@code expectedInstallationId} property.
     *
     * @return Exact receipt identity for explicit owned activation in this session.
     */
    public String getExpectedInstallationId() {
        return expectedInstallationId;
    }

    /**
     * Sets the {@code expectedInstallationId} property.
     *
     * @param value Exact receipt identity for explicit owned activation in this session.
     * @return this request
     */
    public SessionMcpEnableRequest setExpectedInstallationId(String value) {
        this.expectedInstallationId = value;
        return this;
    }
}
