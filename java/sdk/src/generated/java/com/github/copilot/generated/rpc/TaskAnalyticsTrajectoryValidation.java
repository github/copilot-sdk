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
 * Local archive checks; validates supported structure, not the truth or provenance of archived evidence.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskAnalyticsTrajectoryValidation(
    /** Whether the archive and execution options passed local checks. */
    @JsonProperty("valid") Boolean valid,
    /** Compact JSON input size in UTF-8 bytes. */
    @JsonProperty("inputBytes") Long inputBytes,
    /** Actionable validation errors. Does not include transcript content or raw parser errors. */
    @JsonProperty("errors") List<String> errors
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public TaskAnalyticsTrajectoryValidation {}

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
        private Boolean valid;
        private Long inputBytes;
        private List<String> errors;

        /**
         * Sets the {@code valid} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder valid(Boolean value) {
            this.valid = value;
            return this;
        }

        /**
         * Sets the {@code inputBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder inputBytes(Long value) {
            this.inputBytes = value;
            return this;
        }

        /**
         * Sets the {@code errors} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder errors(List<String> value) {
            this.errors = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public TaskAnalyticsTrajectoryValidation build() {
            if (valid == null) throw new IllegalStateException("Missing required property: valid");
            if (inputBytes == null) throw new IllegalStateException("Missing required property: inputBytes");
            if (errors == null) throw new IllegalStateException("Missing required property: errors");
            return new TaskAnalyticsTrajectoryValidation(valid, inputBytes, errors);
        }
    }
}
