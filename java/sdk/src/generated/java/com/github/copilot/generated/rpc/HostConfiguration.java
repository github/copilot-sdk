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
 * Normalized listener settings delivered only to the supervised hosting participant.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record HostConfiguration(
    /** Explicit host-wide admission policy. Always supplied by the runtime after validating host.start. */
    @JsonProperty("authentication") Object authentication,
    /** Normalized local listener settings, absent for relay-only hosts. */
    @JsonProperty("localServer") HostLocalServerConfiguration localServer,
    /** Requested GitHub Mission Control registration. */
    @JsonProperty("githubEnvironment") HostGitHubEnvironmentOptions gitHubEnvironment,
    /** Whether session materialization is delegated to the owning application. */
    @JsonProperty("sessionFactory") Boolean sessionFactory,
    /** Whether app-owned durable sessions are resumed by the owning application. */
    @JsonProperty("resumeFactory") Boolean resumeFactory,
    /** Initial remote creation policy, independent of application session factories. Defaults to denied when omitted. */
    @JsonProperty("allowCreatingNewSessions") Boolean allowCreatingNewSessions
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    HostConfiguration {}

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
        private Object authentication;
        private HostLocalServerConfiguration localServer;
        private HostGitHubEnvironmentOptions gitHubEnvironment;
        private Boolean sessionFactory;
        private Boolean resumeFactory;
        private Boolean allowCreatingNewSessions;

        /**
         * Sets the {@code authentication} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder authentication(Object value) {
            this.authentication = value;
            return this;
        }

        /**
         * Sets the {@code localServer} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder localServer(HostLocalServerConfiguration value) {
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
         * Sets the {@code allowCreatingNewSessions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder allowCreatingNewSessions(Boolean value) {
            this.allowCreatingNewSessions = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public HostConfiguration build() {
            if (sessionFactory == null) throw new IllegalStateException("Missing required property: sessionFactory");
            if (resumeFactory == null) throw new IllegalStateException("Missing required property: resumeFactory");
            return new HostConfiguration(authentication, localServer, gitHubEnvironment, sessionFactory, resumeFactory, allowCreatingNewSessions);
        }
    }
}
