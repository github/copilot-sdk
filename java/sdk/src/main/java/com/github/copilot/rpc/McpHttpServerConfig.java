/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for a remote HTTP/SSE MCP (Model Context Protocol) server.
 * <p>
 * Use this to configure an MCP server that communicates over HTTP or
 * Server-Sent Events (SSE).
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var server = new McpHttpServerConfig().setUrl("https://mcp.example.com/sse").setTools(List.of("*"));
 *
 * var config = new SessionConfig().setMcpServers(Map.of("remote-server", server));
 * }</pre>
 *
 * @see McpServerConfig
 * @see SessionConfig#setMcpServers(java.util.Map)
 * @since 1.3.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class McpHttpServerConfig extends McpServerConfig {

    @JsonProperty("type")
    private final String type = "http";

    @JsonProperty("url")
    private String url;

    @JsonProperty("headers")
    private Map<String, String> headers;

    @JsonProperty("oauthClientId")
    private String oauthClientId;

    @JsonProperty("oauthScopes")
    private List<String> oauthScopes;

    @JsonProperty("oauthPublicClient")
    private Boolean oauthPublicClient;

    @JsonProperty("oauthGrantType")
    private String oauthGrantType;

    /**
     * Gets the server type discriminator.
     *
     * @return always {@code "http"}
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the URL of the remote server.
     *
     * @return the server URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the URL of the remote server.
     *
     * @param url
     *            the server URL
     * @return this config for method chaining
     */
    public McpHttpServerConfig setUrl(String url) {
        this.url = url;
        return this;
    }

    /**
     * Gets the optional HTTP headers to include in requests.
     *
     * @return the headers map, or {@code null}
     */
    public Map<String, String> getHeaders() {
        return headers == null ? null : Collections.unmodifiableMap(headers);
    }

    /**
     * Sets optional HTTP headers to include in requests to this server.
     *
     * @param headers
     *            the headers map
     * @return this config for method chaining
     */
    public McpHttpServerConfig setHeaders(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    /**
     * Gets the statically configured OAuth client ID.
     *
     * @return the OAuth client ID, or {@code null}
     */
    public String getOauthClientId() {
        return oauthClientId;
    }

    /**
     * Sets the statically configured OAuth client ID.
     *
     * @param oauthClientId
     *            the non-empty OAuth client ID
     * @return this config for method chaining
     */
    public McpHttpServerConfig setOauthClientId(String oauthClientId) {
        this.oauthClientId = oauthClientId;
        return this;
    }

    /**
     * Gets the configured OAuth scopes.
     *
     * @return the OAuth scopes, or {@code null}
     */
    public List<String> getOauthScopes() {
        return oauthScopes == null ? null : Collections.unmodifiableList(oauthScopes);
    }

    /**
     * Sets the OAuth scopes to request when the server challenge omits scope or
     * provides an empty scope.
     *
     * @param oauthScopes
     *            the non-empty list of RFC 6749 scope-token strings
     * @return this config for method chaining
     */
    public McpHttpServerConfig setOauthScopes(List<String> oauthScopes) {
        this.oauthScopes = oauthScopes;
        return this;
    }

    /**
     * Gets whether the configured OAuth client is public.
     *
     * @return whether the client is public, or {@code null}
     */
    public Boolean getOauthPublicClient() {
        return oauthPublicClient;
    }

    /**
     * Sets whether the configured OAuth client is public.
     *
     * @param oauthPublicClient
     *            whether the client is public
     * @return this config for method chaining
     */
    public McpHttpServerConfig setOauthPublicClient(Boolean oauthPublicClient) {
        this.oauthPublicClient = oauthPublicClient;
        return this;
    }

    /**
     * Gets the configured OAuth grant type.
     *
     * @return the OAuth grant type, or {@code null}
     */
    public String getOauthGrantType() {
        return oauthGrantType;
    }

    /**
     * Sets the OAuth grant type.
     *
     * @param oauthGrantType
     *            the OAuth grant type
     * @return this config for method chaining
     */
    public McpHttpServerConfig setOauthGrantType(String oauthGrantType) {
        this.oauthGrantType = oauthGrantType;
        return this;
    }

    @Override
    public McpHttpServerConfig setTools(List<String> tools) {
        super.setTools(tools);
        return this;
    }

    @Override
    public McpHttpServerConfig setTimeout(Integer timeout) {
        super.setTimeout(timeout);
        return this;
    }
}
