/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.UUID;
import javax.annotation.processing.Generated;

/**
 * Authoritative operational activity snapshot for the session. This describes agent execution, interactive waits, and process liveness; it does not measure token usage or spending.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMetadataActivityResult(
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
