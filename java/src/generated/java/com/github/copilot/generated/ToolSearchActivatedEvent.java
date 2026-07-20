/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Session event "tool_search.activated". Persisted generic client-side tool activations restored when a session resumes.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ToolSearchActivatedEvent extends SessionEvent {

    @Override
    public String getType() { return "tool_search.activated"; }

    @JsonProperty("data")
    private ToolSearchActivatedEventData data;

    public ToolSearchActivatedEventData getData() { return data; }
    public void setData(ToolSearchActivatedEventData data) { this.data = data; }

    /** Data payload for {@link ToolSearchActivatedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ToolSearchActivatedEventData(
        /** Tool-search strategy that activated the definitions. */
        @JsonProperty("strategy") String strategy,
        /** Names of tool definitions activated by this search invocation. */
        @JsonProperty("toolNames") List<String> toolNames
    ) {
    }
}
