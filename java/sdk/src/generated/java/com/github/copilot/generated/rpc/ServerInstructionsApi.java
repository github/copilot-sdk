/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code instructions} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerInstructionsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerInstructionsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Returns the prompt the `init` command runs to author repository instructions. A host offering its own initialize action sends this text so the instructions it produces match the ones the CLI produces. The text is fixed for a build and depends on no configuration, session or repository.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<InstructionsInitPromptResult> getInitPrompt() {
        return caller.invoke("instructions.getInitPrompt", java.util.Map.of(), InstructionsInitPromptResult.class);
    }

    /**
     * Discovers instruction sources across user, repository, and plugin sources.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<InstructionsDiscoverResult> discover(InstructionsDiscoverParams params) {
        return caller.invoke("instructions.discover", params, InstructionsDiscoverResult.class);
    }

    /**
     * Discovers instruction sources across user, repository, and plugin sources.
     * <p>
     * Accepts the extensible request, including inputs added after the params record.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<InstructionsDiscoverResult> discover(InstructionsDiscoverRequest request) {
        return caller.invoke("instructions.discover", Objects.requireNonNull(request, "request"), InstructionsDiscoverResult.class);
    }

    /**
     * Discovers the instruction sources for one caller-named location and returns them in the order the runtime applies them. `discover` answers for a set of project paths and derives the rest of its scope itself; this one instead takes every argument from the caller — the extra directories to scan and whether to descend into nested instruction files — and honors them as given. It also includes instruction directories from trusted repository settings, matching native repository discovery. Use it when the caller knows its own scope and no session exists to derive that scope from, such as a host reporting how many instruction files a directory contributes before any session starts. Marked internal because it exists to carry a CLI call site off the napi boundary onto the SDK contract; it is migration plumbing, not a surface consumers are meant to depend on.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<ServerInstructionSourceList> discoverRepoSources(InstructionsDiscoverRepoSourcesParams params) {
        return caller.invoke("instructions.discoverRepoSources", params, ServerInstructionSourceList.class);
    }

    /**
     * Returns the canonical files and directories where a client may create custom instructions that the runtime will recognize, including ones that do not exist yet. Repository targets become active once created.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<InstructionsGetDiscoveryPathsResult> getDiscoveryPaths(InstructionsGetDiscoveryPathsParams params) {
        return caller.invoke("instructions.getDiscoveryPaths", params, InstructionsGetDiscoveryPathsResult.class);
    }

    /**
     * Returns the canonical files and directories where a client may create custom instructions that the runtime will recognize, including ones that do not exist yet. Repository targets become active once created.
     * <p>
     * Accepts the extensible request, including inputs added after the params record.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<InstructionsGetDiscoveryPathsResult> getDiscoveryPaths(InstructionsGetDiscoveryPathsRequest request) {
        return caller.invoke("instructions.getDiscoveryPaths", Objects.requireNonNull(request, "request"), InstructionsGetDiscoveryPathsResult.class);
    }

    /**
     * Drops the runtime's in-memory cache of discovered custom instructions so the next discovery reads them from disk again. A host calls this after it changes instruction files, or after it trusts a folder whose instructions an earlier discovery skipped, so that a session started afterwards sees the current files instead of a cached earlier answer. The call performs no I/O of its own and removes nothing from disk; it only invalidates, so the cost lands on the next discovery.         Returns once the cache is clear, which is what lets a caller order a session start after it. Marked internal because it exists to carry a CLI call site off the napi boundary onto the SDK contract; it is migration plumbing, not a surface consumers are meant to depend on.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> clearCache() {
        return caller.invoke("instructions.clearCache", java.util.Map.of(), Void.class);
    }

}
