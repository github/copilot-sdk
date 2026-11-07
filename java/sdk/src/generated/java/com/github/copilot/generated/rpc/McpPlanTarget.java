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
 * Where a plan would be written.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpPlanTarget(
    /** Configuration scope the plan targets. */
    @JsonProperty("scope") McpPlanScope scope,
    /** Configuration key the server would be recorded under within that scope. */
    @JsonProperty("configKey") String configKey
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpPlanTarget {}

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
        private McpPlanScope scope;
        private String configKey;

        /**
         * Sets the {@code scope} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder scope(McpPlanScope value) {
            this.scope = value;
            return this;
        }

        /**
         * Sets the {@code configKey} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder configKey(String value) {
            this.configKey = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpPlanTarget build() {
            if (scope == null) throw new IllegalStateException("Missing required property: scope");
            if (configKey == null) throw new IllegalStateException("Missing required property: configKey");
            return new McpPlanTarget(scope, configKey);
        }
    }
}
