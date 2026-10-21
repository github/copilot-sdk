/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Details of transcript repair reported by session.resume. Invalid line numbers
 * include discarded torn-tail data when applicable.
 *
 * @param plannedBackupPath
 *            path planned for the original transcript backup
 * @param invalidLineNumbers
 *            affected transcript line numbers
 * @param sessionStartMoved
 *            whether the session start event was relocated
 */
public record TranscriptRecoveryReport(@JsonProperty("plannedBackupPath") String plannedBackupPath,
        @JsonProperty("invalidLineNumbers") List<Integer> invalidLineNumbers,
        @JsonProperty("sessionStartMoved") boolean sessionStartMoved) {
}
