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
import javax.annotation.processing.Generated;

/**
 * Variant {@code completed} of {@link AuthLoginStep}.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AuthLoginStepCompleted extends AuthLoginStep {

    @JsonProperty("kind")
    private final String kind = "completed";

    @Override
    public String getKind() { return kind; }

    /** Login result. When status is needs-plaintext-consent or needs-account-selection, advance with the user's decision to continue. */
    @JsonProperty("result")
    private AuthLoginResultDto result;

    public AuthLoginResultDto getResult() { return result; }
    public void setResult(AuthLoginResultDto result) { this.result = result; }


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
        private AuthLoginResultDto result;

        /**
         * Sets the {@code result} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder result(AuthLoginResultDto value) {
            this.result = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AuthLoginStepCompleted build() {
            if (result == null) throw new IllegalStateException("Missing required property: result");
            AuthLoginStepCompleted instance = new AuthLoginStepCompleted();
            instance.setResult(result);
            return instance;
        }
    }
}
