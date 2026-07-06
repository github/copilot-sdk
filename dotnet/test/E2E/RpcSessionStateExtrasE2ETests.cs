/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

/// <summary>
/// E2E coverage for session-scoped RPC methods that were previously untested:
/// completions, model.list, metadata.activity/context attribution/heaviest messages,
/// permissions.getAllowAll/setAllowAll, plan.readSqlTodos, provider.add,
/// telemetry.getEngagementId, tools.getCurrentMetadata/updateSubagentSettings,
/// session visibility, and the session-scoped plugins.reload.
/// </summary>
public class RpcSessionStateExtrasE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rpc_session_state_extras", output)
{
    [Fact]
    public async Task Should_List_Models_For_Session()
    {
        // model.list resolves models through the session's own auth context, which requires the
        // GitHub token -> user resolution to be served by the proxy (a fresh shared client does not
        // route token resolution there). Use a dedicated authenticated client like the server-scoped
        // models.list coverage does.
        const string token = "rpc-session-model-list-token";
        await ConfigureAuthenticatedUserAsync(token);
        await using var client = CreateAuthenticatedClient(token);
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            Model = "claude-sonnet-4.5",
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        var result = await session.Rpc.Model.ListAsync();

        Assert.NotNull(result.List);
        Assert.NotEmpty(result.List);
        // The configured model must be present in the returned catalog.
        Assert.Contains(result.List, model => model.GetRawText().Contains("claude-sonnet-4.5", StringComparison.Ordinal));
    }

    [Fact]
    public async Task Should_Add_Byok_Provider_And_Model_At_Runtime()
    {
        await using var session = await CreateSessionAsync();
        var providerName = $"sdk-runtime-provider-{Guid.NewGuid():N}";
        var modelId = "sdk-runtime-model";
        var selectionId = $"{providerName}/{modelId}";

        var added = await session.Rpc.Provider.AddAsync(
            providers:
            [
                new GitHub.Copilot.Rpc.NamedProviderConfig
                {
                    Name = providerName,
                    Type = ProviderConfigType.Openai,
                    WireApi = ProviderConfigWireApi.Completions,
                    BaseUrl = "https://api.example.test/v1",
                    ApiKey = "runtime-provider-secret",
                    Headers = new Dictionary<string, string> { ["X-SDK-Provider"] = "runtime" },
                },
            ],
            models:
            [
                new GitHub.Copilot.Rpc.ProviderModelConfig
                {
                    Provider = providerName,
                    Id = modelId,
                    Name = "SDK Runtime Model",
                    ModelId = "claude-sonnet-4.5",
                    WireModel = "wire-sdk-runtime-model",
                    MaxContextWindowTokens = 4_096,
                    MaxPromptTokens = 3_072,
                    MaxOutputTokens = 1_024,
                    Capabilities = new GitHub.Copilot.Rpc.ModelCapabilitiesOverride
                    {
                        Limits = new GitHub.Copilot.Rpc.ModelCapabilitiesOverrideLimits
                        {
                            MaxContextWindowTokens = 4_096,
                            MaxPromptTokens = 3_072,
                            MaxOutputTokens = 1_024,
                        },
                        Supports = new GitHub.Copilot.Rpc.ModelCapabilitiesOverrideSupports
                        {
                            ReasoningEffort = false,
                            Vision = false,
                        },
                    },
                },
            ]);

        var addedModel = Assert.Single(added.Models);
        var addedModelJson = addedModel.GetRawText();
        Assert.Contains(selectionId, addedModelJson, StringComparison.Ordinal);
        Assert.Contains("SDK Runtime Model", addedModelJson, StringComparison.Ordinal);

        var listed = await session.Rpc.Model.ListAsync();
        Assert.Contains(listed.List, model => model.GetRawText().Contains(selectionId, StringComparison.Ordinal));

        var switched = await session.Rpc.Model.SwitchToAsync(selectionId);
        Assert.Equal(selectionId, switched.ModelId);
        Assert.Equal(selectionId, (await session.Rpc.Model.GetCurrentAsync()).ModelId);
    }

    [Fact]
    public async Task Should_Report_Session_Activity_When_Idle()
    {
        await using var session = await CreateSessionAsync();

        var activity = await session.Rpc.Metadata.ActivityAsync();

        // A freshly created session that has not been sent any work is idle: no active turns or
        // tasks, and nothing to abort.
        Assert.False(activity.HasActiveWork, "Expected a freshly created session to report no active work.");
        Assert.False(activity.Abortable, "Expected a freshly created session to have nothing abortable.");
    }

    [Fact]
    public async Task Should_Return_Empty_Completions_When_Host_Does_Not_Provide_Them()
    {
        await using var session = await CreateSessionAsync();

        var triggers = await session.Rpc.Completions.GetTriggerCharactersAsync();
        Assert.NotNull(triggers.TriggerCharacters);
        Assert.Empty(triggers.TriggerCharacters);

        var completions = await session.Rpc.Completions.RequestAsync("Use @", offset: 5);
        Assert.NotNull(completions.Items);
        Assert.Empty(completions.Items);
    }

    [Fact]
    public async Task Should_Report_Visibility_As_Unsynced_For_Local_Session()
    {
        await using var session = await CreateSessionAsync();

        var initial = await session.Rpc.Visibility.GetAsync();
        Assert.False(initial.Synced);
        Assert.Null(initial.Status);
        Assert.Null(initial.ShareUrl);

        var set = await session.Rpc.Visibility.SetAsync(SessionVisibilityStatus.Repo);
        Assert.False(set.Synced);
        Assert.Null(set.Status);
        Assert.Null(set.ShareUrl);
    }

    [Fact]
    public async Task Should_Get_And_Set_AllowAll_Permissions()
    {
        await using var session = await CreateSessionAsync();

        try
        {
            var initial = await session.Rpc.Permissions.GetAllowAllAsync();
            Assert.False(initial.Enabled, "Allow-all should be disabled on a fresh session.");

            var enable = await session.Rpc.Permissions.SetAllowAllAsync(enabled: true);
            Assert.True(enable.Success);
            Assert.True(enable.Enabled);
            Assert.True((await session.Rpc.Permissions.GetAllowAllAsync()).Enabled);

            var disable = await session.Rpc.Permissions.SetAllowAllAsync(enabled: false);
            Assert.True(disable.Success);
            Assert.False(disable.Enabled);
            Assert.False((await session.Rpc.Permissions.GetAllowAllAsync()).Enabled);
        }
        finally
        {
            await session.Rpc.Permissions.SetAllowAllAsync(enabled: false);
        }
    }

    [Fact]
    public async Task Should_Read_Empty_Sql_Todos_For_Fresh_Session()
    {
        await using var session = await CreateSessionAsync();

        var result = await session.Rpc.Plan.ReadSqlTodosAsync();

        // A fresh session has never written to its SQL todos table, so the query returns an empty
        // (but non-null) row set rather than failing.
        Assert.NotNull(result.Rows);
        Assert.Empty(result.Rows);
    }

    [Fact]
    public async Task Should_Get_Telemetry_Engagement_Id()
    {
        await using var session = await CreateSessionAsync();

        var result = await session.Rpc.Telemetry.GetEngagementIdAsync();

        // The engagement id is optional (null until telemetry assigns one), but the call must
        // round-trip without error and return a result object.
        Assert.NotNull(result);
    }

    [Fact]
    public async Task Should_Get_Current_Tool_Metadata_After_Initialization()
    {
        await using var session = await CreateSessionAsync();

        // getCurrentMetadata returns the tool snapshot captured for the most recent turn; it is null
        // until the session has processed a turn. Drive one real turn so the runtime computes and
        // records the current tool metadata.
        var answer = await session.SendAndWaitAsync(new MessageOptions { Prompt = "What is 2+2?" });
        Assert.NotNull(answer);

        var result = await session.Rpc.Tools.GetCurrentMetadataAsync();

        Assert.NotNull(result.Tools);
        Assert.NotEmpty(result.Tools!);
        Assert.All(result.Tools!, tool =>
        {
            Assert.False(string.IsNullOrWhiteSpace(tool.Name));
            Assert.NotNull(tool.Description);
        });
    }

    [Fact]
    public async Task Should_Get_Context_Attribution_And_Heaviest_Messages_After_Turn()
    {
        await using var session = await CreateSessionAsync();

        var answer = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Say CONTEXT_METADATA_OK exactly.",
        });
        Assert.Contains("CONTEXT_METADATA_OK", answer?.Data.Content ?? string.Empty, StringComparison.Ordinal);

        var attribution = await session.Rpc.Metadata.GetContextAttributionAsync();
        var contextAttribution = Assert.IsType<MetadataContextAttributionResultContextAttribution>(
            attribution.ContextAttribution);
        Assert.True(contextAttribution.TotalTokens > 0);
        Assert.True(contextAttribution.Compactions.Count >= 0);
        Assert.NotEmpty(contextAttribution.Entries);
        Assert.All(contextAttribution.Entries, entry =>
        {
            Assert.False(string.IsNullOrWhiteSpace(entry.Id));
            Assert.False(string.IsNullOrWhiteSpace(entry.Kind));
            Assert.False(string.IsNullOrWhiteSpace(entry.Label));
            Assert.True(entry.Tokens >= 0);
            if (entry.Attributes is not null)
            {
                Assert.All(entry.Attributes, attribute => Assert.False(string.IsNullOrWhiteSpace(attribute.Key)));
            }
        });

        var heaviest = await session.Rpc.Metadata.GetContextHeaviestMessagesAsync(limit: 2);
        Assert.True(heaviest.TotalTokens > 0);
        Assert.NotNull(heaviest.Messages);
        Assert.True(heaviest.Messages.Count <= 2);
        Assert.All(heaviest.Messages, message =>
        {
            Assert.False(string.IsNullOrWhiteSpace(message.Id));
            Assert.False(string.IsNullOrWhiteSpace(message.Label));
            Assert.False(string.IsNullOrWhiteSpace(message.Role));
            Assert.True(message.Tokens > 0);
        });
    }

    [Fact]
    public async Task Should_Update_And_Clear_Live_Subagent_Settings()
    {
        await using var session = await CreateSessionAsync();

        var update = await session.Rpc.Tools.UpdateSubagentSettingsAsync(new UpdateSubagentSettingsRequestSubagents
        {
            Agents = new Dictionary<string, SubagentSettingsEntry>
            {
                ["general-purpose"] = new()
                {
                    Model = "claude-sonnet-4.5",
                    EffortLevel = "high",
                    ContextTier = SubagentSettingsEntryContextTier.Default,
                },
            },
            DisabledSubagents = ["explore"],
            MaxConcurrency = 2,
            MaxDepth = 1,
        });
        Assert.NotNull(update);

        var clear = await session.Rpc.Tools.UpdateSubagentSettingsAsync();
        Assert.NotNull(clear);
    }

    [Fact]
    public async Task Should_Reload_Session_Plugins()
    {
        await using var session = await CreateSessionAsync();

        // Reloading refreshes the session's plugin set; with no plugins configured it is a no-op
        // that must still complete successfully and leave the plugin list queryable.
        await session.Rpc.Plugins.ReloadAsync();

        var plugins = await session.Rpc.Plugins.ListAsync();
        Assert.NotNull(plugins.Plugins);
        Assert.All(plugins.Plugins, plugin => Assert.False(string.IsNullOrWhiteSpace(plugin.Name)));
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

    private async Task ConfigureAuthenticatedUserAsync(string token)
    {
        await Ctx.SetCopilotUserByTokenAsync(token, new CopilotUserConfig(
            Login: "rpc-session-extras-user",
            CopilotPlan: "individual_pro",
            Endpoints: new CopilotUserEndpoints(Api: Ctx.ProxyUrl, Telemetry: "https://localhost:1/telemetry"),
            AnalyticsTrackingId: "rpc-session-extras-tracking-id"));
    }
}
