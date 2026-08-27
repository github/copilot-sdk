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
 * Variant {@code set-plan-model} of {@link SlashCommandInvocationResult}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SlashCommandInvocationResultSetPlanModel extends SlashCommandInvocationResult {

    @JsonProperty("kind")
    private final String kind = "set-plan-model";

    @Override
    public String getKind() { return kind; }

    /** Dedicated model selected for plan mode. */
    @JsonProperty("planModel")
    private String planModel;

    /** User-facing confirmation message for the plan-model selection. */
    @JsonProperty("message")
    private String message;

    /** Whether command execution changed persisted runtime settings. */
    @JsonProperty("runtimeSettingsChanged")
    private Boolean runtimeSettingsChanged;

    public String getPlanModel() { return planModel; }
    public void setPlanModel(String planModel) { this.planModel = planModel; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Boolean getRuntimeSettingsChanged() { return runtimeSettingsChanged; }
    public void setRuntimeSettingsChanged(Boolean runtimeSettingsChanged) { this.runtimeSettingsChanged = runtimeSettingsChanged; }
}
