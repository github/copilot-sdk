/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Browser permission metadata for an MCP Apps UI resource, including camera, microphone, geolocation, and clipboard-write.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolExecutionCompleteUIResourceMetaUIPermissions(
    /** Marker object for camera permission on an MCP Apps UI resource. */
    @JsonProperty("camera") ToolExecutionCompleteUIResourceMetaUIPermissionsCamera camera,
    /** Marker object for microphone permission on an MCP Apps UI resource. */
    @JsonProperty("microphone") ToolExecutionCompleteUIResourceMetaUIPermissionsMicrophone microphone,
    /** Marker object for geolocation permission on an MCP Apps UI resource. */
    @JsonProperty("geolocation") ToolExecutionCompleteUIResourceMetaUIPermissionsGeolocation geolocation,
    /** Marker object for clipboard-write permission on an MCP Apps UI resource. */
    @JsonProperty("clipboardWrite") ToolExecutionCompleteUIResourceMetaUIPermissionsClipboardWrite clipboardWrite
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ToolExecutionCompleteUIResourceMetaUIPermissions {}

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
        private ToolExecutionCompleteUIResourceMetaUIPermissionsCamera camera;
        private ToolExecutionCompleteUIResourceMetaUIPermissionsMicrophone microphone;
        private ToolExecutionCompleteUIResourceMetaUIPermissionsGeolocation geolocation;
        private ToolExecutionCompleteUIResourceMetaUIPermissionsClipboardWrite clipboardWrite;

        /**
         * Sets the {@code camera} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder camera(ToolExecutionCompleteUIResourceMetaUIPermissionsCamera value) {
            this.camera = value;
            return this;
        }

        /**
         * Sets the {@code microphone} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder microphone(ToolExecutionCompleteUIResourceMetaUIPermissionsMicrophone value) {
            this.microphone = value;
            return this;
        }

        /**
         * Sets the {@code geolocation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder geolocation(ToolExecutionCompleteUIResourceMetaUIPermissionsGeolocation value) {
            this.geolocation = value;
            return this;
        }

        /**
         * Sets the {@code clipboardWrite} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder clipboardWrite(ToolExecutionCompleteUIResourceMetaUIPermissionsClipboardWrite value) {
            this.clipboardWrite = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ToolExecutionCompleteUIResourceMetaUIPermissions build() {
            return new ToolExecutionCompleteUIResourceMetaUIPermissions(camera, microphone, geolocation, clipboardWrite);
        }
    }
}
