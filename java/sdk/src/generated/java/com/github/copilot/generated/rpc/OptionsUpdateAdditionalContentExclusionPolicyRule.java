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
import javax.annotation.processing.Generated;

/**
 * Single content-exclusion rule supplied to `session.options.update`, with paths, match conditions, and source.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record OptionsUpdateAdditionalContentExclusionPolicyRule(
    /** Path patterns covered by this rule. */
    @JsonProperty("paths") List<String> paths,
    /** Conditions of which at least one must match. */
    @JsonProperty("ifAnyMatch") List<String> ifAnyMatch,
    /** Conditions none of which may match. */
    @JsonProperty("ifNoneMatch") List<String> ifNoneMatch,
    /** Source descriptor for a `session.options.update` content-exclusion rule, with source name and type. */
    @JsonProperty("source") OptionsUpdateAdditionalContentExclusionPolicyRuleSource source
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public OptionsUpdateAdditionalContentExclusionPolicyRule {}

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
        private List<String> paths;
        private List<String> ifAnyMatch;
        private List<String> ifNoneMatch;
        private OptionsUpdateAdditionalContentExclusionPolicyRuleSource source;

        /**
         * Sets the {@code paths} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder paths(List<String> value) {
            this.paths = value;
            return this;
        }

        /**
         * Sets the {@code ifAnyMatch} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder ifAnyMatch(List<String> value) {
            this.ifAnyMatch = value;
            return this;
        }

        /**
         * Sets the {@code ifNoneMatch} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder ifNoneMatch(List<String> value) {
            this.ifNoneMatch = value;
            return this;
        }

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder source(OptionsUpdateAdditionalContentExclusionPolicyRuleSource value) {
            this.source = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public OptionsUpdateAdditionalContentExclusionPolicyRule build() {
            if (paths == null) throw new IllegalStateException("Missing required property: paths");
            if (source == null) throw new IllegalStateException("Missing required property: source");
            return new OptionsUpdateAdditionalContentExclusionPolicyRule(paths, ifAnyMatch, ifNoneMatch, source);
        }
    }
}
