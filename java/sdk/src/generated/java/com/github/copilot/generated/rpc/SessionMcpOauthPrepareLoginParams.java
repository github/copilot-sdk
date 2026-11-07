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
 * Effect-free preparation bound to the existing local session, requester and installation, with frozen options.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMcpOauthPrepareLoginParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Name recorded by the authoritative owned installation receipt. */
    @JsonProperty("serverName") String serverName,
    /** Exact installation identity from owned inventory, never a server-name alias. */
    @JsonProperty("expectedInstallationId") String expectedInstallationId,
    /** Request a new authorisation rather than accepting a usable cached grant. */
    @JsonProperty("forceReauth") Boolean forceReauth,
    /** Display name used by the incumbent OAuth client-registration flow. */
    @JsonProperty("clientName") String clientName,
    /** Text shown on the loopback callback page after successful authorisation. */
    @JsonProperty("callbackSuccessMessage") String callbackSuccessMessage
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionMcpOauthPrepareLoginParams {}

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
        private String sessionId;
        private String serverName;
        private String expectedInstallationId;
        private Boolean forceReauth;
        private String clientName;
        private String callbackSuccessMessage;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code serverName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder serverName(String value) {
            this.serverName = value;
            return this;
        }

        /**
         * Sets the {@code expectedInstallationId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder expectedInstallationId(String value) {
            this.expectedInstallationId = value;
            return this;
        }

        /**
         * Sets the {@code forceReauth} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder forceReauth(Boolean value) {
            this.forceReauth = value;
            return this;
        }

        /**
         * Sets the {@code clientName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder clientName(String value) {
            this.clientName = value;
            return this;
        }

        /**
         * Sets the {@code callbackSuccessMessage} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder callbackSuccessMessage(String value) {
            this.callbackSuccessMessage = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionMcpOauthPrepareLoginParams build() {
            if (serverName == null) throw new IllegalStateException("Missing required property: serverName");
            if (expectedInstallationId == null) throw new IllegalStateException("Missing required property: expectedInstallationId");
            return new SessionMcpOauthPrepareLoginParams(sessionId, serverName, expectedInstallationId, forceReauth, clientName, callbackSuccessMessage);
        }
    }
}
