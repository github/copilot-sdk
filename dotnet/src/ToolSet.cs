/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Text.RegularExpressions;

namespace GitHub.Copilot;

/// <summary>
/// Builder for <see cref="SessionConfigBase.AvailableTools"/> /
/// <see cref="SessionConfigBase.ExcludedTools"/> using source-qualified filter
/// patterns (<c>builtin:*</c>, <c>mcp:&lt;name&gt;</c>, <c>custom:*</c>, etc.).
/// </summary>
/// <remarks>
/// <para>
/// Tools are classified by the runtime at registration time (not from name
/// parsing), so <c>AddBuiltIn("foo")</c> matches only tools the runtime
/// registered as built-in, even if an MCP server or custom-agent extension
/// happens to register a tool with the same wire name.
/// </para>
/// <para>
/// <see cref="ToolSet"/> inherits from <c>List&lt;string&gt;</c>, so instances
/// can be assigned directly to <see cref="SessionConfigBase.AvailableTools"/>
/// or <see cref="SessionConfigBase.ExcludedTools"/>.
/// </para>
/// </remarks>
/// <example>
/// <code>
/// var session = await client.CreateSessionAsync(new SessionConfig
/// {
///     AvailableTools = new ToolSet()
///         .AddBuiltIn(BuiltInTools.Isolated)
///         .AddMcp("*")
///         .AddCustom("*"),
/// });
/// </code>
/// </example>
public sealed class ToolSet : List<string>
{
    private static readonly Regex s_validToolName = new(@"^[a-zA-Z0-9_-]+$", RegexOptions.Compiled);

    /// <summary>
    /// Adds one or more built-in tool patterns.
    /// </summary>
    /// <param name="name">A specific built-in tool name (e.g. <c>"bash"</c>) or
    /// <c>"*"</c> to match all built-in tools.</param>
    /// <returns>This <see cref="ToolSet"/> for chaining.</returns>
    public ToolSet AddBuiltIn(string name)
    {
        ValidateName("builtin", name);
        Add($"builtin:{name}");
        return this;
    }

    /// <summary>
    /// Adds a list of built-in tool patterns
    /// (e.g. <see cref="BuiltInTools.Isolated"/>).
    /// </summary>
    /// <param name="names">Built-in tool names to add.</param>
    /// <returns>This <see cref="ToolSet"/> for chaining.</returns>
    public ToolSet AddBuiltIn(IEnumerable<string> names)
    {
        ArgumentNullException.ThrowIfNull(names);
        foreach (var name in names)
        {
            AddBuiltIn(name);
        }
        return this;
    }

    /// <summary>
    /// Adds a custom tool pattern. Matches tools registered via the SDK's
    /// <see cref="SessionConfigBase.Tools"/> option or via custom agents.
    /// </summary>
    /// <param name="name">A specific custom tool name or <c>"*"</c> to match
    /// all custom tools.</param>
    /// <returns>This <see cref="ToolSet"/> for chaining.</returns>
    public ToolSet AddCustom(string name)
    {
        ValidateName("custom", name);
        Add($"custom:{name}");
        return this;
    }

    /// <summary>
    /// Adds an MCP tool pattern. Matches tools advertised by any configured
    /// MCP server.
    /// </summary>
    /// <param name="toolName">The runtime's canonical wire name for the MCP
    /// tool (e.g. <c>"github-list_issues"</c>), or <c>"*"</c> to match all
    /// MCP tools from any server.</param>
    /// <returns>This <see cref="ToolSet"/> for chaining.</returns>
    public ToolSet AddMcp(string toolName)
    {
        ValidateName("mcp", toolName);
        Add($"mcp:{toolName}");
        return this;
    }

    private static void ValidateName(string kind, string name)
    {
        if (string.IsNullOrEmpty(name))
        {
            throw new ArgumentException(
                $"Invalid {kind} tool name: must not be null or empty.",
                nameof(name));
        }
        if (name == "*")
        {
            return;
        }
        if (!s_validToolName.IsMatch(name))
        {
            throw new ArgumentException(
                $"Invalid {kind} tool name '{name}': tool names must match /^[a-zA-Z0-9_-]+$/ " +
                "or be the wildcard '*'.",
                nameof(name));
        }
    }
}

/// <summary>
/// Curated sets of built-in tool names for common scenarios. Each constant is
/// meant to be passed to <see cref="ToolSet.AddBuiltIn(IEnumerable{string})"/>.
/// </summary>
public static class BuiltInTools
{
    /// <summary>
    /// Built-in tools that operate only within the bounds of a single session
    /// — no host filesystem access outside the session, no cross-session
    /// state, no host environment access, no network. Safe to enable in
    /// <see cref="CopilotClientMode.Empty"/> scenarios (e.g. multi-tenant
    /// servers) without leaking host capabilities.
    /// </summary>
    /// <remarks>
    /// <para>
    /// <b>Contract:</b> tools in this set MUST NOT be extended (even behind
    /// options or args) to read or write state outside the session boundary.
    /// Adding cross-session or host-state behavior to one of these tools is a
    /// breaking change that requires removing it from this set.
    /// </para>
    /// </remarks>
    public static IReadOnlyList<string> Isolated { get; } =
    [
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
    ];
}
