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
 * A secret a transport choice needs, referenced by placeholder. No secret value ever appears in a plan, and the placeholder resolves against the keychain only when a plan is applied.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpPlanSecretPlaceholder(
    /** Key the secret is supplied under. Inert untrusted data. */
    @JsonProperty("key") String key,
    /** The runtime-assigned `${secret:<id>}` placeholder written into configuration in place of the value. */
    @JsonProperty("placeholder") String placeholder,
    /** Human-readable label from the card. Inert untrusted text. */
    @JsonProperty("title") String title
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpPlanSecretPlaceholder {}

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
        private String key;
        private String placeholder;
        private String title;

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
         * Sets the {@code placeholder} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder placeholder(String value) {
            this.placeholder = value;
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
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpPlanSecretPlaceholder build() {
            if (key == null) throw new IllegalStateException("Missing required property: key");
            if (placeholder == null) throw new IllegalStateException("Missing required property: placeholder");
            return new McpPlanSecretPlaceholder(key, placeholder, title);
        }
    }
}
