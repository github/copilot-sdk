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
 * Session event "subagent.configured". Resolved runtime configuration for a configured sub-agent
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SubagentConfiguredEvent extends SessionEvent {

    @Override
    public String getType() { return "subagent.configured"; }

    @JsonProperty("data")
    private SubagentConfiguredEventData data;

    public SubagentConfiguredEventData getData() { return data; }
    public void setData(SubagentConfiguredEventData data) { this.data = data; }

    /** Data payload for {@link SubagentConfiguredEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SubagentConfiguredEventData(
        /** Resolved model the sub-agent will run with */
        @JsonProperty("model") String model,
        /** Resolved reasoning effort, when configured for the model */
        @JsonProperty("reasoningEffort") String reasoningEffort,
        /** Resolved context tier, when configured for the model */
        @JsonProperty("contextTier") String contextTier,
        /** Whether the sub-agent accepts follow-up turns */
        @JsonProperty("multiTurn") Boolean multiTurn
    ) {
    }
}
