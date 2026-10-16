/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Session event "session.managed_plugin_progress". Experimental transient presentation-neutral progress for organization-required plugin preparation. Clients should localize the phase copy and display plugin specs without translating them.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionManagedPluginProgressEvent extends SessionEvent {

    @Override
    public String getType() { return "session.managed_plugin_progress"; }

    @JsonProperty("data")
    private SessionManagedPluginProgressEventData data;

    public SessionManagedPluginProgressEventData getData() { return data; }
    public void setData(SessionManagedPluginProgressEventData data) { this.data = data; }

    /** Data payload for {@link SessionManagedPluginProgressEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionManagedPluginProgressEventData(
        /** Current preparation phase. */
        @JsonProperty("phase") ManagedPluginProgressPhase phase,
        /** Ordered plugin install specs affected by this phase. Empty while managed settings initialize and when preparation completes. */
        @JsonProperty("pluginSpecs") List<String> pluginSpecs
    ) {
    }
}
