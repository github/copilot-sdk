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
 * One callable decision model.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DecisionModelInfo(
    /** Model id to pass as `model` to `decisions.ask`. */
    @JsonProperty("id") String id,
    /** Display name. */
    @JsonProperty("name") String name,
    /** Whether `decisions.ask` uses this model when `model` is omitted. */
    @JsonProperty("isDefault") Boolean isDefault,
    /** Effective limits callers can rely on: the stricter of the backend's and the runtime's. */
    @JsonProperty("limits") DecisionModelLimits limits,
    /** Whether the backend can refuse individual questions. */
    @JsonProperty("supportsRefusals") Boolean supportsRefusals
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public DecisionModelInfo {}

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
        private String id;
        private String name;
        private Boolean isDefault;
        private DecisionModelLimits limits;
        private Boolean supportsRefusals;

        /**
         * Sets the {@code id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder id(String value) {
            this.id = value;
            return this;
        }

        /**
         * Sets the {@code name} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder name(String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the {@code isDefault} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder isDefault(Boolean value) {
            this.isDefault = value;
            return this;
        }

        /**
         * Sets the {@code limits} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder limits(DecisionModelLimits value) {
            this.limits = value;
            return this;
        }

        /**
         * Sets the {@code supportsRefusals} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder supportsRefusals(Boolean value) {
            this.supportsRefusals = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public DecisionModelInfo build() {
            if (id == null) throw new IllegalStateException("Missing required property: id");
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (isDefault == null) throw new IllegalStateException("Missing required property: isDefault");
            if (limits == null) throw new IllegalStateException("Missing required property: limits");
            if (supportsRefusals == null) throw new IllegalStateException("Missing required property: supportsRefusals");
            return new DecisionModelInfo(id, name, isDefault, limits, supportsRefusals);
        }
    }
}
