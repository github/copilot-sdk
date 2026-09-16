/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.copilot.generated.rpc.ManagedMcpServerConfig;

/**
 * Configuration for a connected Copilot Connector MCP endpoint supplied from
 * the service catalog.
 * <p>
 * Short-lived client-to-Copilot-Connectors service authorization must be
 * supplied dynamically through {@link McpHeadersRefreshHandler} and must not be
 * stored in this configuration. This is normally the selected account's GitHub
 * bearer token for the exact service-advertised endpoint. It is not a
 * downstream provider credential; the Connector service owns provider tokens
 * for services such as Outlook or Slack.
 * <p>
 * The containing map key is the stable Connector server key reported by
 * {@link McpHeadersRefreshRequest#serverKey()}; {@link #getDisplayName()} is
 * only the human-readable Connector label.
 *
 * @see SessionConfig#setConnectorMcpServers(Map)
 * @see ResumeSessionConfig#setConnectorMcpServers(Map)
 * @since 1.0.0
 */
public final class ConnectorMcpServerConfig {

    private String displayName;
    private String url;
    private List<String> tools;
    private Long timeout;
    private Long authorizationCacheTtlMs;

    /**
     * Gets the human-readable Connector display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the human-readable Connector display name.
     *
     * @param displayName
     *            the display name
     * @return this config instance for method chaining
     */
    public ConnectorMcpServerConfig setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    /**
     * Gets the Copilot Connector MCP streamable HTTP endpoint.
     *
     * @return the server URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the Copilot Connector MCP streamable HTTP endpoint.
     *
     * @param url
     *            the server URL
     * @return this config instance for method chaining
     */
    public ConnectorMcpServerConfig setUrl(String url) {
        this.url = url;
        return this;
    }

    /**
     * Gets the tools to include from the server.
     *
     * @return the included tools, or {@code null} to include all tools
     */
    public List<String> getTools() {
        return tools == null ? null : Collections.unmodifiableList(tools);
    }

    /**
     * Sets the tools to include from the server.
     *
     * @param tools
     *            the included tools, or {@code null} to include all tools
     * @return this config instance for method chaining
     */
    public ConnectorMcpServerConfig setTools(List<String> tools) {
        this.tools = tools;
        return this;
    }

    /**
     * Gets the timeout for tool discovery and tool calls.
     *
     * @return the timeout in milliseconds, or {@code null} for the default
     */
    public Long getTimeout() {
        return timeout;
    }

    /**
     * Sets the timeout for tool discovery and tool calls.
     *
     * @param timeout
     *            the timeout in milliseconds, or {@code null} for the default
     * @return this config instance for method chaining
     */
    public ConnectorMcpServerConfig setTimeout(Long timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * Gets the maximum time the runtime may reuse Connector service authorization.
     *
     * @return the maximum authorization cache lifetime in milliseconds, or
     *         {@code null} for the runtime default
     */
    public Long getAuthorizationCacheTtlMs() {
        return authorizationCacheTtlMs;
    }

    /**
     * Sets the maximum time the runtime may reuse Connector service authorization.
     *
     * @param authorizationCacheTtlMs
     *            the maximum authorization cache lifetime in milliseconds, or
     *            {@code null} for the runtime default
     * @return this config instance for method chaining
     */
    public ConnectorMcpServerConfig setAuthorizationCacheTtlMs(Long authorizationCacheTtlMs) {
        this.authorizationCacheTtlMs = authorizationCacheTtlMs;
        return this;
    }

    ManagedMcpServerConfig toWire() {
        return new ManagedMcpServerConfig(displayName, url, tools, timeout, authorizationCacheTtlMs);
    }

    static ConnectorMcpServerConfig fromWire(ManagedMcpServerConfig wire) {
        return new ConnectorMcpServerConfig().setDisplayName(wire.displayName()).setUrl(wire.url())
                .setTools(wire.tools()).setTimeout(wire.timeout())
                .setAuthorizationCacheTtlMs(wire.headersRefreshTtlMs());
    }

    static Map<String, ManagedMcpServerConfig> toWire(Map<String, ConnectorMcpServerConfig> servers) {
        if (servers == null) {
            return null;
        }
        var wire = new LinkedHashMap<String, ManagedMcpServerConfig>();
        servers.forEach((name, server) -> wire.put(name, server == null ? null : server.toWire()));
        return wire;
    }

    static Map<String, ConnectorMcpServerConfig> fromWire(Map<String, ManagedMcpServerConfig> servers) {
        if (servers == null) {
            return null;
        }
        var configs = new LinkedHashMap<String, ConnectorMcpServerConfig>();
        servers.forEach((name, server) -> configs.put(name, server == null ? null : fromWire(server)));
        return configs;
    }
}
