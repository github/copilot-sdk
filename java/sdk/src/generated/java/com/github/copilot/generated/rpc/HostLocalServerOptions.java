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
 * Local WebSocket transport options.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record HostLocalServerOptions(
    /** Listener hostname. Defaults to 127.0.0.1; explicit non-loopback binds are allowed. */
    @JsonProperty("hostname") String hostname,
    /** Listener port. Omitted or zero requests an OS-allocated port. */
    @JsonProperty("port") Long port,
    /** Nonempty connection token. Generated randomly when required and omitted. */
    @JsonProperty("token") String token,
    /** Require token authentication (default true). Cannot be false with a token. */
    @JsonProperty("requireConnectionToken") Boolean requireConnectionToken
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public HostLocalServerOptions {}

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
        public HostLocalServerOptions build() {
            return new HostLocalServerOptions(hostname, port, token, requireConnectionToken);
        }
    }
}
