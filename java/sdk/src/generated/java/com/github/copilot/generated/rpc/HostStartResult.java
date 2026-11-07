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
 * Listener readiness, returned only after binding and the supervised participant's SDK handshake.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record HostStartResult(
    /** Caller-generated listener UUID. */
    @JsonProperty("hostId") String hostId,
    /** Actual bound WebSocket URL, including the allocated port. */
    @JsonProperty("url") String url,
    /** Secret connection token, absent when authentication is disabled. */
    @JsonProperty("token") String token,
    /** GitHub Mission Control environment ID, present when its relay transport is ready. */
    @JsonProperty("environmentId") String environmentId,
    /** Separate host process ID, when provided by a legacy runtime. Absent for in-process listeners. */
    @JsonProperty("pid") Long pid
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public HostStartResult {}

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
        private String url;
        private String token;
        private String environmentId;
        private Long pid;

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
         * Sets the {@code url} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder url(String value) {
            this.url = value;
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
         * Sets the {@code environmentId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder environmentId(String value) {
            this.environmentId = value;
            return this;
        }

        /**
         * Sets the {@code pid} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder pid(Long value) {
            this.pid = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public HostStartResult build() {
            if (hostId == null) throw new IllegalStateException("Missing required property: hostId");
            return new HostStartResult(hostId, url, token, environmentId, pid);
        }
    }
}
