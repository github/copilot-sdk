/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code taskAnalytics} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerTaskAnalyticsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerTaskAnalyticsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Classifies a recorded Copilot or ATIF trajectory using the runtime's production analytics pipeline. No conversation session is required or created, archived tools are never executed, and classification telemetry is not emitted. Calling this method explicitly requests model processing of retained trajectory content through the connection's GitHub CAPI credentials, independently of background telemetry consent; BYOK endpoints are not used. Callers must authorize that processing and validate archive provenance. Content is not automatically redacted or sanitized for prompt injection. Model-derived results can be manipulated by trajectory content and must not be used as authorization or security decisions.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<TaskAnalyticsClassifyTrajectoryResult> classifyTrajectory(TaskAnalyticsClassifyTrajectoryParams params) {
        return caller.invoke("taskAnalytics.classifyTrajectory", params, TaskAnalyticsClassifyTrajectoryResult.class);
    }

}
