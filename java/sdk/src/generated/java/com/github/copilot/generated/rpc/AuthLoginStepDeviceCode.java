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
}
