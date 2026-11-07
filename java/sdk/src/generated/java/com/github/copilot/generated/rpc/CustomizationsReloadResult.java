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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Results of reloading discovered session customizations. Inspect outcomes for reloaded, skipped, or failed subsystems; a rejection may follow partial mutation. Changes to the model-facing prompt and tools apply on the next turn.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomizationsReloadResult(
    /** Warnings from skill discovery */
    @JsonProperty("warnings") List<String> warnings,
    /** Errors from any component that could not be refreshed */
    @JsonProperty("errors") List<String> errors,
    /** Outcome of each component in reload order; a skipped component was not configured or loaded */
    @JsonProperty("outcomes") List<CustomizationReloadOutcome> outcomes
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CustomizationsReloadResult {}

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
        private List<String> warnings;
        private List<String> errors;
        private List<CustomizationReloadOutcome> outcomes;

        /**
         * Sets the {@code warnings} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder warnings(List<String> value) {
            this.warnings = value;
            return this;
        }

        /**
         * Sets the {@code errors} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder errors(List<String> value) {
            this.errors = value;
            return this;
        }

        /**
         * Sets the {@code outcomes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder outcomes(List<CustomizationReloadOutcome> value) {
            this.outcomes = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CustomizationsReloadResult build() {
            if (warnings == null) throw new IllegalStateException("Missing required property: warnings");
            if (errors == null) throw new IllegalStateException("Missing required property: errors");
            if (outcomes == null) throw new IllegalStateException("Missing required property: outcomes");
            return new CustomizationsReloadResult(warnings, errors, outcomes);
        }
    }
}
