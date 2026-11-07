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
 * Variant {@code device-code} of {@link AuthLoginStep}.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AuthLoginStepDeviceCode extends AuthLoginStep {

    @JsonProperty("kind")
    private final String kind = "device-code";

    @Override
    public String getKind() { return kind; }

    /** One-time code the user enters at the verification URI. */
    @JsonProperty("userCode")
    private String userCode;

    /** Page where the user enters the one-time code (consumer-driven browser-open). */
    @JsonProperty("verificationUri")
    private String verificationUri;

    /** Seconds until the one-time code expires, measured from when this step was produced. Consumers can show a countdown or pick their own timeout instead of blocking until the provider rejects the code. */
    @JsonProperty("expiresIn")
    private Long expiresIn;

    /** Minimum seconds between authorization polls required by the provider. The runtime performs the polling itself for the duration of the single `advance` call, so this is informational (for example to drive a progress indicator); repeating `advance` is rejected as already advancing. */
    @JsonProperty("interval")
    private Long interval;

    public String getUserCode() { return userCode; }
    public void setUserCode(String userCode) { this.userCode = userCode; }

    public String getVerificationUri() { return verificationUri; }
    public void setVerificationUri(String verificationUri) { this.verificationUri = verificationUri; }

    public Long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }

    public Long getInterval() { return interval; }
    public void setInterval(Long interval) { this.interval = interval; }


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
        private String userCode;
        private String verificationUri;
        private Long expiresIn;
        private Long interval;

        /**
         * Sets the {@code userCode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder userCode(String value) {
            this.userCode = value;
            return this;
        }

        /**
         * Sets the {@code verificationUri} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder verificationUri(String value) {
            this.verificationUri = value;
            return this;
        }

        /**
         * Sets the {@code expiresIn} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder expiresIn(Long value) {
            this.expiresIn = value;
            return this;
        }

        /**
         * Sets the {@code interval} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder interval(Long value) {
            this.interval = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AuthLoginStepDeviceCode build() {
            if (userCode == null) throw new IllegalStateException("Missing required property: userCode");
            if (verificationUri == null) throw new IllegalStateException("Missing required property: verificationUri");
            if (expiresIn == null) throw new IllegalStateException("Missing required property: expiresIn");
            if (interval == null) throw new IllegalStateException("Missing required property: interval");
            AuthLoginStepDeviceCode instance = new AuthLoginStepDeviceCode();
            instance.setUserCode(userCode);
            instance.setVerificationUri(verificationUri);
            instance.setExpiresIn(expiresIn);
            instance.setInterval(interval);
            return instance;
        }
    }
}
