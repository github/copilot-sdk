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
 * One Registry string-valued configuration entry for the selected transport.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpInstallationInput(
    /** Exact category declared by the selected choice. */
    @JsonProperty("category") McpPlanValueCategory category,
    /** Exact key declared by the selected choice. */
    @JsonProperty("key") String key,
    /** Explicit non-secret value. Secret placeholders use a separate input channel. */
    @JsonProperty("value") String value
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpInstallationInput {}

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
        private McpPlanValueCategory category;
        private String key;
        private String value;

        /**
         * Sets the {@code category} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder category(McpPlanValueCategory value) {
            this.category = value;
            return this;
        }

        /**
         * Sets the {@code key} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder key(String value) {
            this.key = value;
            return this;
        }

        /**
         * Sets the {@code value} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder value(String value) {
            this.value = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpInstallationInput build() {
            if (category == null) throw new IllegalStateException("Missing required property: category");
            if (key == null) throw new IllegalStateException("Missing required property: key");
            if (value == null) throw new IllegalStateException("Missing required property: value");
            return new McpInstallationInput(category, key, value);
        }
    }
}
