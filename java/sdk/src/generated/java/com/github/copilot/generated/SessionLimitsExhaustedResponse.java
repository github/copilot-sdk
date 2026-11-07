/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * The user's selected action for an exhausted session limit.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionLimitsExhaustedResponse(
    /** Action selected by the user. */
    @JsonProperty("action") SessionLimitsExhaustedResponseAction action,
    /** AI Credits to add to the current max when action is 'add'. */
    @JsonProperty("additionalAiCredits") Double additionalAiCredits,
    /** New absolute max AI Credits when action is 'set'. */
    @JsonProperty("maxAiCredits") Double maxAiCredits
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionLimitsExhaustedResponse {}

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
        private SessionLimitsExhaustedResponseAction action;
        private Double additionalAiCredits;
        private Double maxAiCredits;

        /**
         * Sets the {@code action} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder action(SessionLimitsExhaustedResponseAction value) {
            this.action = value;
            return this;
        }

        /**
         * Sets the {@code additionalAiCredits} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder additionalAiCredits(Double value) {
            this.additionalAiCredits = value;
            return this;
        }

        /**
         * Sets the {@code maxAiCredits} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder maxAiCredits(Double value) {
            this.maxAiCredits = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionLimitsExhaustedResponse build() {
            if (action == null) throw new IllegalStateException("Missing required property: action");
            return new SessionLimitsExhaustedResponse(action, additionalAiCredits, maxAiCredits);
        }
    }
}
