/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * One rule or limit a request broke.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DecisionViolation(
    /** Where in the request, for example `questions[2].options`. */
    @JsonProperty("path") String path,
    /** Stable rule identifier, for example `minOptions` or `requestBytes`. */
    @JsonProperty("rule") String rule,
    /** The value the request had, when numeric. */
    @JsonProperty("actual") Long actual,
    /** The limit it broke, when numeric. */
    @JsonProperty("limit") Long limit
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public DecisionViolation {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static final class Builder {
        private Builder() {}
        private String path;
        private String rule;
        private Long actual;
        private Long limit;

        /**
         * Sets the {@code path} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder path(String value) {
            this.path = value;
            return this;
        }

        /**
         * Sets the {@code rule} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder rule(String value) {
            this.rule = value;
            return this;
        }

        /**
         * Sets the {@code actual} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder actual(Long value) {
            this.actual = value;
            return this;
        }

        /**
         * Sets the {@code limit} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder limit(Long value) {
            this.limit = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public DecisionViolation build() {
            if (path == null) throw new IllegalStateException("Missing required property: path");
            if (rule == null) throw new IllegalStateException("Missing required property: rule");
            return new DecisionViolation(path, rule, actual, limit);
        }
    }
}
