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
 * Parameters for connecting to a live remote session.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionsOpenRemote extends SessionsOpenParams {

    @JsonProperty("kind")
    private final String kind = "remote";

    @Override
    public String getKind() { return kind; }

    /** Remote session identifier to connect to. */
    @JsonProperty("remoteSessionId")
    private String remoteSessionId;

    /** Repository context for the remote session. */
    @JsonProperty("repository")
    private RemoteSessionRepository repository;

    /** Session options for the connection. */
    @JsonProperty("options")
    private SessionOpenOptions options;

    public String getRemoteSessionId() { return remoteSessionId; }
    public void setRemoteSessionId(String remoteSessionId) { this.remoteSessionId = remoteSessionId; }

    public RemoteSessionRepository getRepository() { return repository; }
    public void setRepository(RemoteSessionRepository repository) { this.repository = repository; }

    public SessionOpenOptions getOptions() { return options; }
    public void setOptions(SessionOpenOptions options) { this.options = options; }
}
