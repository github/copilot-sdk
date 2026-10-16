/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Provides session-scoped skills to the Copilot runtime.
 * <p>
 * A skill provider is registered through {@code SessionConfig} or
 * {@code ResumeSessionConfig}. The runtime may invoke provider callbacks
 * concurrently, so implementations should be thread-safe and avoid relying on
 * callback ordering.
 * <p>
 * When the runtime cancels a call, for example after its time limit or when the
 * session disconnects, the SDK cancels the returned future with
 * {@link CompletableFuture#cancel(boolean) cancel(true)} and the runtime
 * ignores any later result. Cancellation does not interrupt running work;
 * observe it with {@link CompletableFuture#isCancelled()} or a completion
 * callback to stop early.
 *
 * @apiNote This API is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
public interface SkillProvider {

    /**
     * Lists the skills currently available for the session.
     *
     * @return a future resolving to the provider's skill descriptors
     */
    CompletableFuture<List<SkillProviderDescriptor>> listSkills();

    /**
     * Reads the Markdown content for one skill.
     *
     * @param name
     *            the skill invocation name
     * @return a future resolving to the skill Markdown, or {@code null} when the
     *         skill is not found
     */
    CompletableFuture<String> readSkill(String name);
}
