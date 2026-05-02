/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Rpc;
using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test.E2E;

public class RpcServerE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rpc_server", output)
{
    private CopilotClient CreateAuthenticatedClient(string token)
    {
        var env = new Dictionary<string, string>(Ctx.GetEnvironment())
        {
            ["COPILOT_DEBUG_GITHUB_API_URL"] = Ctx.ProxyUrl,
        };

        return Ctx.CreateClient(options: new CopilotClientOptions
        {
            Environment = env,
            GitHubToken = token,
        });
    }

    private async Task ConfigureAuthenticatedUserAsync(
        string token,
        IReadOnlyDictionary<string, CopilotUserQuotaSnapshot>? quotaSnapshots = null)
    {
        await Ctx.SetCopilotUserByTokenAsync(token, new CopilotUserConfig(
            Login: "rpc-user",
            CopilotPlan: "individual_pro",
            Endpoints: new CopilotUserEndpoints(Api: Ctx.ProxyUrl, Telemetry: "https://localhost:1/telemetry"),
            AnalyticsTrackingId: "rpc-user-tracking-id",
            QuotaSnapshots: quotaSnapshots));
    }

    [Fact]
    public async Task Should_Call_Rpc_Ping_With_Typed_Params_And_Result()
    {
        await Client.StartAsync();

        var result = await Client.Rpc.PingAsync(message: "typed rpc test");

        Assert.Equal("pong: typed rpc test", result.Message);
        Assert.True(result.Timestamp >= 0);
    }

    [Fact]
    public async Task Should_Call_Rpc_Models_List_With_Typed_Result()
    {
        const string token = "rpc-models-token";
        await ConfigureAuthenticatedUserAsync(token);
        await using var client = CreateAuthenticatedClient(token);
        await client.StartAsync();

        var result = await client.Rpc.Models.ListAsync();

        Assert.NotNull(result.Models);
        Assert.Contains(result.Models, model => model.Id == "claude-sonnet-4.5");
        Assert.All(result.Models, model => Assert.False(string.IsNullOrWhiteSpace(model.Name)));
    }

    [Fact]
    public async Task Should_Call_Rpc_Account_GetQuota_When_Authenticated()
    {
        const string token = "rpc-quota-token";
        await ConfigureAuthenticatedUserAsync(
            token,
            new Dictionary<string, CopilotUserQuotaSnapshot>
            {
                ["chat"] = new(
                    Entitlement: 100,
                    OverageCount: 2,
                    OveragePermitted: true,
                    PercentRemaining: 75,
                    TimestampUtc: "2026-04-30T00:00:00Z"),
            });
        await using var client = CreateAuthenticatedClient(token);
        await client.StartAsync();

        var result = await client.Rpc.Account.GetQuotaAsync(gitHubToken: token);

        var chatQuota = Assert.Contains("chat", result.QuotaSnapshots);
        Assert.Equal(100, chatQuota.EntitlementRequests);
        Assert.Equal(25, chatQuota.UsedRequests);
        Assert.Equal(75, chatQuota.RemainingPercentage);
        Assert.Equal(2, chatQuota.Overage);
        Assert.True(chatQuota.UsageAllowedWithExhaustedQuota);
        Assert.True(chatQuota.OverageAllowedWithExhaustedQuota);
        Assert.Equal("2026-04-30T00:00:00Z", chatQuota.ResetDate);
    }

    [Fact]
    public async Task Should_Call_Rpc_Tools_List_With_Typed_Result()
    {
        await Client.StartAsync();

        var result = await Client.Rpc.Tools.ListAsync();

        Assert.NotNull(result.Tools);
        Assert.NotEmpty(result.Tools);
        Assert.All(result.Tools, tool => Assert.False(string.IsNullOrWhiteSpace(tool.Name)));
    }

    [Fact]
    public async Task Should_Discover_Server_Mcp_And_Skills()
    {
        await Client.StartAsync();

        var skillName = $"server-rpc-skill-{Guid.NewGuid():N}";
        var skillDirectory = CreateSkillDirectory(skillName, "Skill discovered by server-scoped RPC tests.");

        var mcp = await Client.Rpc.Mcp.DiscoverAsync(workingDirectory: Ctx.WorkDir);
        Assert.NotNull(mcp.Servers);

        var skills = await Client.Rpc.Skills.DiscoverAsync(skillDirectories: [skillDirectory]);
        var discoveredSkill = Assert.Single(skills.Skills, skill => string.Equals(skill.Name, skillName, StringComparison.Ordinal));
        Assert.Equal("Skill discovered by server-scoped RPC tests.", discoveredSkill.Description);
        Assert.True(discoveredSkill.Enabled);
        Assert.EndsWith(Path.Join(skillName, "SKILL.md"), discoveredSkill.Path);

        try
        {
            await Client.Rpc.Skills.Config.SetDisabledSkillsAsync([skillName]);
            var disabledSkills = await Client.Rpc.Skills.DiscoverAsync(skillDirectories: [skillDirectory]);
            var disabledSkill = Assert.Single(disabledSkills.Skills, skill => string.Equals(skill.Name, skillName, StringComparison.Ordinal));
            Assert.False(disabledSkill.Enabled);
        }
        finally
        {
            await Client.Rpc.Skills.Config.SetDisabledSkillsAsync([]);
        }
    }

    private string CreateSkillDirectory(string skillName, string description)
    {
        var skillsDir = Path.Join(Ctx.WorkDir, "server-rpc-skills", Guid.NewGuid().ToString("N"));
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

        return skillsDir;
    }
}
