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
 * The `status.get` response, including optional cached error-reporting policy.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProtocolStatusResponse(
    /** The runtime package version. */
    @JsonProperty("version") String version,
    /** The runtime SDK protocol version. */
    @JsonProperty("protocolVersion") Long protocolVersion,
    /** Cached host policy. This field does not grant ownership or confirm network delivery. */
    @JsonProperty("hostDiagnostics") HostDiagnostics hostDiagnostics
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ProtocolStatusResponse {}

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
        private String version;
        private Long protocolVersion;
        private HostDiagnostics hostDiagnostics;

        /**
         * Sets the {@code version} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder version(String value) {
            this.version = value;
            return this;
        }

        /**
         * Sets the {@code protocolVersion} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder protocolVersion(Long value) {
            this.protocolVersion = value;
            return this;
        }

        /**
         * Sets the {@code hostDiagnostics} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder hostDiagnostics(HostDiagnostics value) {
            this.hostDiagnostics = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ProtocolStatusResponse build() {
            if (version == null) throw new IllegalStateException("Missing required property: version");
            if (protocolVersion == null) throw new IllegalStateException("Missing required property: protocolVersion");
            return new ProtocolStatusResponse(version, protocolVersion, hostDiagnostics);
        }
    }
}
