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
 * Exact registry identity of a downloadable model.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record LocalModelAzureMlIdentity(
    /** Azure ML registry name. */
    @JsonProperty("registry") String registry,
    /** Exact model family name. */
    @JsonProperty("name") String name,
    /** Exact model asset version. */
    @JsonProperty("version") String version
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public LocalModelAzureMlIdentity {}

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
        private String registry;
        private String name;
        private String version;

        /**
         * Sets the {@code registry} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder registry(String value) {
            this.registry = value;
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
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public LocalModelAzureMlIdentity build() {
            if (registry == null) throw new IllegalStateException("Missing required property: registry");
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (version == null) throw new IllegalStateException("Missing required property: version");
            return new LocalModelAzureMlIdentity(registry, name, version);
        }
    }
}
