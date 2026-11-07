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
 * Presentation-neutral phase planned for a HydraFusion turn.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FusionPhasePlanStep(
    /** Kind of phase that may execute. */
    @JsonProperty("kind") FusionPhaseKind kind,
    /** Semantic role assigned to the phase. */
    @JsonProperty("role") String role,
    /** Conversation scope in which the phase executes. */
    @JsonProperty("scope") FusionConversationScope scope,
    /** Whether the phase executes only when an earlier phase requests it. */
    @JsonProperty("conditional") Boolean conditional
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public FusionPhasePlanStep {}

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
        private FusionPhaseKind kind;
        private String role;
        private FusionConversationScope scope;
        private Boolean conditional;

        /**
         * Sets the {@code kind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder kind(FusionPhaseKind value) {
            this.kind = value;
            return this;
        }

        /**
         * Sets the {@code role} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder role(String value) {
            this.role = value;
            return this;
        }

        /**
         * Sets the {@code scope} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder scope(FusionConversationScope value) {
            this.scope = value;
            return this;
        }

        /**
         * Sets the {@code conditional} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder conditional(Boolean value) {
            this.conditional = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public FusionPhasePlanStep build() {
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            if (role == null) throw new IllegalStateException("Missing required property: role");
            if (scope == null) throw new IllegalStateException("Missing required property: scope");
            if (conditional == null) throw new IllegalStateException("Missing required property: conditional");
            return new FusionPhasePlanStep(kind, role, scope, conditional);
        }
    }
}
