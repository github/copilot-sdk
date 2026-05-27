/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Builder for {@link SessionConfig#setAvailableTools(java.util.List)} /
 * {@link SessionConfig#setExcludedTools(java.util.List)} using source-qualified
 * filter patterns ({@code builtin:*}, {@code mcp:<name>}, {@code custom:*},
 * etc.).
 * <p>
 * Tools are classified by the runtime at registration time (not from name
 * parsing), so {@link #addBuiltIn(String)} matches only tools the runtime
 * registered as built-in, even if an MCP server or custom-agent extension
 * happens to register a tool with the same wire name.
 * <p>
 * {@code ToolSet} extends {@link ArrayList} so instances can be passed directly
 * to {@link SessionConfig#setAvailableTools(java.util.List)} or
 * {@link SessionConfig#setExcludedTools(java.util.List)}.
 *
 * <h2>Example</h2>
 *
 * <pre>{@code
 * var session = client
 * 		.createSession(new SessionConfig()
 * 				.setAvailableTools(new ToolSet().addBuiltIn(BuiltInTools.ISOLATED).addMcp("*").addCustom("*")))
 * 		.get();
 * }</pre>
 *
 * @since 1.3.0
 */
public class ToolSet extends ArrayList<String> {

    private static final Pattern VALID_TOOL_NAME = Pattern.compile("^[a-zA-Z0-9_-]+$");

    /**
     * Adds a built-in tool pattern.
     *
     * @param name
     *            a specific built-in tool name (e.g. {@code "bash"}) or {@code "*"}
     *            to match all built-in tools
     * @return this {@code ToolSet} for chaining
     * @throws IllegalArgumentException
     *             if name is null, empty, or contains invalid characters
     */
    public ToolSet addBuiltIn(String name) {
        validateName("builtin", name);
        add("builtin:" + name);
        return this;
    }

    /**
     * Adds a list of built-in tool patterns (e.g. {@link BuiltInTools#ISOLATED}).
     *
     * @param names
     *            built-in tool names to add
     * @return this {@code ToolSet} for chaining
     * @throws NullPointerException
     *             if names is null
     */
    public ToolSet addBuiltIn(Collection<String> names) {
        Objects.requireNonNull(names, "names must not be null");
        for (String name : names) {
            addBuiltIn(name);
        }
        return this;
    }

    /**
     * Adds a custom tool pattern. Matches tools registered via the SDK's
     * {@link SessionConfig#setTools(java.util.List)} option or via custom agents.
     *
     * @param name
     *            a specific custom tool name or {@code "*"} to match all custom
     *            tools
     * @return this {@code ToolSet} for chaining
     * @throws IllegalArgumentException
     *             if name is null, empty, or contains invalid characters
     */
    public ToolSet addCustom(String name) {
        validateName("custom", name);
        add("custom:" + name);
        return this;
    }

    /**
     * Adds an MCP tool pattern. Matches tools advertised by any configured MCP
     * server.
     *
     * @param toolName
     *            the runtime's canonical wire name for the MCP tool (e.g.
     *            {@code "github-list_issues"}), or {@code "*"} to match all MCP
     *            tools from any server
     * @return this {@code ToolSet} for chaining
     * @throws IllegalArgumentException
     *             if toolName is null, empty, or contains invalid characters
     */
    public ToolSet addMcp(String toolName) {
        validateName("mcp", toolName);
        add("mcp:" + toolName);
        return this;
    }

    private static void validateName(String kind, String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Invalid " + kind + " tool name: must not be null or empty.");
        }
        if ("*".equals(name)) {
            return;
        }
        if (!VALID_TOOL_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid " + kind + " tool name '" + name
                    + "': tool names must match /^[a-zA-Z0-9_-]+$/ or be the wildcard '*'.");
        }
    }
}
