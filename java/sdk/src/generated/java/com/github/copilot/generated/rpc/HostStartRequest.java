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
 * Starts a supervised AHP host with at least one explicitly selected transport.
 * <p>
 * Required inputs are constructor arguments. Optional inputs have fluent setters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class HostStartRequest {

    /** Caller-generated UUID identifying this connection-owned listener. */
    @JsonProperty("hostId")
    private final String hostId;

    /** Enables a local WebSocket listener. */
    @JsonProperty("localServer")
    private HostLocalServerOptions localServer;

    /** Registers a GitHub Mission Control environment and enables its relay transport. */
    @JsonProperty("githubEnvironment")
    private HostGitHubEnvironmentOptions gitHubEnvironment;

    /** Ask the owning SDK application to materialize AHP sessions. */
    @JsonProperty("sessionFactory")
    private Boolean sessionFactory;

    /** Ask the owning application to resume its durable AHP sessions. */
    @JsonProperty("resumeFactory")
    private Boolean resumeFactory;

    /** Stable application identity selecting its durable host catalog. Defaults to the persisted runtime compute GUID; must agree with githubEnvironment.computeId when both are supplied. */
    @JsonProperty("computeId")
    private String computeId;

    /**
     * Creates a request with its required inputs.
     *
     * @param hostId Caller-generated UUID identifying this connection-owned listener.
     */
    public HostStartRequest(String hostId) {
        this.hostId = Objects.requireNonNull(hostId, "hostId");
    }

    /**
     * Returns the {@code hostId} property.
     *
     * @return Caller-generated UUID identifying this connection-owned listener.
     */
    public String getHostId() {
        return hostId;
    }

    /**
     * Returns the {@code localServer} property.
     *
     * @return Enables a local WebSocket listener.
     */
    public HostLocalServerOptions getLocalServer() {
        return localServer;
    }

    /**
     * Returns the {@code githubEnvironment} property.
     *
     * @return Registers a GitHub Mission Control environment and enables its relay transport.
     */
    public HostGitHubEnvironmentOptions getGitHubEnvironment() {
        return gitHubEnvironment;
    }

    /**
     * Returns the {@code sessionFactory} property.
     *
     * @return Ask the owning SDK application to materialize AHP sessions.
     */
    public Boolean getSessionFactory() {
        return sessionFactory;
    }

    /**
     * Returns the {@code resumeFactory} property.
     *
     * @return Ask the owning application to resume its durable AHP sessions.
     */
    public Boolean getResumeFactory() {
        return resumeFactory;
    }

    /**
     * Returns the {@code computeId} property.
     *
     * @return Stable application identity selecting its durable host catalog. Defaults to the persisted runtime compute GUID; must agree with githubEnvironment.computeId when both are supplied.
     */
    public String getComputeId() {
        return computeId;
    }

    /**
     * Sets the {@code localServer} property.
     *
     * @param value Enables a local WebSocket listener.
     * @return this request
     */
    public HostStartRequest setLocalServer(HostLocalServerOptions value) {
        this.localServer = value;
        return this;
    }

    /**
     * Sets the {@code githubEnvironment} property.
     *
     * @param value Registers a GitHub Mission Control environment and enables its relay transport.
     * @return this request
     */
    public HostStartRequest setGitHubEnvironment(HostGitHubEnvironmentOptions value) {
        this.gitHubEnvironment = value;
        return this;
    }

    /**
     * Sets the {@code sessionFactory} property.
     *
     * @param value Ask the owning SDK application to materialize AHP sessions.
     * @return this request
     */
    public HostStartRequest setSessionFactory(Boolean value) {
        this.sessionFactory = value;
        return this;
    }

    /**
     * Sets the {@code resumeFactory} property.
     *
     * @param value Ask the owning application to resume its durable AHP sessions.
     * @return this request
     */
    public HostStartRequest setResumeFactory(Boolean value) {
        this.resumeFactory = value;
        return this;
    }

    /**
     * Sets the {@code computeId} property.
     *
     * @param value Stable application identity selecting its durable host catalog. Defaults to the persisted runtime compute GUID; must agree with githubEnvironment.computeId when both are supplied.
     * @return this request
     */
    public HostStartRequest setComputeId(String value) {
        this.computeId = value;
        return this;
    }
}
