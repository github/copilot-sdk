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
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * Server liveness response, including the echoed message, current server timestamp, and protocol version.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PingResult(
    /** Echoed message (or default greeting) */
    @JsonProperty("message") String message,
    /** ISO 8601 timestamp when the server handled the ping */
    @JsonProperty("timestamp") OffsetDateTime timestamp,
    /** Server protocol version number */
    @JsonProperty("protocolVersion") Long protocolVersion
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public PingResult {}

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
        private String message;
        private OffsetDateTime timestamp;
        private Long protocolVersion;

        /**
         * Sets the {@code message} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder message(String value) {
            this.message = value;
            return this;
        }

        /**
         * Sets the {@code timestamp} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder timestamp(OffsetDateTime value) {
            this.timestamp = value;
            return this;
        }

        /**
         * Sets the {@code protocolVersion} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder protocolVersion(Long value) {
            this.protocolVersion = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public PingResult build() {
            if (message == null) throw new IllegalStateException("Missing required property: message");
            if (timestamp == null) throw new IllegalStateException("Missing required property: timestamp");
            if (protocolVersion == null) throw new IllegalStateException("Missing required property: protocolVersion");
            return new PingResult(message, timestamp, protocolVersion);
        }
    }
}
