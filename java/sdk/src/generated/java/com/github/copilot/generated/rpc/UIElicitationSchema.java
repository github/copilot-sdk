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
 * JSON Schema describing the form fields to present to the user
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record UIElicitationSchema(
    /** Schema type indicator (always 'object') */
    @JsonProperty("type") String type,
    /** Form field definitions, keyed by field name */
    @JsonProperty("properties") Map<String, Object> properties,
    /** List of required field names */
    @JsonProperty("required") List<String> required
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public UIElicitationSchema {}

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
        private String type;
        private Map<String, Object> properties;
        private List<String> required;

        /**
         * Sets the {@code type} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder type(String value) {
            this.type = value;
            return this;
        }

        /**
         * Sets the {@code properties} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder properties(Map<String, Object> value) {
            this.properties = value;
            return this;
        }

        /**
         * Sets the {@code required} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder required(List<String> value) {
            this.required = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public UIElicitationSchema build() {
            if (type == null) throw new IllegalStateException("Missing required property: type");
            if (properties == null) throw new IllegalStateException("Missing required property: properties");
            return new UIElicitationSchema(type, properties, required);
        }
    }
}
