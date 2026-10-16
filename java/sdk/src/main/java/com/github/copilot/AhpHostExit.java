/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import com.github.copilot.generated.rpc.HostExitReason;

/**
 * A connection-owned listener's termination and cleanup outcome.
 *
 * @param hostId
 *            listener identity
 * @param reason
 *            termination reason
 * @param exitCode
 *            legacy child process status, absent for in-process hosting
 * @param error
 *            startup or cleanup failure, or {@code null} on acknowledged
 *            success
 */
@CopilotExperimental
public record AhpHostExit(String hostId, HostExitReason reason, Long exitCode, String error) {
}
