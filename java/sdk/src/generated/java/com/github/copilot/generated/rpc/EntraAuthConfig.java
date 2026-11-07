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
 * The host application's Microsoft Entra registration, shared by its sessions. This identifies the application requesting tokens, not their target resource.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record EntraAuthConfig(
    /** Application (client) ID of the host's Microsoft Entra registration. Must be nonblank. The runtime does not supply a default application. */
    @JsonProperty("clientId") String clientId,
    /** Application name reported by OneAuth to Microsoft Auth Telemetry System (MATS). Omit or leave blank to disable MATS telemetry without disabling authentication. OneAuth normalizes the name and may group applications into shared event tables. */
    @JsonProperty("matsAppName") String matsAppName
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public EntraAuthConfig {}

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
        private String clientId;
        private String matsAppName;

        /**
         * Sets the {@code clientId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder clientId(String value) {
            this.clientId = value;
            return this;
        }

        /**
         * Sets the {@code matsAppName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder matsAppName(String value) {
            this.matsAppName = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public EntraAuthConfig build() {
            if (clientId == null) throw new IllegalStateException("Missing required property: clientId");
            return new EntraAuthConfig(clientId, matsAppName);
        }
    }

    /**
     * Retains the constructor published in Java SDK 1.0.28.
     * @deprecated Use named construction via {@link #builder()}.
     * @param clientId Application (client) ID of the host's Microsoft Entra registration. Must be nonblank. The runtime does not supply a default application.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public EntraAuthConfig(
        String clientId
    ) {
        this(clientId, null);
    }
}
