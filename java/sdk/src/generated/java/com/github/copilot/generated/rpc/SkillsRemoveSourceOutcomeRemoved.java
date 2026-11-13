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
 * The existing source removal rules resolved and removed the target.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SkillsRemoveSourceOutcomeRemoved extends SkillsRemoveSourceOutcome {

    @JsonProperty("kind")
    private final String kind = "removed";

    @Override
    public String getKind() { return kind; }

    /** Removal result and updated discovery directories. */
    @JsonProperty("result")
    private SkillsRemoveSourceResult result;

    public SkillsRemoveSourceResult getResult() { return result; }
    public void setResult(SkillsRemoveSourceResult result) { this.result = result; }


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
        private SkillsRemoveSourceResult result;

        /**
         * Sets the {@code result} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder result(SkillsRemoveSourceResult value) {
            this.result = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SkillsRemoveSourceOutcomeRemoved build() {
            if (result == null) throw new IllegalStateException("Missing required property: result");
            SkillsRemoveSourceOutcomeRemoved instance = new SkillsRemoveSourceOutcomeRemoved();
            instance.setResult(result);
            return instance;
        }
    }
}
