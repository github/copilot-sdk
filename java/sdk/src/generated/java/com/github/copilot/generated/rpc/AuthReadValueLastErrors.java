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
 * Variant {@code lastErrors} of {@link AuthReadValue}.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AuthReadValueLastErrors extends AuthReadValue {

    @JsonProperty("kind")
    private final String kind = "lastErrors";

    @Override
    public String getKind() { return kind; }

    /** Validation errors from the most recent authentication attempt. */
    @JsonProperty("errors")
    private List<AuthValidationError> errors;

    public List<AuthValidationError> getErrors() { return errors; }
    public void setErrors(List<AuthValidationError> errors) { this.errors = errors; }


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
        private List<AuthValidationError> errors;

        /**
         * Sets the {@code errors} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder errors(List<AuthValidationError> value) {
            this.errors = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AuthReadValueLastErrors build() {
            if (errors == null) throw new IllegalStateException("Missing required property: errors");
            AuthReadValueLastErrors instance = new AuthReadValueLastErrors();
            instance.setErrors(errors);
            return instance;
        }
    }
}
