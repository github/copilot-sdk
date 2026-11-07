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
 * A JSON Schema output contract. OpenAI receives the name, description, schema and strict setting; Anthropic receives the schema in output_config.format and always uses its native strict enforcement.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record JsonSchemaResponseFormat(
    /** Name of the output schema, subject to the provider's naming restrictions. */
    @JsonProperty("name") String name,
    /** JSON Schema passed unchanged to the inference provider. Schemas larger than 32 MiB when JSON-encoded are rejected before admission, using the runtime's existing request-size ceiling. This is not a guarantee that the entire model request fits. Supported keywords and schema restrictions are determined by the provider. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("schema") Object schema,
    /** Optional description passed to OpenAI providers. */
    @JsonProperty("description") String description,
    /** Optional strict enforcement setting for OpenAI providers. Omitted uses the provider default. Anthropic always enforces its supported schema subset. */
    @JsonProperty("strict") Boolean strict
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public JsonSchemaResponseFormat {}

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
        private String name;
        private Object schema;
        private boolean $presence1;
        private String description;
        private Boolean strict;

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
         * Sets the {@code schema} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder schema(Object value) {
            this.schema = value;
            this.$presence1 = true;
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
         * Sets the {@code strict} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder strict(Boolean value) {
            this.strict = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public JsonSchemaResponseFormat build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (!$presence1) throw new IllegalStateException("Missing required property: schema");
            return new JsonSchemaResponseFormat(name, schema, description, strict);
        }
    }
}
