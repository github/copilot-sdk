/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * MCP Apps resource content with URI, optional MIME type, text or base64 blob, and resource metadata.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpAppsResourceContent(
    /** The resource URI (typically ui://...) */
    @JsonProperty("uri") String uri,
    /** MIME type of the content */
    @JsonProperty("mimeType") String mimeType,
    /** Text content (e.g. HTML) */
    @JsonProperty("text") String text,
    /** Base64-encoded binary content */
    @JsonProperty("blob") String blob,
    /** Resource-level metadata (CSP, permissions, etc.) */
    @JsonProperty("_meta") Map<String, Object> meta
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpAppsResourceContent {}

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
        private String mimeType;
        private String text;
        private String blob;
        private Map<String, Object> meta;

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
         * Sets the {@code text} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder text(String value) {
            this.text = value;
            return this;
        }

        /**
         * Sets the {@code blob} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder blob(String value) {
            this.blob = value;
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
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpAppsResourceContent build() {
            if (uri == null) throw new IllegalStateException("Missing required property: uri");
            return new McpAppsResourceContent(uri, mimeType, text, blob, meta);
        }
    }
}
