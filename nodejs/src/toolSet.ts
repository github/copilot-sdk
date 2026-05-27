/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Builder for the {@link SessionConfigBase.availableTools} list using
 * source-qualified filter patterns (`builtin:*`, `mcp:<name>`, `custom:*`, etc.).
 *
 * See plan: client-level Mode = "empty" with explicit tool selection.
 */

/**
 * Tool name character set enforced by the runtime at every registration
 * boundary. Mirrors the runtime's `VALID_TOOL_NAME_REGEX`. Used to validate
 * names passed to the `ToolSet` builder so misuse is caught at the SDK
 * boundary with a better error than the runtime would produce.
 */
const VALID_TOOL_NAME = /^[a-zA-Z0-9_-]+$/;

function validateName(kind: "builtin" | "mcp" | "custom", name: string): void {
    if (name === "*") {
        return;
    }
    if (!VALID_TOOL_NAME.test(name)) {
        throw new Error(
            `Invalid ${kind} tool name '${name}': tool names must match /^[a-zA-Z0-9_-]+$/ ` +
                `or be the wildcard '*'.`
        );
    }
}

/**
 * Builder that produces a list of source-qualified tool filter strings for
 * {@link SessionConfigBase.availableTools}.
 *
 * Tools are classified by the runtime at registration time (not from name
 * parsing), so `addBuiltIn("foo")` matches only tools the runtime registered
 * as built-in, even if an MCP server or custom-agent extension happens to
 * register a tool with the same wire name.
 *
 * @example
 * ```typescript
 * const tools = new ToolSet()
 *     .addBuiltIn(BuiltInTools.Isolated)
 *     .addMcp("*")
 *     .addCustom("*");
 *
 * const session = await client.createSession({
 *     availableTools: tools,
 *     // ...
 * });
 * ```
 */
export class ToolSet {
    private readonly items: string[] = [];

    /**
     * Adds one or more built-in tool patterns.
     *
     * @param name A specific built-in tool name (e.g. `"bash"`) or `"*"` to match all
     *   built-in tools.
     */
    addBuiltIn(name: string): ToolSet;
    /**
     * Adds a list of built-in tool patterns (e.g. {@link BuiltInTools.Isolated}).
     */
    addBuiltIn(names: readonly string[]): ToolSet;
    addBuiltIn(nameOrNames: string | readonly string[]): ToolSet {
        const names = typeof nameOrNames === "string" ? [nameOrNames] : nameOrNames;
        for (const name of names) {
            validateName("builtin", name);
            this.items.push(`builtin:${name}`);
        }
        return this;
    }

    /**
     * Adds a custom tool pattern. Matches tools registered via the SDK's
     * `tools` option or via custom agents.
     *
     * @param name A specific custom tool name or `"*"` to match all custom tools.
     */
    addCustom(name: string): ToolSet {
        validateName("custom", name);
        this.items.push(`custom:${name}`);
        return this;
    }

    /**
     * Adds an MCP tool pattern. Matches tools advertised by any configured
     * MCP server.
     *
     * @param toolName The runtime's canonical wire name for the MCP tool
     *   (e.g. `"github-list_issues"`), or `"*"` to match all MCP tools from
     *   any server.
     */
    addMcp(toolName: string): ToolSet {
        validateName("mcp", toolName);
        this.items.push(`mcp:${toolName}`);
        return this;
    }

    /**
     * Returns a defensive copy of the accumulated filter strings, suitable for
     * passing as {@link SessionConfigBase.availableTools}.
     */
    toArray(): string[] {
        return [...this.items];
    }
}

/**
 * Curated sets of built-in tool names for common scenarios. Each constant is
 * meant to be passed to {@link ToolSet.addBuiltIn}.
 */
export const BuiltInTools = {
    /**
     * Built-in tools that operate only within the bounds of a single session —
     * no host filesystem access outside the session, no cross-session state,
     * no host environment access, no network. Safe to enable in `Mode = "empty"`
     * scenarios (e.g. multi-tenant servers) without leaking host capabilities.
     *
     * **Contract:** tools in this set MUST NOT be extended (even behind options
     * or args) to read or write state outside the session boundary. Adding
     * cross-session or host-state behavior to one of these tools is a
     * breaking change that requires removing it from this set.
     */
    Isolated: [
        "ask_user",
        "task_complete",
        "exit_plan_mode",
        "task",
        "read_agent",
        "write_agent",
        "list_agents",
        "send_inbox",
        "context_board",
        "skill",
    ] as readonly string[],
} as const;
