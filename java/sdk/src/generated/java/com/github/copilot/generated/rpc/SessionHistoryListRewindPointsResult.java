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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Rewind points and file-change-tracking availability for the session.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionHistoryListRewindPointsResult(
    /** Whether this session captured file changes from its first turn. */
    @JsonProperty("fileChangeTrackingEnabled") Boolean fileChangeTrackingEnabled,
    /** Why the listed points could not be produced, when applicable; the points list is empty whenever it is set. `unsupported-remote-session` is permanent for the session and comes with `fileChangeTrackingEnabled: false`. `session-busy` is transient and only ever reported by a session that *is* tracking (`fileChangeTrackingEnabled: true`), because the file-change captures cannot be read while work that may still mutate them is in flight; the same request succeeds once the session settles, so a client that wants points should retry rather than treat it as a failure. It is never `file-change-tracking-disabled`: an untracked local session still lists conversation-only points and reports that through `fileChangeTrackingEnabled: false`. */
    @JsonProperty("unavailableReason") HistoryRewindUnavailableReason unavailableReason,
    /** Root user turns in chronological order. Empty when `unavailableReason` is set. */
    @JsonProperty("points") List<HistoryRewindPoint> points
) {
}
