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
 * API methods for the {@code llmInference} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerLlmInferenceApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerLlmInferenceApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Indicates whether the calling client was registered as the LLM inference provider.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<LlmInferenceSetProviderResult> setProvider() {
        return caller.invoke("llmInference.setProvider", java.util.Map.of(), LlmInferenceSetProviderResult.class);
    }

    /**
     * Response head.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<LlmInferenceHttpResponseStartResult> httpResponseStart(LlmInferenceHttpResponseStartParams params) {
        return caller.invoke("llmInference.httpResponseStart", params, LlmInferenceHttpResponseStartResult.class);
    }

    /**
     * A response body chunk or terminal error.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<LlmInferenceHttpResponseChunkResult> httpResponseChunk(LlmInferenceHttpResponseChunkParams params) {
        return caller.invoke("llmInference.httpResponseChunk", params, LlmInferenceHttpResponseChunkResult.class);
    }

}
