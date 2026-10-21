/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.annotation.processing.Generated;

/**
 * Exact runtime-owned question or reviewed plan paired with the typed response that settled it.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "responseKind", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = HumanResponseRecordedResponseAskUser.class, name = "ask_user"),
    @JsonSubTypes.Type(value = HumanResponseRecordedResponseUserInput.class, name = "user_input"),
    @JsonSubTypes.Type(value = HumanResponseRecordedResponseExitPlanMode.class, name = "exit_plan_mode")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class HumanResponseRecordedResponse {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the responseKind discriminator
     */
    public abstract String getResponseKind();
}
