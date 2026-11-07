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
 * Standard MCP resource annotations plus preserved non-standard annotation fields.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpResourceAnnotations(
    /** Intended audience roles for this resource */
    @JsonProperty("audience") List<String> audience,
    /** Priority hint for model/client use */
    @JsonProperty("priority") Double priority,
    /** Last-modified timestamp hint */
    @JsonProperty("lastModified") String lastModified,
    /** Server-provided non-standard annotation fields preserved from the MCP response */
    @JsonProperty("additionalProperties") Map<String, Object> additionalProperties
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpResourceAnnotations {}

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
        private List<String> audience;
        private Double priority;
        private String lastModified;
        private Map<String, Object> additionalProperties;

        /**
         * Sets the {@code audience} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder audience(List<String> value) {
            this.audience = value;
            return this;
        }

        /**
         * Sets the {@code priority} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder priority(Double value) {
            this.priority = value;
            return this;
        }

        /**
         * Sets the {@code lastModified} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder lastModified(String value) {
            this.lastModified = value;
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
        public McpResourceAnnotations build() {
            return new McpResourceAnnotations(audience, priority, lastModified, additionalProperties);
        }
    }
}
