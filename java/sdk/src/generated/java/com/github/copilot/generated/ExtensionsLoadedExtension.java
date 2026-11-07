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
 * A single extension discovered by `session.extensions_loaded`, including qualified ID, source, and current status.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtensionsLoadedExtension(
    /** Source-qualified extension ID (e.g., 'project:my-ext', 'user:auth-helper', 'plugin:my-plugin:my-ext') */
    @JsonProperty("id") String id,
    /** Extension name (directory name) */
    @JsonProperty("name") String name,
    /** Discovery source */
    @JsonProperty("source") ExtensionsLoadedExtensionSource source,
    /** Current status: running, disabled, failed, or starting */
    @JsonProperty("status") ExtensionsLoadedExtensionStatus status
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ExtensionsLoadedExtension {}

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
        private String id;
        private String name;
        private ExtensionsLoadedExtensionSource source;
        private ExtensionsLoadedExtensionStatus status;

        /**
         * Sets the {@code id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder id(String value) {
            this.id = value;
            return this;
        }

        /**
         * Sets the {@code name} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder name(String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder source(ExtensionsLoadedExtensionSource value) {
            this.source = value;
            return this;
        }

        /**
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder status(ExtensionsLoadedExtensionStatus value) {
            this.status = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ExtensionsLoadedExtension build() {
            if (id == null) throw new IllegalStateException("Missing required property: id");
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (source == null) throw new IllegalStateException("Missing required property: source");
            if (status == null) throw new IllegalStateException("Missing required property: status");
            return new ExtensionsLoadedExtension(id, name, source, status);
        }
    }
}
