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
import javax.annotation.processing.Generated;

/**
 * A cloud task's recorded events, ordered for replay. Read `preStart` first: it selects which of the two event lists a caller should use, and they differ deliberately.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record CloudTasksGetSessionReplayResult(
    /** The events to replay in order to reconstruct the session, as a JSON array. Empty when `preStart` is true, because a session that has not started has nothing to reconstruct and replaying its pending events would invent a history it never had. */
    @JsonProperty("replayEventsJson") String replayEventsJson,
    /** Every recorded event in the same replay order, as a JSON array, whether or not the session has started. This is the list to hand to a subsequent ingress so it can tell which events it has already seen, and unlike `replayEventsJson` it is never emptied. */
    @JsonProperty("observedEventsJson") String observedEventsJson,
    /** True when the task has recorded events but the session has not started yet, so only queued work exists. A started session is anchored by either `session.start` or, for a resumed one, `session.resume`. */
    @JsonProperty("preStart") Boolean preStart
) {
}
