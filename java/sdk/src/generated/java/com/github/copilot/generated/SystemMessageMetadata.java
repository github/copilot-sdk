/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Metadata about the prompt template and its construction
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SystemMessageMetadata(
    /** Version identifier of the prompt template or structured prompt layout used */
    @JsonProperty("promptVersion") String promptVersion,
    /** Template variables used when constructing the prompt */
    @JsonProperty("variables") Map<String, Object> variables
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SystemMessageMetadata {}

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
        private String promptVersion;
        private Map<String, Object> variables;

        /**
         * Sets the {@code promptVersion} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder promptVersion(String value) {
            this.promptVersion = value;
            return this;
        }

        /**
         * Sets the {@code variables} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder variables(Map<String, Object> value) {
            this.variables = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SystemMessageMetadata build() {
            return new SystemMessageMetadata(promptVersion, variables);
        }
    }
}
