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
 * Structured outcome of a rewind request.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionHistoryRewindResult(
    /** Overall rewind outcome. This discriminates the result: it governs which of the remaining fields are populated, so consumers must switch on it before reading `eventsRemoved`, `restoredFiles`, `skippedFiles`, or `error`. See each field for the outcomes that populate it. */
    @JsonProperty("outcome") HistoryRewindOutcome outcome,
    /** Number of persisted events removed by conversation truncation. Present only when truncation succeeded (outcomes `success`, `checkpoint-cleanup-failed`, and `snapshot-prune-failed`); omitted for every unavailable outcome (`session-busy`, `file-change-tracking-disabled`, `unsupported-remote-session`) and for `truncation-failed`, `files-rolled-back`, and `rollback-incomplete`. */
    @JsonProperty("eventsRemoved") Long eventsRemoved,
    /** Absolute paths restored to their captured preimages. Always empty for conversation-only rewinds and for the unavailable outcomes (`session-busy`, `file-change-tracking-disabled`, `unsupported-remote-session`); only conversation-and-files outcomes that reached the file-restore stage populate it. */
    @JsonProperty("restoredFiles") List<String> restoredFiles,
    /** Captured files intentionally left unchanged. Always empty for conversation-only rewinds and for the unavailable outcomes (`session-busy`, `file-change-tracking-disabled`, `unsupported-remote-session`); only conversation-and-files outcomes that reached the file-restore stage populate it. */
    @JsonProperty("skippedFiles") List<HistorySkippedFileRestore> skippedFiles,
    /** Failure detail. Set only for the failure and partial-failure outcomes (`files-rolled-back`, `rollback-incomplete`, `truncation-failed`, `checkpoint-cleanup-failed`, `snapshot-prune-failed`); omitted for `success` and for the unavailable outcomes (`session-busy`, `file-change-tracking-disabled`, `unsupported-remote-session`). */
    @JsonProperty("error") String error
) {
}
