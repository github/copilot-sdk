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
 * Remote MCP server name and optional overrides controlling reauthentication, OAuth client display name, callback success-page copy, and static OAuth client selection.
 * <p>
 * Required inputs are constructor arguments. Optional inputs have fluent setters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SessionMcpOauthLoginRequest {

    /** Name of the remote MCP server to authenticate */
    @JsonProperty("serverName")
    private final String serverName;

    /** When true, clears any cached OAuth token for the server and runs a full new authorization. Use when the user explicitly wants to switch accounts or believes their session is stuck. */
    @JsonProperty("forceReauth")
    private Boolean forceReauth;

    /** Optional override for the OAuth client display name shown on the consent screen. Applies to newly registered dynamic clients only — existing registrations keep the name they were created with. When omitted, the runtime applies a neutral fallback; callers driving interactive auth should pass their own surface-specific label so the consent screen matches the product the user sees. */
    @JsonProperty("clientName")
    private String clientName;

    /** Optional override for the body text shown on the OAuth loopback callback success page. When omitted, the runtime applies a neutral fallback; callers driving interactive auth should pass surface-specific copy telling the user where to return. */
    @JsonProperty("callbackSuccessMessage")
    private String callbackSuccessMessage;

    /** Optional OAuth client ID override for this login. When set, the runtime uses this pre-registered static client instead of dynamic client registration. */
    @JsonProperty("clientId")
    private String clientId;

    /** Optional OAuth client secret override for this login. The runtime treats this as an ephemeral host-owned secret, uses it for this authentication attempt and does not persist it. */
    @JsonProperty("clientSecret")
    private String clientSecret;

    /** Optional override indicating whether the static OAuth client is public. When false, the runtime treats it as confidential and uses the per-login clientSecret if provided, otherwise retrieving the client secret from the MCP OAuth secret store. */
    @JsonProperty("publicClient")
    private Boolean publicClient;

    /** Optional OAuth grant type override for this login. Defaults to the server configuration, or authorization_code when no grant type is specified. */
    @JsonProperty("grantType")
    private McpOauthLoginGrantType grantType;

    /** Required for owned login. Consumes the exact prepareLogin handle once. Set forceReauth and display options during preparation, not consumption. */
    @JsonProperty("loginId")
    private String loginId;

    /** Exact owned receipt identity. Owned login never uses an implicit helper session. */
    @JsonProperty("expectedInstallationId")
    private String expectedInstallationId;

    /**
     * Creates a request with its required inputs.
     *
     * @param serverName Name of the remote MCP server to authenticate
     */
    public SessionMcpOauthLoginRequest(String serverName) {
        this.serverName = Objects.requireNonNull(serverName, "serverName");
    }

    /**
     * Returns the {@code serverName} property.
     *
     * @return Name of the remote MCP server to authenticate
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * Returns the {@code forceReauth} property.
     *
     * @return When true, clears any cached OAuth token for the server and runs a full new authorization. Use when the user explicitly wants to switch accounts or believes their session is stuck.
     */
    public Boolean getForceReauth() {
        return forceReauth;
    }

    /**
     * Returns the {@code clientName} property.
     *
     * @return Optional override for the OAuth client display name shown on the consent screen. Applies to newly registered dynamic clients only — existing registrations keep the name they were created with. When omitted, the runtime applies a neutral fallback; callers driving interactive auth should pass their own surface-specific label so the consent screen matches the product the user sees.
     */
    public String getClientName() {
        return clientName;
    }

    /**
     * Returns the {@code callbackSuccessMessage} property.
     *
     * @return Optional override for the body text shown on the OAuth loopback callback success page. When omitted, the runtime applies a neutral fallback; callers driving interactive auth should pass surface-specific copy telling the user where to return.
     */
    public String getCallbackSuccessMessage() {
        return callbackSuccessMessage;
    }

    /**
     * Returns the {@code clientId} property.
     *
     * @return Optional OAuth client ID override for this login. When set, the runtime uses this pre-registered static client instead of dynamic client registration.
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Returns the {@code clientSecret} property.
     *
     * @return Optional OAuth client secret override for this login. The runtime treats this as an ephemeral host-owned secret, uses it for this authentication attempt and does not persist it.
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * Returns the {@code publicClient} property.
     *
     * @return Optional override indicating whether the static OAuth client is public. When false, the runtime treats it as confidential and uses the per-login clientSecret if provided, otherwise retrieving the client secret from the MCP OAuth secret store.
     */
    public Boolean getPublicClient() {
        return publicClient;
    }

    /**
     * Returns the {@code grantType} property.
     *
     * @return Optional OAuth grant type override for this login. Defaults to the server configuration, or authorization_code when no grant type is specified.
     */
    public McpOauthLoginGrantType getGrantType() {
        return grantType;
    }

    /**
     * Returns the {@code loginId} property.
     *
     * @return Required for owned login. Consumes the exact prepareLogin handle once. Set forceReauth and display options during preparation, not consumption.
     */
    public String getLoginId() {
        return loginId;
    }

    /**
     * Returns the {@code expectedInstallationId} property.
     *
     * @return Exact owned receipt identity. Owned login never uses an implicit helper session.
     */
    public String getExpectedInstallationId() {
        return expectedInstallationId;
    }

    /**
     * Sets the {@code forceReauth} property.
     *
     * @param value When true, clears any cached OAuth token for the server and runs a full new authorization. Use when the user explicitly wants to switch accounts or believes their session is stuck.
     * @return this request
     */
    public SessionMcpOauthLoginRequest setForceReauth(Boolean value) {
        this.forceReauth = value;
        return this;
    }

    /**
     * Sets the {@code clientName} property.
     *
     * @param value Optional override for the OAuth client display name shown on the consent screen. Applies to newly registered dynamic clients only — existing registrations keep the name they were created with. When omitted, the runtime applies a neutral fallback; callers driving interactive auth should pass their own surface-specific label so the consent screen matches the product the user sees.
     * @return this request
     */
    public SessionMcpOauthLoginRequest setClientName(String value) {
        this.clientName = value;
        return this;
    }

    /**
     * Sets the {@code callbackSuccessMessage} property.
     *
     * @param value Optional override for the body text shown on the OAuth loopback callback success page. When omitted, the runtime applies a neutral fallback; callers driving interactive auth should pass surface-specific copy telling the user where to return.
     * @return this request
     */
    public SessionMcpOauthLoginRequest setCallbackSuccessMessage(String value) {
        this.callbackSuccessMessage = value;
        return this;
    }

    /**
     * Sets the {@code clientId} property.
     *
     * @param value Optional OAuth client ID override for this login. When set, the runtime uses this pre-registered static client instead of dynamic client registration.
     * @return this request
     */
    public SessionMcpOauthLoginRequest setClientId(String value) {
        this.clientId = value;
        return this;
    }

    /**
     * Sets the {@code clientSecret} property.
     *
     * @param value Optional OAuth client secret override for this login. The runtime treats this as an ephemeral host-owned secret, uses it for this authentication attempt and does not persist it.
     * @return this request
     */
    public SessionMcpOauthLoginRequest setClientSecret(String value) {
        this.clientSecret = value;
        return this;
    }

    /**
     * Sets the {@code publicClient} property.
     *
     * @param value Optional override indicating whether the static OAuth client is public. When false, the runtime treats it as confidential and uses the per-login clientSecret if provided, otherwise retrieving the client secret from the MCP OAuth secret store.
     * @return this request
     */
    public SessionMcpOauthLoginRequest setPublicClient(Boolean value) {
        this.publicClient = value;
        return this;
    }

    /**
     * Sets the {@code grantType} property.
     *
     * @param value Optional OAuth grant type override for this login. Defaults to the server configuration, or authorization_code when no grant type is specified.
     * @return this request
     */
    public SessionMcpOauthLoginRequest setGrantType(McpOauthLoginGrantType value) {
        this.grantType = value;
        return this;
    }

    /**
     * Sets the {@code loginId} property.
     *
     * @param value Required for owned login. Consumes the exact prepareLogin handle once. Set forceReauth and display options during preparation, not consumption.
     * @return this request
     */
    public SessionMcpOauthLoginRequest setLoginId(String value) {
        this.loginId = value;
        return this;
    }

    /**
     * Sets the {@code expectedInstallationId} property.
     *
     * @param value Exact owned receipt identity. Owned login never uses an implicit helper session.
     * @return this request
     */
    public SessionMcpOauthLoginRequest setExpectedInstallationId(String value) {
        this.expectedInstallationId = value;
        return this;
    }
}
