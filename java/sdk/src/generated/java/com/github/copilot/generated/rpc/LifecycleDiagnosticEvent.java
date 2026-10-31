/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: lifecycle-diagnostics.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.copilot.CopilotExperimental;
import java.time.OffsetDateTime;

/** Experimental runtime lifecycle diagnostic event. */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = RuntimeLifecycleSystemStateDiagnosticEvent.class, name = "runtime.lifecycle.system_state"),
    @JsonSubTypes.Type(value = RuntimeLifecycleProgressStateDiagnosticEvent.class, name = "runtime.lifecycle.progress_state")
})
@javax.annotation.processing.Generated("copilot-sdk-codegen")
abstract class LifecycleDiagnosticEvent {
    @JsonIgnore
    public abstract String getType();

    @JsonProperty("timestamp")
    private OffsetDateTime timestamp;

    public OffsetDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }
}
