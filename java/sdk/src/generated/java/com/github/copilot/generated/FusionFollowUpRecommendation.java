/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Durable server recommendation for subsequent HydraFusion turns.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FusionFollowUpRecommendation(
    /** Recommended routing action for the next user-message turn. */
    @JsonProperty("userTurn") FusionFollowUpAction userTurn,
    /** Recommended routing action for the next compaction turn. */
    @JsonProperty("compactionTurn") FusionFollowUpAction compactionTurn
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public FusionFollowUpRecommendation {}

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
        private FusionFollowUpAction userTurn;
        private FusionFollowUpAction compactionTurn;

        /**
         * Sets the {@code userTurn} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder userTurn(FusionFollowUpAction value) {
            this.userTurn = value;
            return this;
        }

        /**
         * Sets the {@code compactionTurn} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder compactionTurn(FusionFollowUpAction value) {
            this.compactionTurn = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public FusionFollowUpRecommendation build() {
            if (userTurn == null) throw new IllegalStateException("Missing required property: userTurn");
            if (compactionTurn == null) throw new IllegalStateException("Missing required property: compactionTurn");
            return new FusionFollowUpRecommendation(userTurn, compactionTurn);
        }
    }
}
