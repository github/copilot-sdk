/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import com.github.copilot.rpc.SessionConfig;
import java.util.concurrent.CompletionStage;

/**
 * An application-owned AHP session creation request. Preserve the supplied
 * settings when adding application tools, prompts, permissions, and hooks.
 *
 * @param config
 *            host-selected session settings
 * @param cancellation
 *            completes normally when the participation ends; a late factory
 *            result is still delivered to the release callback
 */
@CopilotExperimental
public record AhpSessionCreateRequest(SessionConfig config, CompletionStage<Void> cancellation) {
}
