/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import com.github.copilot.rpc.ResumeSessionConfig;
import java.util.concurrent.CompletionStage;

/**
 * An application-owned durable AHP session resume request. Reattach application
 * callbacks using the supplied settings, or return the exact retained session.
 *
 * @param sessionId
 *            durable session identity to resume
 * @param config
 *            host-selected resume settings
 * @param cancellation
 *            completes normally when the participation ends
 */
@CopilotExperimental
public record AhpSessionResumeRequest(String sessionId, ResumeSessionConfig config,
        CompletionStage<Void> cancellation) {
}
