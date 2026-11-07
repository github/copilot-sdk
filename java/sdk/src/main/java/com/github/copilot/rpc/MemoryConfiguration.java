/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.AllowCopilotExperimental;
import com.github.copilot.CopilotExperimental;
import com.github.copilot.generated.rpc.MemoryMode;

/**
 * Configuration for session memory.
 * <p>
 * Controls whether the session can read and write persistent memory.
 *
 * @since 1.6.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllowCopilotExperimental
public class MemoryConfiguration {

    @JsonProperty("enabled")
    private boolean enabled;

    @JsonProperty("mode")
    private MemoryMode mode;

    @JsonIgnore
    private Function<StoredMemory, CompletableFuture<Void>> storeMemory;

    /**
     * Gets whether memory is enabled for the session.
     *
     * @return {@code true} if memory is enabled, {@code false} otherwise
     */
    public boolean getEnabled() {
        return enabled;
    }

    /**
     * Sets whether memory is enabled for the session.
     *
     * @param enabled
     *            {@code true} to enable memory, {@code false} to disable
     * @return this config for method chaining
     */
    public MemoryConfiguration setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Gets where memories are placed.
     *
     * @return the memory mode, or {@code null} for the default
     *         {@link MemoryMode#SYSTEM_PROMPT}
     * @apiNote This option is experimental and may change in a future version.
     */
    @CopilotExperimental
    public MemoryMode getMode() {
        return mode;
    }

    /**
     * Sets where memories are placed. {@link MemoryMode#SYSTEM_PROMPT} (default)
     * places memories in the system prompt. {@link MemoryMode#USER_PROMPT} places
     * memories in generated user-role messages.
     *
     * @param mode
     *            the memory mode, or {@code null} to use the default
     * @return this config for method chaining
     * @apiNote This option is experimental and may change in a future version.
     */
    @CopilotExperimental
    public MemoryConfiguration setMode(MemoryMode mode) {
        this.mode = mode;
        return this;
    }

    /**
     * Gets the callback that stores memories in the app.
     *
     * @return the store-memory callback, or {@code null} if not set
     * @apiNote This option is experimental and may change in a future version.
     */
    @CopilotExperimental
    @JsonIgnore
    public Function<StoredMemory, CompletableFuture<Void>> getStoreMemory() {
        return storeMemory;
    }

    /**
     * Stores memories in the app instead of the built-in memory backends.
     * <p>
     * The runtime validates the memory, filters secrets and requests approval, then
     * calls this callback. {@code store_memory} succeeds only after the returned
     * future completes; an exceptional completion fails the tool call with a
     * generic message and nothing is stored elsewhere. While set, the runtime
     * retrieves no memories and {@code vote_memory} is unavailable, so supply
     * recall yourself, for example through the system message. Requires memory to
     * be {@linkplain #setEnabled(boolean) enabled}; creating or resuming a session
     * with this callback set and memory disabled fails. Not supported for cloud
     * sessions.
     * <p>
     * When the runtime cancels a call, for example after its 60-second limit or
     * when the session disconnects, the SDK cancels the returned future with
     * {@link CompletableFuture#cancel(boolean) cancel(true)}. Cancellation does not
     * interrupt running work; observe it with
     * {@link CompletableFuture#isCancelled()} or a completion callback to stop
     * early.
     *
     * @param storeMemory
     *            the callback, or {@code null} to use the built-in backends
     * @return this config for method chaining
     * @apiNote This option is experimental and may change in a future version.
     */
    @CopilotExperimental
    @JsonIgnore
    public MemoryConfiguration setStoreMemory(Function<StoredMemory, CompletableFuture<Void>> storeMemory) {
        this.storeMemory = storeMemory;
        return this;
    }
}
