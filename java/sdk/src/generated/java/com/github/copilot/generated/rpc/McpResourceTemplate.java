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
 * An MCP resource template descriptor (spec `ResourceTemplate`): an RFC 6570 URI template, name, and optional title, description, MIME type, icons, annotations, and metadata. Server-provided fields outside the standard descriptor shape are exposed under `additionalProperties`.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpResourceTemplate(
    /** An RFC 6570 URI template for constructing resource URIs */
    @JsonProperty("uriTemplate") String uriTemplate,
    /** The programmatic name of the resource template */
    @JsonProperty("name") String name,
    /** Optional human-readable display title */
    @JsonProperty("title") String title,
    /** Optional description of what this template is for */
    @JsonProperty("description") String description,
    /** MIME type for resources matching this template, if uniform */
    @JsonProperty("mimeType") String mimeType,
    /** Icons associated with resources matching this template */
    @JsonProperty("icons") List<McpResourceIcon> icons,
    /** Model/client annotations associated with this template */
    @JsonProperty("annotations") McpResourceAnnotations annotations,
    /** Resource-template-level metadata */
    @JsonProperty("_meta") Map<String, Object> meta,
    /** Server-provided non-standard descriptor fields preserved from the MCP response */
    @JsonProperty("additionalProperties") Map<String, Object> additionalProperties
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpResourceTemplate {}

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
        private String uriTemplate;
        private String name;
        private String title;
        private String description;
        private String mimeType;
        private List<McpResourceIcon> icons;
        private McpResourceAnnotations annotations;
        private Map<String, Object> meta;
        private Map<String, Object> additionalProperties;

        /**
         * Sets the {@code uriTemplate} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder uriTemplate(String value) {
            this.uriTemplate = value;
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
        public McpResourceTemplate build() {
            if (uriTemplate == null) throw new IllegalStateException("Missing required property: uriTemplate");
            if (name == null) throw new IllegalStateException("Missing required property: name");
            return new McpResourceTemplate(uriTemplate, name, title, description, mimeType, icons, annotations, meta, additionalProperties);
        }
    }
}
