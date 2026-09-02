/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Session event "sandbox.decision". Payload of `sandbox.decision`, a bounded governance record of what the process sandbox was configured to do and whether it took effect. Discriminated by `kind`.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SandboxDecisionEvent extends SessionEvent {

    @Override
    public String getType() { return "sandbox.decision"; }

    @JsonProperty("data")
    private SandboxDecisionEventData data;

    public SandboxDecisionEventData getData() { return data; }
    public void setData(SandboxDecisionEventData data) { this.data = data; }

    /** Data payload for {@link SandboxDecisionEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SandboxDecisionEventData() {
    }
}
