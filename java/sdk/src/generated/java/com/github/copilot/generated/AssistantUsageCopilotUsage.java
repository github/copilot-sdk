/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Per-request cost and usage data from the CAPI copilot_usage response field
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AssistantUsageCopilotUsage(
    /** Default billing model for token details that do not identify their own model */
    @JsonProperty("model") String model,
    /** Itemized token usage breakdown */
    @JsonProperty("tokenDetails") List<AssistantUsageCopilotUsageTokenDetail> tokenDetails,
    /** Total cost in nano-AI units for this request */
    @JsonProperty("totalNanoAiu") Double totalNanoAiu
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AssistantUsageCopilotUsage {}

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
        private String model;
        private List<AssistantUsageCopilotUsageTokenDetail> tokenDetails;
        private Double totalNanoAiu;

        /**
         * Sets the {@code model} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder model(String value) {
            this.model = value;
            return this;
        }

        /**
         * Sets the {@code tokenDetails} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder tokenDetails(List<AssistantUsageCopilotUsageTokenDetail> value) {
            this.tokenDetails = value;
            return this;
        }

        /**
         * Sets the {@code totalNanoAiu} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder totalNanoAiu(Double value) {
            this.totalNanoAiu = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AssistantUsageCopilotUsage build() {
            if (totalNanoAiu == null) throw new IllegalStateException("Missing required property: totalNanoAiu");
            return new AssistantUsageCopilotUsage(model, tokenDetails, totalNanoAiu);
        }
    }
}
