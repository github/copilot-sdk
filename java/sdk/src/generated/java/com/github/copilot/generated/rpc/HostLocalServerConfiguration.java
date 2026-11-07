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
 * Normalized local WebSocket listener settings.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record HostLocalServerConfiguration(
    /** Hostname or IP address to bind. */
    @JsonProperty("hostname") String hostname,
    /** Port to bind, with zero requesting OS allocation. */
    @JsonProperty("port") Long port,
    /** Secret connection token, absent when authentication is disabled. */
    @JsonProperty("token") String token,
    /** Whether the listener requires token authentication. */
    @JsonProperty("requireConnectionToken") Boolean requireConnectionToken
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    HostLocalServerConfiguration {}

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
        private String hostname;
        private Long port;
        private String token;
        private Boolean requireConnectionToken;

        /**
         * Sets the {@code hostname} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder hostname(String value) {
            this.hostname = value;
            return this;
        }

        /**
         * Sets the {@code port} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder port(Long value) {
            this.port = value;
            return this;
        }

        /**
         * Sets the {@code token} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder token(String value) {
            this.token = value;
            return this;
        }

        /**
         * Sets the {@code requireConnectionToken} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder requireConnectionToken(Boolean value) {
            this.requireConnectionToken = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public HostLocalServerConfiguration build() {
            if (hostname == null) throw new IllegalStateException("Missing required property: hostname");
            if (port == null) throw new IllegalStateException("Missing required property: port");
            if (requireConnectionToken == null) throw new IllegalStateException("Missing required property: requireConnectionToken");
            return new HostLocalServerConfiguration(hostname, port, token, requireConnectionToken);
        }
    }
}
