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
 * MCP Apps UI resource content for rendering in a sandboxed iframe
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolExecutionCompleteUIResource(
    /** The ui:// URI of the resource */
    @JsonProperty("uri") String uri,
    /** MIME type of the content */
    @JsonProperty("mimeType") String mimeType,
    /** HTML content as a string */
    @JsonProperty("text") String text,
    /** Base64-encoded HTML content */
    @JsonProperty("blob") String blob,
    /** Resource-level UI metadata (CSP, permissions, visual preferences) */
    @JsonProperty("_meta") ToolExecutionCompleteUIResourceMeta meta
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ToolExecutionCompleteUIResource {}

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
        private ToolExecutionCompleteUIResourceMeta meta;

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
        public Builder meta(ToolExecutionCompleteUIResourceMeta value) {
            this.meta = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ToolExecutionCompleteUIResource build() {
            if (uri == null) throw new IllegalStateException("Missing required property: uri");
            if (mimeType == null) throw new IllegalStateException("Missing required property: mimeType");
            return new ToolExecutionCompleteUIResource(uri, mimeType, text, blob, meta);
        }
    }
}
