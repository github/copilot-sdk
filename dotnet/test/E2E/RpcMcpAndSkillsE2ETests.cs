/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Text.Json;
using GitHub.Copilot.Rpc;
using Xunit;
using Xunit.Abstractions;
using RpcSkill = GitHub.Copilot.Rpc.Skill;
using RpcSkillList = GitHub.Copilot.Rpc.SkillList;

namespace GitHub.Copilot.Test.E2E;

public class RpcMcpAndSkillsE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rpc_mcp_and_skills", output)
{
    private static async Task<Exception> AssertFailureAsync(Func<Task> action, string expectedMessage)
    {
        var ex = await Assert.ThrowsAnyAsync<Exception>(action);
        var message = ex.ToString();
        Assert.Contains(expectedMessage, message, StringComparison.OrdinalIgnoreCase);
        AssertNotUnhandledMethod(message);
        return ex;
    }

    private static void AssertNotUnhandledMethod(string message)
    {
        Assert.DoesNotContain("Unhandled method", message, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task Should_List_And_Toggle_Session_Skills()
    {
        var skillName = $"session-rpc-skill-{Guid.NewGuid():N}";
        var skillsDir = CreateSkillDirectory(skillName, "Session skill controlled by RPC.");
        var session = await CreateSessionAsync(new SessionConfig
        {
            SkillDirectories = [skillsDir],
            DisabledSkills = [skillName],
        });

        var disabled = await session.Rpc.Skills.ListAsync();
        AssertSkill(disabled, skillName, enabled: false);

        await session.Rpc.Skills.EnableAsync(skillName);
        var enabled = await session.Rpc.Skills.ListAsync();
        AssertSkill(enabled, skillName, enabled: true);

        await session.Rpc.Skills.DisableAsync(skillName);
        var disabledAgain = await session.Rpc.Skills.ListAsync();
        AssertSkill(disabledAgain, skillName, enabled: false);
    }

    [Fact]
    public async Task Should_Ensure_Skills_Are_Loaded_And_List_Invoked_Skills()
    {
        var skillName = $"ensure-rpc-skill-{Guid.NewGuid():N}";
        var skillsDir = CreateSkillDirectory(skillName, "Skill loaded explicitly by RPC.");
        var session = await CreateSessionAsync(new SessionConfig
        {
            SkillDirectories = [skillsDir],
        });

        await session.Rpc.Skills.EnsureLoadedAsync();

        var loaded = await session.Rpc.Skills.ListAsync();
        var skill = AssertSkill(loaded, skillName, enabled: true);
        Assert.Equal("Skill loaded explicitly by RPC.", skill.Description);

        var invoked = await session.Rpc.Skills.GetInvokedAsync();
        Assert.NotNull(invoked.Skills);
        Assert.Empty(invoked.Skills);
    }

    [Fact]
    public async Task Should_Reload_Session_Skills()
    {
        var skillsDir = Path.Join(Ctx.WorkDir, "reloadable-rpc-skills", Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(skillsDir);
        var skillName = $"reload-rpc-skill-{Guid.NewGuid():N}";

        var session = await CreateSessionAsync(new SessionConfig { SkillDirectories = [skillsDir] });
        var before = await session.Rpc.Skills.ListAsync();
        Assert.DoesNotContain(before.Skills, skill => string.Equals(skill.Name, skillName, StringComparison.Ordinal));

        CreateSkill(skillsDir, skillName, "Skill added after session creation.");
        await session.Rpc.Skills.ReloadAsync();

        var after = await session.Rpc.Skills.ListAsync();
        var reloadedSkill = AssertSkill(after, skillName, enabled: true);
        Assert.Equal("Skill added after session creation.", reloadedSkill.Description);
    }

    [Fact]
    public async Task Should_List_Mcp_Servers_With_Configured_Server()
    {
        const string serverName = "rpc-list-mcp-server";
        var session = await CreateSessionAsync(new SessionConfig
        {
            McpServers = CreateTestMcpServers(serverName),
        });

        await WaitForMcpServerStatusAsync(session, serverName, McpServerStatus.Connected);
        var result = await session.Rpc.Mcp.ListAsync();

        var server = Assert.Single(result.Servers, server => string.Equals(server.Name, serverName, StringComparison.Ordinal));
        Assert.Equal(McpServerStatus.Connected, server.Status);
    }

    [Fact]
    public async Task Should_Set_Mcp_Env_Value_Mode_And_Remove_GitHub_Server()
    {
        const string serverName = "github";
        var session = await CreateSessionAsync(new SessionConfig
        {
            McpServers = CreateTestMcpServers(serverName),
        });

        await WaitForMcpServerStatusAsync(session, serverName, McpServerStatus.Connected);

        var direct = await session.Rpc.Mcp.SetEnvValueModeAsync(McpSetEnvValueModeDetails.Direct);
        Assert.Equal(McpSetEnvValueModeDetails.Direct, direct.Mode);

        var indirect = await session.Rpc.Mcp.SetEnvValueModeAsync(McpSetEnvValueModeDetails.Indirect);
        Assert.Equal(McpSetEnvValueModeDetails.Indirect, indirect.Mode);

        var removeGitHub = await session.Rpc.Mcp.RemoveGitHubAsync();
        Assert.False(removeGitHub.Removed);

        var servers = await session.Rpc.Mcp.ListAsync();
        Assert.Contains(servers.Servers, server =>
            string.Equals(server.Name, serverName, StringComparison.Ordinal)
            && server.Status == McpServerStatus.Connected);
    }

    [Fact]
    public async Task Should_Report_Mcp_Sampling_Failure_And_Cancel_Missing_Sampling()
    {
        const string serverName = "rpc-sampling-server";
        var session = await CreateSessionAsync(new SessionConfig
        {
            McpServers = CreateTestMcpServers(serverName),
        });

        await WaitForMcpServerStatusAsync(session, serverName, McpServerStatus.Connected);

        var cancelMissing = await session.Rpc.Mcp.CancelSamplingExecutionAsync($"missing-{Guid.NewGuid():N}");
        Assert.False(cancelMissing.Cancelled);

        try
        {
            var result = await session.Rpc.Mcp.ExecuteSamplingAsync(
                $"sampling-{Guid.NewGuid():N}",
                serverName,
                $"mcp-request-{Guid.NewGuid():N}",
                new McpExecuteSamplingRequest());

            Assert.Equal(McpSamplingExecutionAction.Failure, result.Action);
            Assert.Null(result.Result);
            Assert.False(string.IsNullOrWhiteSpace(result.Error));
            AssertNotUnhandledMethod(result.Error!);
            AssertSamplingError(result.Error!);
        }
        catch (Exception ex)
        {
            var message = ex.ToString();
            AssertNotUnhandledMethod(message);
            AssertSamplingError(message);
        }
    }

    [Fact]
    public async Task Should_List_Plugins()
    {
        var session = await CreateSessionAsync();

        var result = await session.Rpc.Plugins.ListAsync();

        Assert.NotNull(result.Plugins);
        Assert.All(result.Plugins, plugin => Assert.False(string.IsNullOrWhiteSpace(plugin.Name)));
    }

    [Fact]
    public async Task Should_List_Extensions()
    {
        // Use --yolo to auto-approve extension permission gates at the CLI level,
        // preventing breakage from new gates (e.g., extension-permission-access).
        await using var yoloClient = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(args: ["--yolo"]),
        });
        await using var session = await yoloClient.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        var result = await session.Rpc.Extensions.ListAsync();

        Assert.NotNull(result.Extensions);
        Assert.All(result.Extensions, extension =>
        {
            Assert.False(string.IsNullOrWhiteSpace(extension.Id));
            Assert.False(string.IsNullOrWhiteSpace(extension.Name));
        });
    }

    [Fact]
    public async Task Should_Round_Trip_Mcp_App_Host_Context()
    {
        await using var client = CreateMcpAppsClient();
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        await session.Rpc.Mcp.Apps.SetHostContextAsync(new McpAppsSetHostContextDetails
        {
            AvailableDisplayModes =
            [
                McpAppsSetHostContextDetailsAvailableDisplayMode.Inline,
                McpAppsSetHostContextDetailsAvailableDisplayMode.Fullscreen,
            ],
            DisplayMode = McpAppsSetHostContextDetailsDisplayMode.Inline,
            Locale = "en-GB",
            Platform = McpAppsSetHostContextDetailsPlatform.Desktop,
            Theme = McpAppsSetHostContextDetailsTheme.Dark,
            TimeZone = "Etc/UTC",
            UserAgent = "dotnet-sdk-e2e",
        });

        var result = await session.Rpc.Mcp.Apps.GetHostContextAsync();

        Assert.Equal("inline", result.Context.DisplayMode?.Value);
        Assert.Equal("en-GB", result.Context.Locale);
        Assert.Equal("desktop", result.Context.Platform?.Value);
        Assert.Equal("dark", result.Context.Theme?.Value);
        Assert.Equal("Etc/UTC", result.Context.TimeZone);
        Assert.Equal("dotnet-sdk-e2e", result.Context.UserAgent);
        Assert.NotNull(result.Context.AvailableDisplayModes);
        var displayModes = result.Context.AvailableDisplayModes!;
        Assert.Equal(2, displayModes.Count);
        Assert.Contains(displayModes, mode => mode.Value == "inline");
        Assert.Contains(displayModes, mode => mode.Value == "fullscreen");
    }

    [Fact]
    public async Task Should_Diagnose_And_Report_Mcp_App_Capability_Errors()
    {
        const string serverName = "rpc-apps-server";
        const string otherServerName = "rpc-apps-other-server";
        var mcpServers = CreateTestMcpServers(serverName, otherServerName);
        ((McpStdioServerConfig)mcpServers[serverName]).Env =
            new Dictionary<string, string> { ["MCP_APP_RPC_VALUE"] = "from-app-rpc" };

        await using var client = CreateMcpAppsClient();
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            McpServers = mcpServers,
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        await WaitForMcpServerStatusAsync(session, serverName, McpServerStatus.Connected);
        await WaitForMcpServerStatusAsync(session, otherServerName, McpServerStatus.Connected);

        var diagnose = await session.Rpc.Mcp.Apps.DiagnoseAsync(serverName);
        Assert.NotNull(diagnose.Capability);
        Assert.True(diagnose.Server.Connected);
        Assert.True(diagnose.Server.ToolCount >= 1);
        Assert.Equal(0, diagnose.Server.ToolsWithUiMeta);
        Assert.Empty(diagnose.Server.SampleToolNames);

        await AssertFailureAsync(
            () => session.Rpc.Mcp.Apps.ListToolsAsync(serverName, originServerName: serverName),
            "mcp-apps");
        await AssertFailureAsync(
            () => session.Rpc.Mcp.Apps.ListToolsAsync(serverName, originServerName: otherServerName),
            "mcp-apps");
        await AssertFailureAsync(
            () => session.Rpc.Mcp.Apps.CallToolAsync(
                serverName,
                "get_env",
                originServerName: serverName,
                arguments: new Dictionary<string, JsonElement>
                {
                    ["name"] = ParseJsonElement("\"MCP_APP_RPC_VALUE\""),
                }),
            "mcp-apps");
    }

    [Fact]
    public async Task Should_Report_Error_When_Mcp_App_Resource_Is_Not_Available()
    {
        const string serverName = "rpc-apps-resource-server";
        await using var client = CreateMcpAppsClient();
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            McpServers = CreateTestMcpServers(serverName),
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        await WaitForMcpServerStatusAsync(session, serverName, McpServerStatus.Connected);

        var ex = await Assert.ThrowsAnyAsync<Exception>(
            () => session.Rpc.Mcp.Apps.ReadResourceAsync(serverName, "ui://missing-resource"));
        var message = ex.ToString();
        AssertNotUnhandledMethod(message);
        Assert.True(
            message.Contains("resource", StringComparison.OrdinalIgnoreCase)
                || message.Contains("not found", StringComparison.OrdinalIgnoreCase)
                || message.Contains("Method not found", StringComparison.OrdinalIgnoreCase),
            message);
    }

    [Fact]
    public async Task Should_Report_Error_When_Mcp_Host_Is_Not_Initialized()
    {
        var session = await CreateSessionAsync();

        await AssertFailureAsync(
            () => session.Rpc.Mcp.EnableAsync("missing-server"),
            "No MCP host initialized");
        await AssertFailureAsync(
            () => session.Rpc.Mcp.DisableAsync("missing-server"),
            "No MCP host initialized");
        await AssertFailureAsync(
            () => session.Rpc.Mcp.ReloadAsync(),
            "MCP config reload not available");
        await AssertFailureAsync(
            () => session.Rpc.Mcp.Oauth.LoginAsync("missing-server"),
            "MCP host is not available");
    }

    [Fact]
    public async Task Should_Report_Error_When_Mcp_Oauth_Server_Is_Not_Configured()
    {
        var session = await CreateSessionAsync(new SessionConfig
        {
            McpServers = CreateTestMcpServers("configured-stdio-server"),
        });
        await WaitForMcpServerStatusAsync(session, "configured-stdio-server", McpServerStatus.Connected);

        await AssertFailureAsync(
            () => session.Rpc.Mcp.Oauth.LoginAsync("missing-server"),
            "is not configured");
    }

    [Fact]
    public async Task Should_Report_Error_When_Mcp_Oauth_Server_Is_Not_Remote()
    {
        const string serverName = "configured-stdio-server";
        var session = await CreateSessionAsync(new SessionConfig
        {
            McpServers = CreateTestMcpServers(serverName),
        });
        await WaitForMcpServerStatusAsync(session, serverName, McpServerStatus.Connected);

        await AssertFailureAsync(
            () => session.Rpc.Mcp.Oauth.LoginAsync(serverName, forceReauth: true, clientName: "SDK E2E", callbackSuccessMessage: "Done"),
            "not a remote server");
    }

    [Fact]
    public async Task Should_Report_Error_When_Extensions_Are_Not_Available()
    {
        // Use --yolo to auto-approve extension permission gates at the CLI level,
        // preventing breakage from new gates (e.g., extension-permission-access).
        await using var yoloClient = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(args: ["--yolo"]),
        });
        await using var session = await yoloClient.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        await AssertFailureAsync(
            () => session.Rpc.Extensions.EnableAsync("missing-extension"),
            "Extensions not available");
        await AssertFailureAsync(
            () => session.Rpc.Extensions.DisableAsync("missing-extension"),
            "Extensions not available");
        await AssertFailureAsync(
            () => session.Rpc.Extensions.ReloadAsync(),
            "Extensions not available");
    }

    private string CreateSkillDirectory(string skillName, string description)
    {
        var skillsDir = Path.Join(Ctx.WorkDir, "session-rpc-skills", Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(skillsDir);
        CreateSkill(skillsDir, skillName, description);
        return skillsDir;
    }

    private CopilotClient CreateMcpAppsClient()
    {
        var environment = Ctx.GetEnvironment();
        environment["COPILOT_MCP_APPS"] = "true";
        environment["MCP_APPS"] = "true";

        return Ctx.CreateClient(options: new CopilotClientOptions
        {
            Environment = environment,
        });
    }

    private static void CreateSkill(string skillsDir, string skillName, string description)
    {
        var skillSubdir = Path.Join(skillsDir, skillName);
        Directory.CreateDirectory(skillSubdir);

        var skillContent = $"""
            ---
            name: {skillName}
            description: {description}
            ---

            # {skillName}

            This skill is used by RPC E2E tests.
            """.ReplaceLineEndings("\n");
        File.WriteAllText(Path.Join(skillSubdir, "SKILL.md"), skillContent);
    }

    private static RpcSkill AssertSkill(RpcSkillList list, string skillName, bool enabled)
    {
        var skill = Assert.Single(list.Skills, skill => string.Equals(skill.Name, skillName, StringComparison.Ordinal));
        Assert.Equal(enabled, skill.Enabled);
        Assert.EndsWith(Path.Join(skillName, "SKILL.md"), skill.Path);
        return skill;
    }

    private static string? GetStringProperty(IDictionary<string, JsonElement> properties, string name)
    {
        return properties.TryGetValue(name, out var value) && value.ValueKind == JsonValueKind.String
            ? value.GetString()
            : null;
    }

    private static JsonElement ParseJsonElement(string json)
    {
        using var document = JsonDocument.Parse(json);
        return document.RootElement.Clone();
    }

    private static void AssertToolTextContent(IDictionary<string, JsonElement> result, string expectedText)
    {
        Assert.True(result.TryGetValue("content", out var content));
        Assert.Equal(JsonValueKind.Array, content.ValueKind);
        var contentItem = Assert.Single(content.EnumerateArray());
        Assert.Equal("text", contentItem.GetProperty("type").GetString());
        Assert.Equal(expectedText, contentItem.GetProperty("text").GetString());
    }

    private static void AssertSamplingError(string message)
    {
        Assert.True(
            message.Contains("sampling", StringComparison.OrdinalIgnoreCase)
                || message.Contains("message", StringComparison.OrdinalIgnoreCase)
                || message.Contains("request", StringComparison.OrdinalIgnoreCase),
            message);
    }
}
