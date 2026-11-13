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
 * The user's selected action for an exhausted session limit.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record UISessionLimitsExhaustedResponse(
    /** Action selected by the user. */
    @JsonProperty("action") UISessionLimitsExhaustedResponseAction action,
    /** AI Credits to add to the current max when action is 'add'. */
    @JsonProperty("additionalAiCredits") Double additionalAiCredits,
    /** New absolute max AI Credits when action is 'set'. */
    @JsonProperty("maxAiCredits") Double maxAiCredits
) {
}
