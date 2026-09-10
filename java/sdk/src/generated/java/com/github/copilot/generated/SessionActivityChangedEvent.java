/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import javax.annotation.processing.Generated;

/**
 * Session event "session.activity_changed". Authoritative operational activity snapshot for the session. This describes agent execution, interactive waits, and process liveness; it does not measure token usage or spending.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionActivityChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.activity_changed"; }

    @JsonProperty("data")
    private SessionActivityChangedEventData data;

    public SessionActivityChangedEventData getData() { return data; }
    public void setData(SessionActivityChangedEventData data) { this.data = data; }

    /** Data payload for {@link SessionActivityChangedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionActivityChangedEventData(
        /** Legacy broad abortability flag retained for compatibility. New consumers should use mainAgent.abortable and scoped cancellation methods instead. */
        @JsonProperty("abortable") Boolean abortable,
        /** Compatibility aggregate that is true when the main agent is working or at least one background agent is running. Process liveness and idle-but-live agents do not make this true. */
        @JsonProperty("hasActiveWork") Boolean hasActiveWork,
        /** Activity contract version. Presence with value 1 is the capability signal for this contract; absence means unsupported, not idle. */
        @JsonProperty("contractVersion") Long contractVersion,
        /** Opaque identifier for the current live runtime incarnation of this session. A changed epoch is ordered only when received from the current connection. */
        @JsonProperty("activityEpoch") UUID activityEpoch,
        /** Monotonically increasing revision within activityEpoch. Equal revisions are idempotent; lower revisions are stale. */
        @JsonProperty("revision") Long revision,
        /** Main-agent execution or interactive-wait state. */
        @JsonProperty("mainAgent") SessionMainAgentActivity mainAgent,
        /** Background-agent activity counts. Idle multi-turn agents remain live but are not active work. */
        @JsonProperty("backgroundAgents") SessionBackgroundAgentActivity backgroundAgents,
        /** Live shell/process counts, reported separately from agent work. */
        @JsonProperty("processes") SessionProcessActivity processes
    ) {
    }
}
