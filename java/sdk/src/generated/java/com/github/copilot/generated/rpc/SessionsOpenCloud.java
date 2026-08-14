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
 * Parameters for creating a new cloud session.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionsOpenCloud extends SessionsOpenParams {

    @JsonProperty("kind")
    private final String kind = "cloud";

    @Override
    public String getKind() { return kind; }

    /** Repository for the cloud session. */
    @JsonProperty("repository")
    private RemoteSessionRepository repository;

    /** Optional owner (user or organization login) to associate with the cloud session when no repository is provided. Ignored when `repository` is set (the repo's owner takes precedence). */
    @JsonProperty("owner")
    private String owner;

    /** Session options for cloud session creation. */
    @JsonProperty("options")
    private SessionOpenOptions options;

    /** In-process callback invoked when the cloud task is created (before connection). Marked internal because a function reference cannot cross the JSON-RPC boundary. Disappears in the SDK migration: the field is purely cosmetic (it flips a single CLI phase label from 'creating' to 'connecting') and the wire-clean version just drops the intermediate phase. */
    @JsonProperty("onTaskCreated")
    private Object onTaskCreated;

    public RemoteSessionRepository getRepository() { return repository; }
    public void setRepository(RemoteSessionRepository repository) { this.repository = repository; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public SessionOpenOptions getOptions() { return options; }
    public void setOptions(SessionOpenOptions options) { this.options = options; }

    public Object getOnTaskCreated() { return onTaskCreated; }
    public void setOnTaskCreated(Object onTaskCreated) { this.onTaskCreated = onTaskCreated; }
}
