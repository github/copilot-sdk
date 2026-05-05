/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;
using Xunit.Abstractions;
using RpcSkill = GitHub.Copilot.SDK.Rpc.Skill;
using RpcSkillList = GitHub.Copilot.SDK.Rpc.SkillList;

namespace GitHub.Copilot.SDK.Test.E2E;

public class RpcMcpAndSkillsE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rpc_mcp_and_skills", output)
{
    private static async Task<Exception> AssertFailureAsync(Func<Task> action, string expectedMessage)
    {
        var ex = await Assert.ThrowsAnyAsync<Exception>(action);
        Assert.Contains(expectedMessage, ex.ToString(), StringComparison.OrdinalIgnoreCase);
        return ex;
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
            McpServers = new Dictionary<string, McpServerConfig>
            {
                [serverName] = new McpStdioServerConfig
                {
                    Command = "echo",
                    Args = ["rpc-list-mcp-server"],
                    Tools = ["*"],
                },
            },
        });

        var result = await session.Rpc.Mcp.ListAsync();

        var server = Assert.Single(result.Servers, server => string.Equals(server.Name, serverName, StringComparison.Ordinal));
        Assert.True(Enum.IsDefined(server.Status));
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
            CliArgs = ["--yolo"],
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
            McpServers = new Dictionary<string, McpServerConfig>
            {
                ["configured-stdio-server"] = new McpStdioServerConfig
                {
                    Command = "echo",
                    Args = ["configured-stdio-server"],
                    Tools = ["*"],
                },
            },
        });

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
            McpServers = new Dictionary<string, McpServerConfig>
            {
                [serverName] = new McpStdioServerConfig
                {
                    Command = "echo",
                    Args = [serverName],
                    Tools = ["*"],
                },
            },
        });

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
            CliArgs = ["--yolo"],
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
}
