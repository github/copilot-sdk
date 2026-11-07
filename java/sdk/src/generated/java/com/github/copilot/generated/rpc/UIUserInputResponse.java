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
 * User response for a pending user-input request, with answer text and whether it was typed freeform.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record UIUserInputResponse(
    /** The user's answer text */
    @JsonProperty("answer") String answer,
    /** True if the user typed a freeform response, false if they selected a presented choice. Used by telemetry to differentiate between free text input and choice selection. */
    @JsonProperty("wasFreeform") Boolean wasFreeform
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public UIUserInputResponse {}

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
        private String answer;
        private Boolean wasFreeform;

        /**
         * Sets the {@code answer} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder answer(String value) {
            this.answer = value;
            return this;
        }

        /**
         * Sets the {@code wasFreeform} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder wasFreeform(Boolean value) {
            this.wasFreeform = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public UIUserInputResponse build() {
            if (answer == null) throw new IllegalStateException("Missing required property: answer");
            if (wasFreeform == null) throw new IllegalStateException("Missing required property: wasFreeform");
            return new UIUserInputResponse(answer, wasFreeform);
        }
    }
}
