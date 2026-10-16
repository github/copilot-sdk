/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code mcp.oauth} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class ServerMcpOauthApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerMcpOauthApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Reads the static OAuth client a remote MCP server configuration pins, so a host can decide whether to register a client dynamically or reuse the configured one. Omits `clientConfig` when no non-empty `oauthClientId` is configured. The client secret is deliberately never carried here: the runtime loads it from the OAuth secret store at flow time.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<McpOauthStaticClientConfigResult> staticClientConfig(McpOauthStaticClientConfigParams params) {
        return caller.invoke("mcp.oauth.staticClientConfig", params, McpOauthStaticClientConfigResult.class);
    }

    /**
     * Reads raw protected-resource metadata after a 401 establishes that OAuth is required. Uses the metadata URL from the server's `WWW-Authenticate` challenge when provided; otherwise, tries conventional metadata endpoints derived from the request URL. A host configuring a remote server interactively needs the unparsed document to classify the challenge before it starts OAuth. The request URL and each metadata URL pass through the named session's egress policy and sandbox proxy. The runtime refuses URLs that the session may not reach.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<McpOauthFetchProtectedResourceMetadataResult> fetchProtectedResourceMetadata(McpOauthFetchProtectedResourceMetadataParams params) {
        return caller.invoke("mcp.oauth.fetchProtectedResourceMetadata", params, McpOauthFetchProtectedResourceMetadataResult.class);
    }

    /**
     * Asks a remote MCP server's authorization server whether it supports Dynamic Client Registration, after a 401 has already established that OAuth is required. A host configuring a remote server interactively needs this to choose between registering a client automatically and asking the user for client credentials. The request travels through the named session's egress policy and sandbox proxy, so a server that session may not reach is refused rather than contacted.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<McpOauthCheckDcrSupportResult> checkDcrSupport(McpOauthCheckDcrSupportParams params) {
        return caller.invoke("mcp.oauth.checkDcrSupport", params, McpOauthCheckDcrSupportResult.class);
    }

    /**
     * Reports whether an authorization server URL belongs to Microsoft Entra ID. Entra does not support Dynamic Client Registration, so a host that finds no DCR support can still authorize against it with a pre-registered client instead of asking the user for client credentials.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<McpOauthIsMicrosoftAuthorizationServerResult> isMicrosoftAuthorizationServer(McpOauthIsMicrosoftAuthorizationServerParams params) {
        return caller.invoke("mcp.oauth.isMicrosoftAuthorizationServer", params, McpOauthIsMicrosoftAuthorizationServerResult.class);
    }

}
