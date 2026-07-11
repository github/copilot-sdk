/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.Collections;
import java.util.List;

/**
 * Curated sets of built-in tool names for common scenarios. Each constant is
 * meant to be passed to {@link ToolSet#addBuiltIn(java.util.Collection)}.
 *
 * @since 1.3.0
 */
public final class BuiltInTools {

    /**
     * Built-in tools that operate only within the bounds of a single session — no
     * host filesystem access outside the session, no cross-session state, no host
     * environment access, no network. Safe to enable in
     * {@link CopilotClientMode#EMPTY} scenarios (e.g. multi-tenant servers) without
     * leaking host capabilities.
     * <p>
     * <b>Contract:</b> tools in this set MUST NOT be extended (even behind options
     * or args) to read or write state outside the session boundary. Adding
     * cross-session or host-state behavior to one of these tools is a breaking
     * change that requires removing it from this set.
     */
    public static final List<String> ISOLATED = Collections
            .unmodifiableList(List.of("ask_user", "task_complete", "exit_plan_mode", "task", "read_agent",
                    "write_agent", "list_agents", "send_inbox", "context_board", "skill"));

    private BuiltInTools() {
        // utility class
    }
}
