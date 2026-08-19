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
 * Redacted model routing settings for a session.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionSettingsModelSnapshot(
    /** Selected model identifier. */
    @JsonProperty("model") String model,
    /** Default reasoning effort for the selected model. */
    @JsonProperty("defaultReasoningEffort") String defaultReasoningEffort,
    /** Agent job identifier for the session. */
    @JsonProperty("instanceId") String instanceId,
    /** Agent service callback URL for job and progress updates. */
    @JsonProperty("callbackUrl") String callbackUrl
) {
}
