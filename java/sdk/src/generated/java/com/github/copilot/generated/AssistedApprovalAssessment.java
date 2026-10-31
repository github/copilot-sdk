/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Canonical runtime-owned Assisted Permissions assessment for one permission request. This assessment is advisory; `permission.completed` remains the authoritative final permission decision.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "status", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = AssistedApprovalAssessmentUnavailable.class, name = "unavailable"),
    @JsonSubTypes.Type(value = AssistedApprovalAssessmentNotRun.class, name = "not-run"),
    @JsonSubTypes.Type(value = AssistedApprovalAssessmentExcluded.class, name = "excluded"),
    @JsonSubTypes.Type(value = AssistedApprovalAssessmentCompleted.class, name = "completed"),
    @JsonSubTypes.Type(value = AssistedApprovalAssessmentFailed.class, name = "failed")
})
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class AssistedApprovalAssessment {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the status discriminator
     */
    public abstract String getStatus();
}
