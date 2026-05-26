/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;
using RpcSessionFsSetProviderCapabilities = GitHub.Copilot.Rpc.SessionFsSetProviderCapabilities;
using RpcSessionFsSetProviderConventions = GitHub.Copilot.Rpc.SessionFsSetProviderConventions;
using RpcSessionContext = GitHub.Copilot.Rpc.SessionContext;
using RpcSessionListFilter = GitHub.Copilot.Rpc.SessionListFilter;
using RpcSessionMetadata = GitHub.Copilot.Rpc.SessionMetadata;

namespace GitHub.Copilot.Test.E2E;

public class RpcServerE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rpc_server", output)
{
    private static readonly TimeSpan SessionPersistenceTimeout = TimeSpan.FromSeconds(30);

    private static async Task<Exception> AssertImplementedFailureAsync(Func<Task> action, string method)
    {
        var ex = await Assert.ThrowsAnyAsync<Exception>(action);
        var text = ex.ToString();
        Assert.DoesNotContain($"Unhandled method {method}", text, StringComparison.OrdinalIgnoreCase);
        Assert.Contains("session", text, StringComparison.OrdinalIgnoreCase);
        return ex;
    }

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

    private string CreateUniqueWorkDirectory(string prefix)
    {
        var directory = Path.Join(Ctx.WorkDir, $"{prefix}-{Guid.NewGuid():N}");
        Directory.CreateDirectory(directory);
        return directory;
    }

    private static bool PathEquals(string? expected, string? actual)
    {
        if (expected is null || actual is null)
        {
            return expected is null && actual is null;
        }

        var comparison = OperatingSystem.IsWindows() ? StringComparison.OrdinalIgnoreCase : StringComparison.Ordinal;
        var normalizedExpected = Path.GetFullPath(expected).TrimEnd(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar);
        var normalizedActual = Path.GetFullPath(actual).TrimEnd(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar);
        return string.Equals(normalizedExpected, normalizedActual, comparison);
    }

    private async Task<string> SaveAndWaitForEventFileAsync(string sessionId)
        => await SaveAndWaitForEventFileAsync(Client, sessionId);

    private static async Task<string> SaveAndWaitForEventFileAsync(CopilotClient client, string sessionId)
    {
        var saveResult = await client.Rpc.Sessions.SaveAsync(sessionId);
        Assert.NotNull(saveResult);

        var pathResult = await client.Rpc.Sessions.GetEventFilePathAsync(sessionId);
        Assert.False(string.IsNullOrWhiteSpace(pathResult.FilePath));
        Assert.True(Path.IsPathRooted(pathResult.FilePath), $"Expected an absolute event file path, got '{pathResult.FilePath}'.");
        Assert.Equal("events.jsonl", Path.GetFileName(pathResult.FilePath));

        return pathResult.FilePath;
    }

    private static async Task<string> PersistSessionAsync(CopilotClient client, CopilotSession session, string marker)
    {
        await session.LogAsync(marker);
        return await SaveAndWaitForEventFileAsync(client, session.SessionId);
    }

    private async Task<RpcSessionMetadata> WaitForListedSessionAsync(
        string sessionId,
        RpcSessionListFilter? filter = null,
        long? metadataLimit = null)
        => await WaitForListedSessionAsync(Client, sessionId, filter, metadataLimit);

    private static async Task<RpcSessionMetadata> WaitForListedSessionAsync(
        CopilotClient client,
        string sessionId,
        RpcSessionListFilter? filter = null,
        long? metadataLimit = null)
    {
        RpcSessionMetadata? metadata = null;
        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                var list = await client.Rpc.Sessions.ListAsync(metadataLimit: metadataLimit, filter: filter);
                metadata = list.Sessions.FirstOrDefault(session => string.Equals(session.SessionId, sessionId, StringComparison.Ordinal));
                return metadata is not null;
            },
            timeout: SessionPersistenceTimeout,
            timeoutMessage: $"Timed out waiting for session '{sessionId}' to appear in sessions.list.");

        return metadata!;
    }

    [Fact]
    public async Task Should_Call_Rpc_Ping_With_Typed_Params_And_Result()
    {
        await Client.StartAsync();

        var result = await Client.Rpc.PingAsync(message: "typed rpc test");

        Assert.Equal("pong: typed rpc test", result.Message);
        Assert.NotEqual(default, result.Timestamp);
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
        Assert.Equal(DateTimeOffset.Parse("2026-04-30T00:00:00Z"), chatQuota.ResetDate);
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
    public async Task Should_Call_Rpc_SessionFs_SetProvider_With_Typed_Result()
    {
        await using var client = Ctx.CreateClient();
        await client.StartAsync();

        var result = await client.Rpc.SessionFs.SetProviderAsync(
            initialCwd: "/",
            sessionStatePath: "/session-state",
            conventions: RpcSessionFsSetProviderConventions.Posix,
            capabilities: new RpcSessionFsSetProviderCapabilities { Sqlite = true });

        Assert.True(result.Success);
    }

    [Fact]
    public async Task Should_Add_Secret_Filter_Values()
    {
        var environment = Ctx.GetEnvironment();
        environment["COPILOT_ENABLE_SECRET_FILTERING"] = "true";
        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Environment = environment,
        });
        await client.StartAsync();
        var secret = $"rpc-secret-{Guid.NewGuid():N}";

        var result = await client.Rpc.Secrets.AddFilterValuesAsync([secret]);

        Assert.True(result.Ok);
    }

    [Fact]
    public async Task Should_List_Find_And_Inspect_Persisted_Session_State()
    {
        var token = $"rpc-server-list-token-{Guid.NewGuid():N}";
        await ConfigureAuthenticatedUserAsync(token);
        await using var client = CreateAuthenticatedClient(token);
        var sessionId = Guid.NewGuid().ToString();
        var workingDirectory = CreateUniqueWorkDirectory("server-rpc-list");
        var missingTaskId = $"missing-task-{Guid.NewGuid():N}";
        var missingSessionId = Guid.NewGuid().ToString();

        var session = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = sessionId,
            WorkingDirectory = workingDirectory,
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        try
        {
            var eventFilePath = await SaveAndWaitForEventFileAsync(client, sessionId);
            Assert.Contains(sessionId, eventFilePath, StringComparison.OrdinalIgnoreCase);

            var listed = await client.Rpc.Sessions.ListAsync(
                metadataLimit: 0,
                filter: new RpcSessionListFilter { Cwd = workingDirectory });
            Assert.NotNull(listed.Sessions);
            Assert.DoesNotContain(listed.Sessions, session => !PathEquals(workingDirectory, session.Context?.Cwd));

            var prefix = sessionId[..8];
            var byPrefix = await client.Rpc.Sessions.FindByPrefixAsync(prefix);
            Assert.Null(byPrefix.SessionId);

            var byTaskId = await client.Rpc.Sessions.FindByTaskIdAsync(missingTaskId);
            Assert.Null(byTaskId.SessionId);

            var lastForContext = await client.Rpc.Sessions.GetLastForContextAsync(new RpcSessionContext { Cwd = workingDirectory });
            Assert.Null(lastForContext.SessionId);

            var sizes = await client.Rpc.Sessions.GetSizesAsync();
            Assert.NotNull(sizes.Sizes);
            if (sizes.Sizes.TryGetValue(sessionId, out var size))
            {
                Assert.True(size >= 0);
            }

            var inUse = await client.Rpc.Sessions.CheckInUseAsync([sessionId, missingSessionId]);
            Assert.DoesNotContain(missingSessionId, inUse.InUse);

            var remoteSteerable = await client.Rpc.Sessions.GetPersistedRemoteSteerableAsync(sessionId);
            Assert.Null(remoteSteerable.RemoteSteerable);
        }
        finally
        {
            await session.DisposeAsync();
        }
    }

    [Fact]
    public async Task Should_Enrich_Basic_Session_Metadata()
    {
        var token = $"rpc-server-enrich-token-{Guid.NewGuid():N}";
        await ConfigureAuthenticatedUserAsync(token);
        await using var client = CreateAuthenticatedClient(token);
        var sessionId = Guid.NewGuid().ToString();
        var workingDirectory = CreateUniqueWorkDirectory("server-rpc-enrich");

        var session = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = sessionId,
            WorkingDirectory = workingDirectory,
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        try
        {
            await SaveAndWaitForEventFileAsync(client, sessionId);

            var basic = new RpcSessionMetadata
            {
                SessionId = sessionId,
                StartTime = DateTimeOffset.UtcNow.ToString("O"),
                ModifiedTime = DateTimeOffset.UtcNow.ToString("O"),
                IsRemote = false,
                Name = "Basic metadata",
                Context = new RpcSessionContext { Cwd = workingDirectory },
            };

            var result = await client.Rpc.Sessions.EnrichMetadataAsync([
                basic,
            ]);

            var enriched = Assert.Single(result.Sessions);
            Assert.Equal(sessionId, enriched.SessionId);
            Assert.True(PathEquals(workingDirectory, enriched.Context?.Cwd),
                $"Expected enriched session cwd '{workingDirectory}', actual '{enriched.Context?.Cwd}'.");
            Assert.False(enriched.IsRemote);
        }
        finally
        {
            await session.DisposeAsync();
        }
    }

    [Fact]
    public async Task Should_Close_Active_Session_And_Release_Lock()
    {
        var token = $"rpc-server-close-token-{Guid.NewGuid():N}";
        await ConfigureAuthenticatedUserAsync(token);
        await using var client = CreateAuthenticatedClient(token);
        var sessionId = Guid.NewGuid().ToString();
        var workingDirectory = CreateUniqueWorkDirectory("server-rpc-close");
        var session = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = sessionId,
            WorkingDirectory = workingDirectory,
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        await PersistSessionAsync(client, session, "SERVER_RPC_CLOSE_READY");

        var closeResult = await client.Rpc.Sessions.CloseAsync(sessionId);
        Assert.NotNull(closeResult);

        var releaseResult = await client.Rpc.Sessions.ReleaseLockAsync(sessionId);
        Assert.NotNull(releaseResult);

        var inUse = await client.Rpc.Sessions.CheckInUseAsync([sessionId]);
        Assert.DoesNotContain(sessionId, inUse.InUse);
    }

    [Fact]
    public async Task Should_Check_In_Use_Session_From_Another_Runtime_And_Release_Lock()
    {
        var sessionId = Guid.NewGuid().ToString();
        var workingDirectory = CreateUniqueWorkDirectory("server-rpc-in-use");
        await using var otherClient = Ctx.CreateClient(useStdio: true);
        await using var otherSession = await otherClient.CreateSessionAsync(new SessionConfig
        {
            SessionId = sessionId,
            WorkingDirectory = workingDirectory,
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        await Client.StartAsync();

        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                var result = await Client.Rpc.Sessions.CheckInUseAsync([sessionId]);
                return result.InUse.Contains(sessionId);
            },
            timeout: SessionPersistenceTimeout,
            timeoutMessage: $"Timed out waiting for sessions.checkInUse to report '{sessionId}' as held by another runtime.");

        var releaseResult = await otherClient.Rpc.Sessions.ReleaseLockAsync(sessionId);
        Assert.NotNull(releaseResult);

        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                var result = await Client.Rpc.Sessions.CheckInUseAsync([sessionId]);
                return !result.InUse.Contains(sessionId);
            },
            timeout: SessionPersistenceTimeout,
            timeoutMessage: $"Timed out waiting for sessions.releaseLock to release '{sessionId}'.");
    }

    [Fact]
    public async Task Should_Prune_DryRun_And_BulkDelete_Persisted_Session()
    {
        var token = $"rpc-server-delete-token-{Guid.NewGuid():N}";
        await ConfigureAuthenticatedUserAsync(token);
        await using var client = CreateAuthenticatedClient(token);
        var sessionId = Guid.NewGuid().ToString();
        var missingSessionId = Guid.NewGuid().ToString();
        var workingDirectory = CreateUniqueWorkDirectory("server-rpc-delete");

        var session = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = sessionId,
            WorkingDirectory = workingDirectory,
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        await SaveAndWaitForEventFileAsync(client, sessionId);
        await client.Rpc.Sessions.CloseAsync(sessionId);

        var prune = await client.Rpc.Sessions.PruneOldAsync(
            olderThanDays: 0,
            dryRun: true,
            includeNamed: true,
            excludeSessionIds: []);

        Assert.True(prune.DryRun);
        Assert.DoesNotContain(missingSessionId, prune.Candidates);
        Assert.DoesNotContain(sessionId, prune.Deleted);
        Assert.True(prune.FreedBytes >= 0);

        var delete = await client.Rpc.Sessions.BulkDeleteAsync([sessionId, missingSessionId]);
        Assert.True(delete.FreedBytes.TryGetValue(sessionId, out var freedBytes), $"Expected sessions.bulkDelete to delete '{sessionId}'.");
        Assert.True(freedBytes >= 0);
        if (delete.FreedBytes.TryGetValue(missingSessionId, out var missingFreedBytes))
        {
            Assert.Equal(0, missingFreedBytes);
        }

        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                var list = await client.Rpc.Sessions.ListAsync();
                return list.Sessions.All(s => s.SessionId != sessionId);
            },
            timeout: SessionPersistenceTimeout,
            timeoutMessage: $"Timed out waiting for sessions.bulkDelete to remove '{sessionId}'.");

        GC.KeepAlive(session);
    }

    [Fact]
    public async Task Should_Set_Additional_Plugins_And_Reload_Deferred_Hooks()
    {
        await Client.StartAsync();
        var clearPlugins = await Client.Rpc.Sessions.SetAdditionalPluginsAsync([]);
        Assert.NotNull(clearPlugins);

        var sessionId = Guid.NewGuid().ToString();
        var workingDirectory = CreateUniqueWorkDirectory("server-rpc-hooks");
        var session = await CreateSessionAsync(new SessionConfig
        {
            SessionId = sessionId,
            WorkingDirectory = workingDirectory,
            EnableConfigDiscovery = false,
        });

        try
        {
            var reload = await Client.Rpc.Sessions.ReloadPluginHooksAsync(sessionId, deferRepoHooks: true);
            Assert.NotNull(reload);

            var loaded = await Client.Rpc.Sessions.LoadDeferredRepoHooksAsync(sessionId);
            Assert.NotNull(loaded.StartupPrompts);
            Assert.Equal(0, loaded.HookCount);
            Assert.Empty(loaded.StartupPrompts);
        }
        finally
        {
            await Client.Rpc.Sessions.SetAdditionalPluginsAsync([]);
            await session.DisposeAsync();
        }
    }

    [Fact]
    public async Task Should_Report_Implemented_Error_When_Connecting_Unknown_Remote_Session()
    {
        await Client.StartAsync();
        var remoteSessionId = $"remote-{Guid.NewGuid():N}";

        var ex = await AssertImplementedFailureAsync(
            () => Client.Rpc.Sessions.ConnectAsync(remoteSessionId),
            "sessions.connect");

        Assert.False(string.IsNullOrWhiteSpace(ex.Message));
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
