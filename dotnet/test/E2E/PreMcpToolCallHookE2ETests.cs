/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Text.Json;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

/// <summary>
/// E2E tests for the preMcpToolCall hook, verifying meta manipulation scenarios:
/// setting meta, replacing meta, and removing meta.
/// </summary>
public class PreMcpToolCallHookE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "pre_mcp_tool_call_hook", output)
{
    private static string FindMetaEchoTestHarnessDir()
    {
        var dir = new DirectoryInfo(AppContext.BaseDirectory);
        while (dir != null)
        {
            var candidate = Path.Combine(dir.FullName, "test", "harness", "test-mcp-meta-echo-server.mjs");
            if (File.Exists(candidate))
                return Path.GetDirectoryName(candidate)!;
            dir = dir.Parent;
        }
        throw new InvalidOperationException("Could not find test/harness/test-mcp-meta-echo-server.mjs");
    }

    private static Dictionary<string, McpServerConfig> CreateMetaEchoMcpConfig(string testHarnessDir) => new()
    {
        ["meta-echo"] = new McpStdioServerConfig
        {
            Command = "node",
            Args = [Path.Combine(testHarnessDir, "test-mcp-meta-echo-server.mjs")],
            WorkingDirectory = testHarnessDir,
            Tools = ["*"]
        }
    };

    [Fact]
    public async Task Should_Set_Meta_Via_PreMcpToolCall_Hook()
    {
        var testHarnessDir = FindMetaEchoTestHarnessDir();
        var hookInputs = new List<PreMcpToolCallHookInput>();

        var session = await CreateSessionAsync(new SessionConfig
        {
            McpServers = CreateMetaEchoMcpConfig(testHarnessDir),
            Hooks = new SessionHooks
            {
                OnPreMcpToolCall = (input, invocation) =>
                {
                    hookInputs.Add(input);
                    using var doc = JsonDocument.Parse("""{"injected":"by-hook","source":"test"}""");
                    return Task.FromResult<PreMcpToolCallHookOutput?>(new PreMcpToolCallHookOutput
                    {
                        MetaToUse = doc.RootElement.Clone()
                    });
                },
            },
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        var message = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Use the meta-echo/echo_meta tool with value 'test-set'. Reply with just the raw tool result."
        });

        Assert.NotNull(message);
        Assert.Contains("injected", message!.Data.Content);
        Assert.Contains("by-hook", message.Data.Content);

        Assert.NotEmpty(hookInputs);
        Assert.Equal("meta-echo", hookInputs[0].ServerName);
        Assert.Equal("echo_meta", hookInputs[0].ToolName);
        Assert.False(string.IsNullOrEmpty(hookInputs[0].WorkingDirectory));
        Assert.True(hookInputs[0].Timestamp > DateTimeOffset.UnixEpoch);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Replace_Meta_Via_PreMcpToolCall_Hook()
    {
        var testHarnessDir = FindMetaEchoTestHarnessDir();
        var hookInputs = new List<PreMcpToolCallHookInput>();

        var session = await CreateSessionAsync(new SessionConfig
        {
            McpServers = CreateMetaEchoMcpConfig(testHarnessDir),
            Hooks = new SessionHooks
            {
                OnPreMcpToolCall = (input, invocation) =>
                {
                    hookInputs.Add(input);
                    // Completely replace: ignore input.Meta entirely
                    using var doc = JsonDocument.Parse("""{"completely":"replaced"}""");
                    return Task.FromResult<PreMcpToolCallHookOutput?>(new PreMcpToolCallHookOutput
                    {
                        MetaToUse = doc.RootElement.Clone()
                    });
                },
            },
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        var message = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Use the meta-echo/echo_meta tool with value 'test-replace'. Reply with just the raw tool result."
        });

        Assert.NotNull(message);
        Assert.Contains("completely", message!.Data.Content);
        Assert.Contains("replaced", message.Data.Content);

        Assert.NotEmpty(hookInputs);
        Assert.Equal("meta-echo", hookInputs[0].ServerName);
        Assert.Equal("echo_meta", hookInputs[0].ToolName);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Remove_Meta_Via_PreMcpToolCall_Hook()
    {
        var testHarnessDir = FindMetaEchoTestHarnessDir();
        var hookInputs = new List<PreMcpToolCallHookInput>();

        var session = await CreateSessionAsync(new SessionConfig
        {
            McpServers = CreateMetaEchoMcpConfig(testHarnessDir),
            Hooks = new SessionHooks
            {
                OnPreMcpToolCall = (input, invocation) =>
                {
                    hookInputs.Add(input);
                    // Return output with null MetaToUse to signal removal
                    return Task.FromResult<PreMcpToolCallHookOutput?>(new PreMcpToolCallHookOutput
                    {
                        MetaToUse = null
                    });
                },
            },
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        var message = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Use the meta-echo/echo_meta tool with value 'test-remove'. Reply with just the raw tool result."
        });

        Assert.NotNull(message);
        Assert.Contains("\"meta\":null", message!.Data.Content);
        Assert.Contains("test-remove", message.Data.Content);

        Assert.NotEmpty(hookInputs);
        Assert.Equal("meta-echo", hookInputs[0].ServerName);
        Assert.Equal("echo_meta", hookInputs[0].ToolName);

        await session.DisposeAsync();
    }
}
