/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Variant {@code setCredentials} of {@link AuthWrite}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AuthWriteSetCredentials extends AuthWrite {

    @JsonProperty("kind")
    private final String kind = "setCredentials";

    @Override
    public String getKind() { return kind; }

    /** Authentication host URL. */
    @JsonProperty("host")
    private String host;

    /** Login/username for the credential. */
    @JsonProperty("login")
    private String login;

    /** GitHub authentication token to install. */
    @JsonProperty("token")
    private String token;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
