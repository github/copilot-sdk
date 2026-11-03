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
 * API methods for the {@code cafeMetrics} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class ServerCafeMetricsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerCafeMetricsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Starts anonymous CAFE metrics for this process. The runtime selects the endpoint from COPILOT_CAFE_METRICS_URL or this connection's original auth owner. Offline and unsupported platforms refuse startup. Release eligibility must resolve before samples are sent. Requires the private trusted CLI host capability.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<CafeMetricsStartResult> start(CafeMetricsStartParams params) {
        return caller.invoke("cafeMetrics.start", params, CafeMetricsStartResult.class);
    }

    /**
     * Resolves release eligibility once from the immutable release lookup shared with auto-update. Requires the private trusted CLI host capability.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<CafeMetricsResolveAdmissionResult> resolveAdmission(CafeMetricsResolveAdmissionParams params) {
        return caller.invoke("cafeMetrics.resolveAdmission", params, CafeMetricsResolveAdmissionResult.class);
    }

    /**
     * Records one terminal resume result. Does nothing before startup. Requires the private trusted CLI host capability.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> recordResumeResult(CafeMetricsRecordResumeResultParams params) {
        return caller.invoke("cafeMetrics.recordResumeResult", params, Void.class);
    }

    /**
     * Stops recording and drains within one 1.5-second deadline. flushOnly sends at most one batch without stopping recording. Requires the private trusted CLI host capability.
     * <p>
     * Invokes the method with no params, applying the runtime defaults.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> shutdown() {
        return shutdown(null);
    }

    /**
     * Stops recording and drains within one 1.5-second deadline. flushOnly sends at most one batch without stopping recording. Requires the private trusted CLI host capability.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> shutdown(CafeMetricsShutdownParams params) {
        return caller.invoke("cafeMetrics.shutdown", params == null ? java.util.Map.of() : params, Void.class);
    }

}
