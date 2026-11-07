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
 * An MCP prompt icon with standard size hints and preserved non-standard fields.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpPromptIcon(
    /** Icon URI */
    @JsonProperty("src") String src,
    /** Icon MIME type, when known */
    @JsonProperty("mimeType") String mimeType,
    /** Icon sizes, such as `48x48` or `any` */
    @JsonProperty("sizes") List<String> sizes,
    /** Theme hint for this icon */
    @JsonProperty("theme") String theme,
    /** Server-provided non-standard icon fields */
    @JsonProperty("additionalProperties") Map<String, Object> additionalProperties
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpPromptIcon {}

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
        private String src;
        private String mimeType;
        private List<String> sizes;
        private String theme;
        private Map<String, Object> additionalProperties;

        /**
         * Sets the {@code src} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder src(String value) {
            this.src = value;
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
         * Sets the {@code sizes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sizes(List<String> value) {
            this.sizes = value;
            return this;
        }

        /**
         * Sets the {@code theme} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder theme(String value) {
            this.theme = value;
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
        public McpPromptIcon build() {
            if (src == null) throw new IllegalStateException("Missing required property: src");
            return new McpPromptIcon(src, mimeType, sizes, theme, additionalProperties);
        }
    }
}
