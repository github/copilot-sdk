/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: lifecycle-diagnostics.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;

/** Experimental runtime lifecycle diagnostic event. */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class RuntimeLifecycleSystemStateDiagnosticEvent extends LifecycleDiagnosticEvent {
    @Override
    public String getType() { return "runtime.lifecycle.system_state"; }

    @JsonProperty("data")
    private RuntimeLifecycleSystemStateData data;

    public RuntimeLifecycleSystemStateData getData() { return data; }
    public void setData(RuntimeLifecycleSystemStateData data) { this.data = data; }
}
