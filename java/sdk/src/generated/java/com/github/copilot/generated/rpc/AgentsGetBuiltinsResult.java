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
 * The agents this runtime ships, named so a consumer can tell them apart from authored ones.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record AgentsGetBuiltinsResult(
    /** Every agent name this runtime ships. */
    @JsonProperty("names") List<String> names,
    /** The subset of `names` a user is allowed to turn off. A shipped agent outside this list is always active and a client should not offer a toggle for it. */
    @JsonProperty("disableableNames") List<String> disableableNames,
    /** The subset of `names` defined by a shipped YAML definition. The remainder are special-cased in code and have no definition to load. */
    @JsonProperty("yamlBasedNames") List<String> yamlBasedNames
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    AgentsGetBuiltinsResult {}

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
        private List<String> names;
        private List<String> disableableNames;
        private List<String> yamlBasedNames;

        /**
         * Sets the {@code names} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder names(List<String> value) {
            this.names = value;
            return this;
        }

        /**
         * Sets the {@code disableableNames} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder disableableNames(List<String> value) {
            this.disableableNames = value;
            return this;
        }

        /**
         * Sets the {@code yamlBasedNames} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder yamlBasedNames(List<String> value) {
            this.yamlBasedNames = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AgentsGetBuiltinsResult build() {
            if (names == null) throw new IllegalStateException("Missing required property: names");
            if (disableableNames == null) throw new IllegalStateException("Missing required property: disableableNames");
            if (yamlBasedNames == null) throw new IllegalStateException("Missing required property: yamlBasedNames");
            return new AgentsGetBuiltinsResult(names, disableableNames, yamlBasedNames);
        }
    }
}
