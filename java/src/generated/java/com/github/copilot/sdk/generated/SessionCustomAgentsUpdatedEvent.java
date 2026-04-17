/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * The {@code session.custom_agents_updated} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionCustomAgentsUpdatedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.custom_agents_updated"; }

    @JsonProperty("data")
    private SessionCustomAgentsUpdatedEventData data;

    public SessionCustomAgentsUpdatedEventData getData() { return data; }
    public void setData(SessionCustomAgentsUpdatedEventData data) { this.data = data; }

    /** Data payload for {@link SessionCustomAgentsUpdatedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionCustomAgentsUpdatedEventData(
        /** Array of loaded custom agent metadata */
        @JsonProperty("agents") List<SessionCustomAgentsUpdatedEventDataAgentsItem> agents,
        /** Non-fatal warnings from agent loading */
        @JsonProperty("warnings") List<String> warnings,
        /** Fatal errors from agent loading */
        @JsonProperty("errors") List<String> errors
    ) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record SessionCustomAgentsUpdatedEventDataAgentsItem(
            /** Unique identifier for the agent */
            @JsonProperty("id") String id,
            /** Internal name of the agent */
            @JsonProperty("name") String name,
            /** Human-readable display name */
            @JsonProperty("displayName") String displayName,
            /** Description of what the agent does */
            @JsonProperty("description") String description,
            /** Source location: user, project, inherited, remote, or plugin */
            @JsonProperty("source") String source,
            /** List of tool names available to this agent */
            @JsonProperty("tools") List<String> tools,
            /** Whether the agent can be selected by the user */
            @JsonProperty("userInvocable") Boolean userInvocable,
            /** Model override for this agent, if set */
            @JsonProperty("model") String model
        ) {
        }
    }
}
