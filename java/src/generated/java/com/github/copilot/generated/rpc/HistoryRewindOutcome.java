/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Outcome of a rewind request.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum HistoryRewindOutcome {
    /** The {@code success} variant. */
    SUCCESS("success"),
    /** The {@code session-busy} variant. */
    SESSION_BUSY("session-busy"),
    /** The {@code file-change-tracking-disabled} variant. */
    FILE_CHANGE_TRACKING_DISABLED("file-change-tracking-disabled"),
    /** The {@code unsupported-remote-session} variant. */
    UNSUPPORTED_REMOTE_SESSION("unsupported-remote-session"),
    /** The {@code files-rolled-back} variant. */
    FILES_ROLLED_BACK("files-rolled-back"),
    /** The {@code rollback-incomplete} variant. */
    ROLLBACK_INCOMPLETE("rollback-incomplete"),
    /** The {@code truncation-failed} variant. */
    TRUNCATION_FAILED("truncation-failed"),
    /** The {@code checkpoint-cleanup-failed} variant. */
    CHECKPOINT_CLEANUP_FAILED("checkpoint-cleanup-failed"),
    /** The {@code snapshot-prune-failed} variant. */
    SNAPSHOT_PRUNE_FAILED("snapshot-prune-failed");

    private final String value;
    HistoryRewindOutcome(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static HistoryRewindOutcome fromValue(String value) {
        for (HistoryRewindOutcome v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown HistoryRewindOutcome value: " + value);
    }
}
