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
 * Handshake result reporting the server's protocol version and package version on success.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record ConnectResult(
    /** Always true on success */
    @JsonProperty("ok") Boolean ok,
    /** Server protocol version number */
    @JsonProperty("protocolVersion") Long protocolVersion,
    /** Server package version */
    @JsonProperty("version") String version,
    /** Acknowledges that this connection's host-owned Entra token provider is enabled. Clients requesting it must reject a missing or false acknowledgement. */
    @CopilotExperimental
    @JsonProperty("entraTokenProvider") Boolean entraTokenProvider,
    /** Task kinds the server may return to this connection. */
    @JsonProperty("taskKinds") List<TaskKind> taskKinds
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    ConnectResult {}

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
        private Boolean ok;
        private Long protocolVersion;
        private String version;
        private Boolean entraTokenProvider;
        private List<TaskKind> taskKinds;

        /**
         * Sets the {@code ok} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder ok(Boolean value) {
            this.ok = value;
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
         * Sets the {@code version} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder version(String value) {
            this.version = value;
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
         * Sets the {@code taskKinds} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder taskKinds(List<TaskKind> value) {
            this.taskKinds = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ConnectResult build() {
            if (ok == null) throw new IllegalStateException("Missing required property: ok");
            if (protocolVersion == null) throw new IllegalStateException("Missing required property: protocolVersion");
            if (version == null) throw new IllegalStateException("Missing required property: version");
            return new ConnectResult(ok, protocolVersion, version, entraTokenProvider, taskKinds);
        }
    }
}
