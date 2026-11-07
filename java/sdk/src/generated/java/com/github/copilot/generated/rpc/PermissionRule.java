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
 * A permission approval or denial rule matched against a tool request, identified by a rule kind with an optional argument value.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PermissionRule(
    /** The rule kind, such as Shell or GitHubMCP */
    @JsonProperty("kind") String kind,
    /** Argument value matched against the request, or null when the rule kind has no argument (e.g. 'read', 'write', 'memory'). */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("argument") String argument
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public PermissionRule {}

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
        private String kind;
        private String argument;
        private boolean $presence1;

        /**
         * Sets the {@code kind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder kind(String value) {
            this.kind = value;
            return this;
        }

        /**
         * Sets the {@code argument} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder argument(String value) {
            this.argument = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public PermissionRule build() {
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            if (!$presence1) throw new IllegalStateException("Missing required property: argument");
            return new PermissionRule(kind, argument);
        }
    }
}
