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
 * Identity of the integrating host, declared once on the `server.connect` handshake so telemetry from this connection is attributed to a single, consistent surface. All fields are optional; omit them to keep the default attribution.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record ConnectClientInfo(
    /** Name of the host editor, e.g. `"vscode"`. */
    @JsonProperty("editorName") String editorName,
    /** Version of the host editor, e.g. `"1.124.2"`. Ignored unless it looks like a version string. */
    @JsonProperty("editorVersion") String editorVersion,
    /** Name of the Copilot extension within the host, e.g. `"copilot-chat"`. */
    @JsonProperty("extensionName") String extensionName,
    /** Version of the Copilot extension within the host, e.g. `"0.54.0"`. Ignored unless it looks like a version string. */
    @JsonProperty("extensionVersion") String extensionVersion
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    ConnectClientInfo {}

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
        private String editorName;
        private String editorVersion;
        private String extensionName;
        private String extensionVersion;

        /**
         * Sets the {@code editorName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder editorName(String value) {
            this.editorName = value;
            return this;
        }

        /**
         * Sets the {@code editorVersion} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder editorVersion(String value) {
            this.editorVersion = value;
            return this;
        }

        /**
         * Sets the {@code extensionName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder extensionName(String value) {
            this.extensionName = value;
            return this;
        }

        /**
         * Sets the {@code extensionVersion} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder extensionVersion(String value) {
            this.extensionVersion = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ConnectClientInfo build() {
            return new ConnectClientInfo(editorName, editorVersion, extensionName, extensionVersion);
        }
    }
}
