/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Capability negotiation snapshot
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpAppsDiagnoseCapability(
    /** Whether the session has the `mcp-apps` capability */
    @JsonProperty("sessionHasMcpApps") Boolean sessionHasMcpApps,
    /** Whether the MCP_APPS feature flag (or COPILOT_MCP_APPS env override) is on */
    @JsonProperty("featureFlagEnabled") Boolean featureFlagEnabled,
    /** Whether the runtime advertises `extensions.io.modelcontextprotocol/ui` to MCP servers */
    @JsonProperty("advertised") Boolean advertised
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpAppsDiagnoseCapability {}

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
        private Boolean sessionHasMcpApps;
        private Boolean featureFlagEnabled;
        private Boolean advertised;

        /**
         * Sets the {@code sessionHasMcpApps} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionHasMcpApps(Boolean value) {
            this.sessionHasMcpApps = value;
            return this;
        }

        /**
         * Sets the {@code featureFlagEnabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder featureFlagEnabled(Boolean value) {
            this.featureFlagEnabled = value;
            return this;
        }

        /**
         * Sets the {@code advertised} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder advertised(Boolean value) {
            this.advertised = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpAppsDiagnoseCapability build() {
            if (sessionHasMcpApps == null) throw new IllegalStateException("Missing required property: sessionHasMcpApps");
            if (featureFlagEnabled == null) throw new IllegalStateException("Missing required property: featureFlagEnabled");
            if (advertised == null) throw new IllegalStateException("Missing required property: advertised");
            return new McpAppsDiagnoseCapability(sessionHasMcpApps, featureFlagEnabled, advertised);
        }
    }
}
