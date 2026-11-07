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
 * Validated HydraFusion routing capability scores.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FusionScores(
    /** Reasoning capability score returned by the authenticated router. */
    @JsonProperty("reasoning") Double reasoning,
    /** Code-generation capability score returned by the authenticated router. */
    @JsonProperty("codeGen") Double codeGen,
    /** Debugging capability score returned by the authenticated router. */
    @JsonProperty("debugging") Double debugging,
    /** Tool-use capability score returned by the authenticated router. */
    @JsonProperty("toolUse") Double toolUse
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public FusionScores {}

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
        private Double reasoning;
        private Double codeGen;
        private Double debugging;
        private Double toolUse;

        /**
         * Sets the {@code reasoning} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder reasoning(Double value) {
            this.reasoning = value;
            return this;
        }

        /**
         * Sets the {@code codeGen} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder codeGen(Double value) {
            this.codeGen = value;
            return this;
        }

        /**
         * Sets the {@code debugging} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder debugging(Double value) {
            this.debugging = value;
            return this;
        }

        /**
         * Sets the {@code toolUse} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder toolUse(Double value) {
            this.toolUse = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public FusionScores build() {
            if (reasoning == null) throw new IllegalStateException("Missing required property: reasoning");
            if (codeGen == null) throw new IllegalStateException("Missing required property: codeGen");
            if (debugging == null) throw new IllegalStateException("Missing required property: debugging");
            if (toolUse == null) throw new IllegalStateException("Missing required property: toolUse");
            return new FusionScores(reasoning, codeGen, debugging, toolUse);
        }
    }
}
