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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Connection-level opt-ins for the `server.connect` handshake. Transport authentication is consumed by the native protocol boundary before dispatch.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record ConnectParams(
    /** Entra application used by this client's authentication flows. Required to enable built-in Microsoft Entra sign-in and token renewal; other authentication methods remain available. */
    @CopilotExperimental
    @JsonProperty("entra") EntraAuthConfig entra,
    /** Opt this connection in to host-owned Entra identity and token acquisition through entraToken.getToken. Requires entra.clientId and an explicit true acknowledgement in the response. SDKs set this internally when a callback is configured. */
    @CopilotExperimental
    @JsonProperty("entraTokenProvider") Boolean entraTokenProvider,
    /** Opt this connection in to GitHub telemetry forwarding for its lifetime. When set, the runtime forwards this host's telemetry across all its sessions, its sessionless events, and explicitly process-wide events over the `gitHubTelemetry.event` notification. Connections intentionally sharing one server receive that server's events; independently embedded runtime hosts do not receive each other's host-owned events. Regular events are also written to the runtime's normal GitHub/CTS path (dual-write); host-only compatibility events are forward-only and intentionally skip that path. Intended for first-party hosts that re-emit the events into their own telemetry stores. Both unrestricted and restricted events are forwarded, each tagged with a `restricted` discriminator; a backstop drops restricted events when restricted telemetry is disabled — using the process-global gate for ordinary events and an explicit session-scoped decision for host-only events. */
    @JsonProperty("enableGitHubTelemetryForwarding") Boolean enableGitHubTelemetryForwarding,
    /** Identity of the integrating host. Optional; omit it to keep the default attribution. */
    @JsonProperty("clientInfo") ConnectClientInfo clientInfo,
    /** Task kinds this connection can decode when observing session tasks. Omit to retain agent and shell compatibility. */
    @JsonProperty("supportedTaskKinds") List<TaskKind> supportedTaskKinds,
    /** Connection token; required when the server was started with COPILOT_CONNECTION_TOKEN */
    @JsonProperty("token") String token
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    ConnectParams {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    public static final class Builder {
        private Builder() {}
        private EntraAuthConfig entra;
        private Boolean entraTokenProvider;
        private Boolean enableGitHubTelemetryForwarding;
        private ConnectClientInfo clientInfo;
        private List<TaskKind> supportedTaskKinds;
        private String token;

        /**
         * Sets the {@code entra} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder entra(EntraAuthConfig value) {
            this.entra = value;
            return this;
        }

        /**
         * Sets the {@code entraTokenProvider} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder entraTokenProvider(Boolean value) {
            this.entraTokenProvider = value;
            return this;
        }

        /**
         * Sets the {@code enableGitHubTelemetryForwarding} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder enableGitHubTelemetryForwarding(Boolean value) {
            this.enableGitHubTelemetryForwarding = value;
            return this;
        }

        /**
         * Sets the {@code clientInfo} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder clientInfo(ConnectClientInfo value) {
            this.clientInfo = value;
            return this;
        }

        /**
         * Sets the {@code supportedTaskKinds} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder supportedTaskKinds(List<TaskKind> value) {
            this.supportedTaskKinds = value;
            return this;
        }

        /**
         * Sets the {@code token} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder token(String value) {
            this.token = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ConnectParams build() {
            return new ConnectParams(entra, entraTokenProvider, enableGitHubTelemetryForwarding, clientInfo, supportedTaskKinds, token);
        }
    }
}
