/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

/**
 * Selects the defaulting strategy used by
 * {@link com.github.copilot.CopilotClient}.
 *
 * @since 1.3.0
 */
public enum CopilotClientMode {

    /**
     * Disables optional features by default. The app must explicitly opt into
     * anything it needs. Required for any scenario where CLI-like ambient behavior
     * is unsafe (e.g., multi-user servers).
     * <p>
     * When this mode is selected:
     * <ul>
     * <li>The client constructor requires
     * {@link CopilotClientOptions#getCopilotHome()} or a session filesystem to be
     * set.</li>
     * <li>{@link SessionConfig#getAvailableTools()} must be supplied on every
     * session — no tools are exposed by default.</li>
     * <li>{@code session.create} always sets
     * {@code toolFilterPrecedence: "excluded"} so the allowlist and denylist
     * compose naturally.</li>
     * <li>The SDK injects safe defaults for ambient session features (telemetry,
     * custom instructions, plugins, environment context, etc.).</li>
     * </ul>
     */
    EMPTY,

    /**
     * Uses defaults equivalent to GitHub Copilot CLI. The default. Useful when
     * building a coding agent that shares sessions with Copilot CLI.
     * <p>
     * <b>Do not use this mode for server-based multi-user applications</b> — the
     * default coding agent has tools and capabilities that operate across sessions
     * and can access the host OS environment.
     */
    COPILOT_CLI
}
