/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Starts a supervised AHP host with at least one explicitly selected transport.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record HostStartParams(
    /** Caller-generated UUID identifying this connection-owned listener. */
    @JsonProperty("hostId") String hostId,
    /** Enables a local WebSocket listener. */
    @JsonProperty("localServer") HostLocalServerOptions localServer,
    /** Registers a GitHub Mission Control environment and enables its relay transport. */
    @JsonProperty("githubEnvironment") HostGitHubEnvironmentOptions gitHubEnvironment,
    /** Ask the owning SDK application to materialize AHP sessions. */
    @JsonProperty("sessionFactory") Boolean sessionFactory,
    /** Ask the owning application to resume its durable AHP sessions. */
    @JsonProperty("resumeFactory") Boolean resumeFactory
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public HostStartParams {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static final class Builder {
        private Builder() {}
        private String hostId;
        private HostLocalServerOptions localServer;
        private HostGitHubEnvironmentOptions gitHubEnvironment;
        private Boolean sessionFactory;
        private Boolean resumeFactory;

        /**
         * Sets the {@code hostId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder hostId(String value) {
            this.hostId = value;
            return this;
        }

        /**
         * Sets the {@code localServer} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder localServer(HostLocalServerOptions value) {
            this.localServer = value;
            return this;
        }

        /**
         * Sets the {@code githubEnvironment} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder gitHubEnvironment(HostGitHubEnvironmentOptions value) {
            this.gitHubEnvironment = value;
            return this;
        }

        /**
         * Sets the {@code sessionFactory} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionFactory(Boolean value) {
            this.sessionFactory = value;
            return this;
        }

        /**
         * Sets the {@code resumeFactory} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder resumeFactory(Boolean value) {
            this.resumeFactory = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public HostStartParams build() {
            if (hostId == null) throw new IllegalStateException("Missing required property: hostId");
            return new HostStartParams(hostId, localServer, gitHubEnvironment, sessionFactory, resumeFactory);
        }
    }
}
