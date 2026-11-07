/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * An MCP resource descriptor (spec `Resource`): URI, name, and optional title, description, MIME type, size, icons, annotations, and metadata. Server-provided fields outside the standard descriptor shape are exposed under `additionalProperties`.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpResource(
    /** The resource URI (e.g. ui://... or file:///...) */
    @JsonProperty("uri") String uri,
    /** The programmatic name of the resource */
    @JsonProperty("name") String name,
    /** Optional human-readable display title */
    @JsonProperty("title") String title,
    /** Optional description of what this resource represents */
    @JsonProperty("description") String description,
    /** MIME type of the resource, if known */
    @JsonProperty("mimeType") String mimeType,
    /** Resource size in bytes, when known */
    @JsonProperty("size") Long size,
    /** Icons associated with this resource */
    @JsonProperty("icons") List<McpResourceIcon> icons,
    /** Model/client annotations associated with this resource */
    @JsonProperty("annotations") McpResourceAnnotations annotations,
    /** Resource-level metadata */
    @JsonProperty("_meta") Map<String, Object> meta,
    /** Server-provided non-standard descriptor fields preserved from the MCP response */
    @JsonProperty("additionalProperties") Map<String, Object> additionalProperties
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpResource {}

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
        private String uri;
        private String name;
        private String title;
        private String description;
        private String mimeType;
        private Long size;
        private List<McpResourceIcon> icons;
        private McpResourceAnnotations annotations;
        private Map<String, Object> meta;
        private Map<String, Object> additionalProperties;

        /**
         * Sets the {@code uri} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder uri(String value) {
            this.uri = value;
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
         * Sets the {@code title} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder title(String value) {
            this.title = value;
            return this;
        }

        /**
         * Sets the {@code description} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder description(String value) {
            this.description = value;
            return this;
        }

        /**
         * Sets the {@code mimeType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mimeType(String value) {
            this.mimeType = value;
            return this;
        }

        /**
         * Sets the {@code size} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder size(Long value) {
            this.size = value;
            return this;
        }

        /**
         * Sets the {@code icons} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder icons(List<McpResourceIcon> value) {
            this.icons = value;
            return this;
        }

        /**
         * Sets the {@code annotations} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder annotations(McpResourceAnnotations value) {
            this.annotations = value;
            return this;
        }

        /**
         * Sets the {@code _meta} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder meta(Map<String, Object> value) {
            this.meta = value;
            return this;
        }

        /**
         * Sets the {@code additionalProperties} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder additionalProperties(Map<String, Object> value) {
            this.additionalProperties = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpResource build() {
            if (uri == null) throw new IllegalStateException("Missing required property: uri");
            if (name == null) throw new IllegalStateException("Missing required property: name");
            return new McpResource(uri, name, title, description, mimeType, size, icons, annotations, meta, additionalProperties);
        }
    }
}
