/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class RpcSessionStateE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rpc_session_state", output)
{
    private static async Task<Exception> AssertImplementedFailureAsync(Func<Task> action, string method)
    {
        var ex = await Assert.ThrowsAnyAsync<Exception>(action);
        Assert.DoesNotContain($"Unhandled method {method}", ex.ToString(), StringComparison.OrdinalIgnoreCase);
        return ex;
    }

    [Fact]
    public async Task Should_Call_Session_Rpc_Model_GetCurrent()
    {
        await using var session = await CreateSessionAsync(new SessionConfig { Model = "claude-sonnet-4.5" });

        var result = await session.Rpc.Model.GetCurrentAsync();

        Assert.NotNull(result.ModelId);
        Assert.NotEmpty(result.ModelId);
        // Strengthen: verify the configured model is actually in effect, not just any model
        Assert.Equal("claude-sonnet-4.5", result.ModelId);
    }

    [Fact]
    public async Task Should_Call_Session_Rpc_Model_SwitchTo()
    {
        await using var session = await CreateSessionAsync(new SessionConfig { Model = "claude-sonnet-4.5" });

        var before = await session.Rpc.Model.GetCurrentAsync();
        Assert.Equal("claude-sonnet-4.5", before.ModelId);

        var result = await session.Rpc.Model.SwitchToAsync(modelId: "gpt-5.4", reasoningEffort: "high");
        Assert.Equal("gpt-5.4", result.ModelId);

        var after = await session.Rpc.Model.GetCurrentAsync();
        Assert.Equal("gpt-5.4", after.ModelId);
    }

    [Fact]
    public async Task Should_Get_And_Set_Session_Mode()
    {
        await using var session = await CreateSessionAsync();

        var initial = await session.Rpc.Mode.GetAsync();
        Assert.Equal(SessionMode.Interactive, initial);

        await session.Rpc.Mode.SetAsync(SessionMode.Plan);
        Assert.Equal(SessionMode.Plan, await session.Rpc.Mode.GetAsync());

        await session.Rpc.Mode.SetAsync(SessionMode.Interactive);
        Assert.Equal(SessionMode.Interactive, await session.Rpc.Mode.GetAsync());
    }

    [Fact]
    public async Task Should_Shutdown_Session_With_Routine_Type()
    {
        await using var session = await CreateSessionAsync();

        var shutdownTask = TestHelper.GetNextEventOfTypeAsync<SessionShutdownEvent>(
            session,
            evt => evt.Data.ShutdownType == ShutdownType.Routine,
            TimeSpan.FromSeconds(15),
            timeoutDescription: "session.shutdown event after shutdown RPC");

        await session.Rpc.ShutdownAsync(ShutdownType.Routine, reason: "SDK E2E shutdown coverage");

        var shutdown = await shutdownTask;
        Assert.Equal(ShutdownType.Routine, shutdown.Data.ShutdownType);
    }

    [Theory]
    [InlineData("interactive")]
    [InlineData("plan")]
    [InlineData("autopilot")]
    public async Task Should_Set_And_Get_Each_Session_Mode_Value(string modeValue)
    {
        await using var session = await CreateSessionAsync();
        var mode = new SessionMode(modeValue);

        await session.Rpc.Mode.SetAsync(mode);
        Assert.Equal(mode, await session.Rpc.Mode.GetAsync());
    }

    [Fact]
    public async Task Should_Read_Update_And_Delete_Plan()
    {
        await using var session = await CreateSessionAsync();

        var initial = await session.Rpc.Plan.ReadAsync();
        Assert.False(initial.Exists);
        Assert.Null(initial.Content);

        var planContent = "# Test Plan\n\n- Step 1\n- Step 2";
        await session.Rpc.Plan.UpdateAsync(planContent);

        var afterUpdate = await session.Rpc.Plan.ReadAsync();
        Assert.True(afterUpdate.Exists);
        Assert.Equal(planContent, afterUpdate.Content);

        await session.Rpc.Plan.DeleteAsync();

        var afterDelete = await session.Rpc.Plan.ReadAsync();
        Assert.False(afterDelete.Exists);
        Assert.Null(afterDelete.Content);
    }

    [Fact]
    public async Task Should_Call_Workspace_File_Rpc_Methods()
    {
        await using var session = await CreateSessionAsync();

        var initial = await session.Rpc.Workspaces.ListFilesAsync();
        Assert.NotNull(initial.Files);

        await session.Rpc.Workspaces.CreateFileAsync("test.txt", "Hello, workspace!");

        var afterCreate = await session.Rpc.Workspaces.ListFilesAsync();
        Assert.Contains("test.txt", afterCreate.Files);

        var file = await session.Rpc.Workspaces.ReadFileAsync("test.txt");
        Assert.Equal("Hello, workspace!", file.Content);

        var workspace = await session.Rpc.Workspaces.GetWorkspaceAsync();
        Assert.NotNull(workspace.Workspace);
        Assert.NotEmpty(workspace.Workspace.Id);
    }

    [Theory]
    [InlineData("../escaped.txt")]
    [InlineData("../../escaped.txt")]
    [InlineData("nested/../../../escaped.txt")]
    public async Task Should_Reject_Workspace_File_Path_Traversal(string path)
    {
        await using var session = await CreateSessionAsync();

        // The runtime's resolveWorkspacePath enforces that resolved paths must remain
        // inside the workspace files directory. Path traversal attempts must throw,
        // not silently succeed.
        var ex = await Assert.ThrowsAnyAsync<Exception>(
            () => session.Rpc.Workspaces.CreateFileAsync(path, "should not land outside workspace"));
        Assert.Contains("workspace files directory", ex.ToString(), StringComparison.OrdinalIgnoreCase);

        var readEx = await Assert.ThrowsAnyAsync<Exception>(
            () => session.Rpc.Workspaces.ReadFileAsync(path));
        Assert.Contains("workspace files directory", readEx.ToString(), StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task Should_Create_Workspace_File_With_Nested_Path_Auto_Creating_Dirs()
    {
        await using var session = await CreateSessionAsync();

        // workspaceManager.writeWorkspaceFile mkdirs parent dirs recursively.
        var nestedPath = $"nested-{Guid.NewGuid():N}/subdir/file.txt";
        await session.Rpc.Workspaces.CreateFileAsync(nestedPath, "nested content");

        var read = await session.Rpc.Workspaces.ReadFileAsync(nestedPath);
        Assert.Equal("nested content", read.Content);

        var listed = await session.Rpc.Workspaces.ListFilesAsync();
        Assert.Contains(listed.Files, f => f.EndsWith("file.txt", StringComparison.Ordinal));
    }

    [Fact]
    public async Task Should_Report_Error_Reading_Nonexistent_Workspace_File()
    {
        await using var session = await CreateSessionAsync();

        await Assert.ThrowsAnyAsync<Exception>(
            () => session.Rpc.Workspaces.ReadFileAsync($"never-exists-{Guid.NewGuid():N}.txt"));
    }

    [Fact]
    public async Task Should_Update_Existing_Workspace_File_With_Update_Operation()
    {
        await using var session = await CreateSessionAsync();
        var path = $"reused-{Guid.NewGuid():N}.txt";

        await session.Rpc.Workspaces.CreateFileAsync(path, "v1");

        var updateTask = TestHelper.GetNextEventOfTypeAsync<SessionWorkspaceFileChangedEvent>(
            session,
            evt => string.Equals(evt.Data.Path, path, StringComparison.Ordinal)
                && evt.Data.Operation == WorkspaceFileChangedOperation.Update,
            TimeSpan.FromSeconds(15),
            timeoutDescription: $"workspace_file_changed Update event for '{path}'");

        await session.Rpc.Workspaces.CreateFileAsync(path, "v2");

        var evt = await updateTask;
        Assert.Equal(WorkspaceFileChangedOperation.Update, evt.Data.Operation);
        Assert.Equal("v2", (await session.Rpc.Workspaces.ReadFileAsync(path)).Content);
    }

    [Theory]
    [InlineData("")]
    [InlineData("   ")]
    [InlineData("\t\n  \r")]
    public async Task Should_Reject_Empty_Or_Whitespace_Session_Name(string emptyOrWhitespace)
    {
        await using var session = await CreateSessionAsync();

        // workspaceManager.renameSession trims and rejects empty/whitespace-only names
        // with "Session name cannot be empty".
        var ex = await Assert.ThrowsAnyAsync<Exception>(() => session.Rpc.Name.SetAsync(emptyOrWhitespace));
        Assert.Contains("empty", ex.ToString(), StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task Should_Emit_Title_Changed_Event_Each_Time_Name_Set_Is_Called()
    {
        await using var session = await CreateSessionAsync();
        var titleA = $"Title-A-{Guid.NewGuid():N}";
        var titleB = $"Title-B-{Guid.NewGuid():N}";

        // session.title_changed is ephemeral. Subscribe before invoking.
        var firstTask = TestHelper.GetNextEventOfTypeAsync<SessionTitleChangedEvent>(
            session,
            evt => string.Equals(evt.Data.Title, titleA, StringComparison.Ordinal),
            TimeSpan.FromSeconds(15),
            timeoutDescription: "first title_changed event");
        await session.Rpc.Name.SetAsync(titleA);
        await firstTask;

        // Setting a different name MUST emit another event (renameSession does not
        // suppress duplicates, and the second value is observably different anyway).
        var secondTask = TestHelper.GetNextEventOfTypeAsync<SessionTitleChangedEvent>(
            session,
            evt => string.Equals(evt.Data.Title, titleB, StringComparison.Ordinal),
            TimeSpan.FromSeconds(15),
            timeoutDescription: "second title_changed event");
        await session.Rpc.Name.SetAsync(titleB);
        var second = await secondTask;
        Assert.Equal(titleB, second.Data.Title);
    }

    [Fact]
    public async Task Should_Get_And_Set_Session_Metadata()
    {
        await using var session = await CreateSessionAsync();

        await session.Rpc.Name.SetAsync("SDK test session");
        var name = await session.Rpc.Name.GetAsync();
        Assert.Equal("SDK test session", name.Name);

        var sources = await session.Rpc.Instructions.GetSourcesAsync();
        Assert.NotNull(sources.Sources);
    }

    [Fact]
    public async Task Should_Call_Metadata_Snapshot_SetWorkingDirectory_And_RecordContextChange()
    {
        var firstDirectory = CreateUniqueDirectory();
        var secondDirectory = CreateUniqueDirectory();
        var contextDirectory = CreateUniqueDirectory();
        var branch = $"rpc-context-{Guid.NewGuid():N}";
        await using var session = await CreateSessionAsync(new SessionConfig
        {
            Model = "claude-sonnet-4.5",
            WorkingDirectory = firstDirectory,
        });

        var initialSnapshot = await session.Rpc.Metadata.SnapshotAsync();
        Assert.Equal(session.SessionId, initialSnapshot.SessionId);
        Assert.Equal(MetadataSnapshotCurrentMode.Interactive, initialSnapshot.CurrentMode);
        Assert.Equal("claude-sonnet-4.5", initialSnapshot.SelectedModel);
        Assert.False(initialSnapshot.IsRemote);
        Assert.False(initialSnapshot.AlreadyInUse);
        Assert.NotEqual(default, initialSnapshot.StartTime);
        Assert.NotEqual(default, initialSnapshot.ModifiedTime);
        Assert.True(PathEquals(firstDirectory, initialSnapshot.WorkingDirectory),
            $"Expected working directory '{firstDirectory}', actual '{initialSnapshot.WorkingDirectory}'.");
        Assert.NotNull(initialSnapshot.Workspace);
        Assert.Equal(session.SessionId, initialSnapshot.Workspace.Id);
        Assert.False(string.IsNullOrWhiteSpace(initialSnapshot.WorkspacePath));

        var setWorkingDirectory = await session.Rpc.Metadata.SetWorkingDirectoryAsync(secondDirectory);
        Assert.True(PathEquals(secondDirectory, setWorkingDirectory.WorkingDirectory),
            $"Expected setWorkingDirectory result '{secondDirectory}', actual '{setWorkingDirectory.WorkingDirectory}'.");

        SessionMetadataSnapshot? updatedSnapshot = null;
        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                updatedSnapshot = await session.Rpc.Metadata.SnapshotAsync();
                return PathEquals(secondDirectory, updatedSnapshot.WorkingDirectory);
            },
            timeout: TimeSpan.FromSeconds(15),
            timeoutMessage: "Timed out waiting for metadata snapshot to reflect setWorkingDirectory.");
        Assert.NotNull(updatedSnapshot);
        Assert.True(PathEquals(secondDirectory, updatedSnapshot!.WorkingDirectory));

        var contextChangedTask = TestHelper.GetNextEventOfTypeAsync<SessionContextChangedEvent>(
            session,
            evt => string.Equals(evt.Data.Branch, branch, StringComparison.Ordinal),
            TimeSpan.FromSeconds(15),
            timeoutDescription: "session.context_changed event after metadata.recordContextChange");

        var context = new SessionWorkingDirectoryContext
        {
            Cwd = contextDirectory,
            GitRoot = firstDirectory,
            Branch = branch,
            Repository = "github/copilot-sdk-e2e",
            RepositoryHost = "github.com",
            HostType = SessionWorkingDirectoryContextHostType.GitHub,
            BaseCommit = "0000000000000000000000000000000000000000",
            HeadCommit = "1111111111111111111111111111111111111111",
        };

        var recordResult = await session.Rpc.Metadata.RecordContextChangeAsync(context);
        Assert.NotNull(recordResult);

        var contextChanged = await contextChangedTask;
        Assert.True(PathEquals(contextDirectory, contextChanged.Data.Cwd),
            $"Expected context cwd '{contextDirectory}', actual '{contextChanged.Data.Cwd}'.");
        Assert.True(PathEquals(firstDirectory, contextChanged.Data.GitRoot),
            $"Expected context git root '{firstDirectory}', actual '{contextChanged.Data.GitRoot}'.");
        Assert.Equal(branch, contextChanged.Data.Branch);
        Assert.Equal("github/copilot-sdk-e2e", contextChanged.Data.Repository);
        Assert.Equal("github.com", contextChanged.Data.RepositoryHost);
        Assert.True(contextChanged.Data.HostType.HasValue);
        var hostType = contextChanged.Data.HostType.Value;
        Assert.Equal("github", hostType.Value);
        Assert.Equal(context.BaseCommit, contextChanged.Data.BaseCommit);
        Assert.Equal(context.HeadCommit, contextChanged.Data.HeadCommit);
    }

    [Fact]
    public async Task Should_Update_Options_And_Initialize_Session_Services()
    {
        var initialDirectory = CreateUniqueDirectory();
        var optionsDirectory = CreateUniqueDirectory();
        var featureName = $"rpc-session-state-{Guid.NewGuid():N}";
        await using var session = await CreateSessionAsync(new SessionConfig
        {
            WorkingDirectory = initialDirectory,
        });

        var update = await session.Rpc.Options.UpdateAsync(
            clientName: "dotnet-sdk-rpc-session-state-e2e",
            lspClientName: "dotnet-sdk-rpc-session-state-lsp",
            integrationId: $"dotnet-sdk-{Guid.NewGuid():N}",
            featureFlags: new Dictionary<string, bool> { [featureName] = true },
            workingDirectory: optionsDirectory,
            coauthorEnabled: false,
            enableStreaming: false,
            askUserDisabled: true);
        Assert.True(update.Success);

        await TestHelper.WaitForConditionAsync(
            async () => PathEquals(optionsDirectory, (await session.Rpc.Metadata.SnapshotAsync()).WorkingDirectory),
            timeout: TimeSpan.FromSeconds(15),
            timeoutMessage: "Timed out waiting for options.update workingDirectory to reach metadata snapshot.");

        await session.Rpc.Lsp.InitializeAsync(
            workingDirectory: optionsDirectory,
            gitRoot: initialDirectory,
            force: true);

        await session.Rpc.Telemetry.SetFeatureOverridesAsync(new Dictionary<string, string>
        {
            ["rpc_session_state_feature"] = featureName,
            ["rpc_session_state_value"] = "enabled",
        });

        var tools = await session.Rpc.Tools.InitializeAndValidateAsync();
        Assert.NotNull(tools);

        var snapshot = await session.Rpc.Metadata.SnapshotAsync();
        Assert.True(PathEquals(optionsDirectory, snapshot.WorkingDirectory),
            $"Expected options working directory '{optionsDirectory}', actual '{snapshot.WorkingDirectory}'.");
    }

    [Fact]
    public async Task Should_Set_ReasoningEffort_And_Auto_Name()
    {
        await using var session = await CreateSessionAsync(new SessionConfig
        {
            Model = "claude-sonnet-4.5",
        });

        var reasoning = await session.Rpc.Model.SetReasoningEffortAsync("high");
        Assert.Equal("high", reasoning.ReasoningEffort);

        var currentModel = await session.Rpc.Model.GetCurrentAsync();
        Assert.Equal("claude-sonnet-4.5", currentModel.ModelId);
        Assert.Equal("high", currentModel.ReasoningEffort);

        var autoName = $"Auto Session {Guid.NewGuid():N}";
        var titleChangedTask = TestHelper.GetNextEventOfTypeAsync<SessionTitleChangedEvent>(
            session,
            evt => string.Equals(evt.Data.Title, autoName, StringComparison.Ordinal),
            TimeSpan.FromSeconds(15),
            timeoutDescription: "session.title_changed event after name.setAuto");

        var autoResult = await session.Rpc.Name.SetAutoAsync($"  {autoName}  ");
        Assert.True(autoResult.Applied);
        var titleChanged = await titleChangedTask;
        Assert.Equal(autoName, titleChanged.Data.Title);
        Assert.Equal(autoName, (await session.Rpc.Name.GetAsync()).Name);

        var explicitName = $"Explicit Session {Guid.NewGuid():N}";
        var explicitTitleChangedTask = TestHelper.GetNextEventOfTypeAsync<SessionTitleChangedEvent>(
            session,
            evt => string.Equals(evt.Data.Title, explicitName, StringComparison.Ordinal),
            TimeSpan.FromSeconds(15),
            timeoutDescription: "session.title_changed event after explicit name.set");
        await session.Rpc.Name.SetAsync(explicitName);
        Assert.Equal(explicitName, (await explicitTitleChangedTask).Data.Title);
        var ignoredAutoResult = await session.Rpc.Name.SetAutoAsync($"Ignored {Guid.NewGuid():N}");
        Assert.False(ignoredAutoResult.Applied);
        Assert.Equal(explicitName, (await session.Rpc.Name.GetAsync()).Name);
    }

    [Fact]
    public async Task Should_Set_Auth_Credentials()
    {
        await using var client = Ctx.CreateClient();
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });
        var login = $"sdk-rpc-{Guid.NewGuid():N}";

        var setCredentials = await session.Rpc.Auth.SetCredentialsAsync(new AuthInfoUser
        {
            CopilotUser = new CopilotUserResponse
            {
                AnalyticsTrackingId = "rpc-session-state-tracking-id",
                ChatEnabled = true,
                CopilotPlan = "individual_pro",
                Endpoints = new CopilotUserResponseEndpoints
                {
                    Api = Ctx.ProxyUrl,
                    Telemetry = "https://localhost:1/telemetry",
                },
                Login = login,
            },
            Host = "https://github.com",
            Login = login,
        });
        Assert.True(setCredentials.Success);

        var status = await session.Rpc.Auth.GetStatusAsync();
        Assert.True(status.IsAuthenticated);
        Assert.Equal(AuthInfoType.User, status.AuthType);
        Assert.Equal("https://github.com", status.Host);
        Assert.Equal(login, status.Login);
    }

    [Fact]
    public async Task Should_Fork_Session_With_Persisted_Messages()
    {
        const string sourcePrompt = "Say FORK_SOURCE_ALPHA exactly.";
        const string forkPrompt = "Now say FORK_CHILD_BETA exactly.";

        await using var session = await CreateSessionAsync();

        var initialAnswer = await session.SendAndWaitAsync(new MessageOptions { Prompt = sourcePrompt });
        Assert.Contains("FORK_SOURCE_ALPHA", initialAnswer?.Data.Content ?? string.Empty);

        var sourceConversation = GetConversationMessages(await session.GetEventsAsync());
        Assert.Contains(sourceConversation, message => message.Role == "user" && message.Content == sourcePrompt);
        Assert.Contains(sourceConversation, message => message.Role == "assistant" && message.Content.Contains("FORK_SOURCE_ALPHA", StringComparison.Ordinal));

        var fork = await Client.Rpc.Sessions.ForkAsync(session.SessionId);
        Assert.False(string.IsNullOrWhiteSpace(fork.SessionId));
        Assert.NotEqual(session.SessionId, fork.SessionId);

        await using var forkedSession = await ResumeSessionAsync(fork.SessionId);
        var forkedConversation = GetConversationMessages(await forkedSession.GetEventsAsync());
        Assert.Equal(sourceConversation, forkedConversation.Take(sourceConversation.Count));

        var forkAnswer = await forkedSession.SendAndWaitAsync(new MessageOptions { Prompt = forkPrompt });
        Assert.Contains("FORK_CHILD_BETA", forkAnswer?.Data.Content ?? string.Empty);

        var sourceAfterFork = GetConversationMessages(await session.GetEventsAsync());
        Assert.DoesNotContain(sourceAfterFork, message => message.Content == forkPrompt);

        var forkAfterPrompt = GetConversationMessages(await forkedSession.GetEventsAsync());
        Assert.Contains(forkAfterPrompt, message => message.Role == "user" && message.Content == forkPrompt);
        Assert.Contains(forkAfterPrompt, message => message.Role == "assistant" && message.Content.Contains("FORK_CHILD_BETA", StringComparison.Ordinal));
    }

    [Fact]
    public async Task Should_Handle_Forking_Session_Without_Persisted_Events()
    {
        await using var session = await CreateSessionAsync();

        SessionsForkResult? fork = null;
        var ex = await Record.ExceptionAsync(async () =>
        {
            fork = await Client.Rpc.Sessions.ForkAsync(session.SessionId);
        });

        if (ex is not null)
        {
            Assert.Contains("not found or has no persisted events", ex.ToString(), StringComparison.OrdinalIgnoreCase);
            Assert.DoesNotContain("Unhandled method sessions.fork", ex.ToString(), StringComparison.OrdinalIgnoreCase);
            return;
        }

        var forkSessionId = Assert.IsType<SessionsForkResult>(fork).SessionId;
        Assert.False(string.IsNullOrWhiteSpace(forkSessionId));
        Assert.NotEqual(session.SessionId, forkSessionId);

        await using var forkedSession = await ResumeSessionAsync(forkSessionId);
        Assert.Empty(GetConversationMessages(await forkedSession.GetEventsAsync()));
    }

    [Fact]
    public async Task Should_Fork_Session_To_Event_Id_Excluding_Boundary_Event()
    {
        const string firstPrompt = "Say FORK_BOUNDARY_FIRST exactly.";
        const string secondPrompt = "Say FORK_BOUNDARY_SECOND exactly.";

        await using var session = await CreateSessionAsync();
        await session.SendAndWaitAsync(new MessageOptions { Prompt = firstPrompt });
        await session.SendAndWaitAsync(new MessageOptions { Prompt = secondPrompt });

        var sourceEvents = await session.GetEventsAsync();
        var secondUserEvent = sourceEvents
            .OfType<UserMessageEvent>()
            .FirstOrDefault(e => string.Equals(e.Data.Content, secondPrompt, StringComparison.Ordinal))
            ?? throw new InvalidOperationException("Expected the second user.message in persisted history");
        var boundaryEventId = secondUserEvent.Id.ToString();

        // Runtime semantics (localSessionManager.forkSession): toEventId is exclusive,
        // so the boundary event is NOT included in the forked session.
        var fork = await Client.Rpc.Sessions.ForkAsync(session.SessionId, boundaryEventId);
        Assert.False(string.IsNullOrWhiteSpace(fork.SessionId));
        Assert.NotEqual(session.SessionId, fork.SessionId);

        await using var forkedSession = await ResumeSessionAsync(fork.SessionId);
        var forkedEvents = await forkedSession.GetEventsAsync();
        Assert.DoesNotContain(forkedEvents, e => e.Id == secondUserEvent.Id);

        var forkedConversation = GetConversationMessages(forkedEvents);
        Assert.Contains(forkedConversation, m => m.Role == "user" && m.Content == firstPrompt);
        Assert.DoesNotContain(forkedConversation, m => m.Role == "user" && m.Content == secondPrompt);
    }

    [Fact]
    public async Task Should_Report_Error_When_Forking_Session_To_Unknown_Event_Id()
    {
        const string sourcePrompt = "Say FORK_UNKNOWN_EVENT_OK exactly.";

        await using var session = await CreateSessionAsync();
        await session.SendAndWaitAsync(new MessageOptions { Prompt = sourcePrompt });

        var bogusEventId = Guid.NewGuid().ToString();

        var ex = await Assert.ThrowsAnyAsync<Exception>(
            () => Client.Rpc.Sessions.ForkAsync(session.SessionId, bogusEventId));

        Assert.Contains($"Event {bogusEventId} not found", ex.ToString(), StringComparison.OrdinalIgnoreCase);
        Assert.DoesNotContain("Unhandled method sessions.fork", ex.ToString(), StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task Should_Call_Session_Usage_And_Permission_Rpcs()
    {
        await using var session = await CreateSessionAsync();

        var metrics = await session.Rpc.Usage.GetMetricsAsync();
        Assert.NotEqual(default, metrics.SessionStartTime);
        Assert.True(metrics.TotalNanoAiu is null or >= 0);
        if (metrics.TokenDetails is not null)
        {
            Assert.All(metrics.TokenDetails.Values, detail => Assert.True(detail.TokenCount >= 0));
        }

        Assert.All(
            metrics.ModelMetrics.Values,
            modelMetric =>
            {
                Assert.True(modelMetric.TotalNanoAiu is null or >= 0);
                if (modelMetric.TokenDetails is not null)
                {
                    Assert.All(modelMetric.TokenDetails.Values, detail => Assert.True(detail.TokenCount >= 0));
                }
            });

        try
        {
            var approveAll = await session.Rpc.Permissions.SetApproveAllAsync(true);
            Assert.True(approveAll.Success);

            var reset = await session.Rpc.Permissions.ResetSessionApprovalsAsync();
            Assert.True(reset.Success);
        }
        finally
        {
            await session.Rpc.Permissions.SetApproveAllAsync(false);
        }
    }

    [Fact]
    public async Task Should_Report_Implemented_Errors_For_Unsupported_Session_Rpc_Paths()
    {
        await using var session = await CreateSessionAsync();

        await AssertImplementedFailureAsync(
            () => session.Rpc.History.TruncateAsync("missing-event"),
            "session.history.truncate");

        await AssertImplementedFailureAsync(
            () => session.Rpc.Mcp.Oauth.LoginAsync("missing-server"),
            "session.mcp.oauth.login");
    }

    [Fact]
    public async Task Should_Compact_Session_History_After_Messages()
    {
        await using var session = await CreateSessionAsync();

        Assert.False((await session.Rpc.Metadata.IsProcessingAsync()).Processing);

        var answer = await session.SendAndWaitAsync(new MessageOptions { Prompt = "What is 2+2?" });
        Assert.NotNull(answer);
        Assert.Contains("4", answer!.Data.Content ?? string.Empty, StringComparison.Ordinal);
        Assert.False((await session.Rpc.Metadata.IsProcessingAsync()).Processing);

        var contextInfo = await session.Rpc.Metadata.ContextInfoAsync(
            promptTokenLimit: 128_000,
            outputTokenLimit: 4_096,
            selectedModel: "claude-sonnet-4.5");
        var context = Assert.IsType<MetadataContextInfoResultContextInfo>(contextInfo.ContextInfo);
        Assert.Equal("claude-sonnet-4.5", context.ModelName);
        Assert.Equal(128_000, context.PromptTokenLimit);
        Assert.True(context.Limit >= context.PromptTokenLimit);
        Assert.True(context.TotalTokens > 0);
        Assert.True(context.SystemTokens > 0);
        Assert.True(context.ConversationTokens > 0);
        Assert.True(context.ToolDefinitionsTokens >= 0);
        Assert.Equal(
            context.SystemTokens + context.ConversationTokens + context.ToolDefinitionsTokens,
            context.TotalTokens);

        var recomputed = await session.Rpc.Metadata.RecomputeContextTokensAsync("claude-sonnet-4.5");
        Assert.True(recomputed.SystemTokenCount > 0);
        Assert.True(recomputed.MessagesTokenCount > 0);
        Assert.Equal(recomputed.SystemTokenCount + recomputed.MessagesTokenCount, recomputed.TotalTokens);

        var result = await session.Rpc.History.CompactAsync();

        Assert.NotNull(result);
        Assert.True(result.Success, "Expected History.CompactAsync to report Success=true");
        Assert.True(result.MessagesRemoved >= 0, "MessagesRemoved must be non-negative");
        // TODO: once copilot-agent-runtime PR #7285 ("Runtime: Fix compact history no-op
        // accounting") merges and is rolled into the @github/copilot version pinned by
        // nodejs/package-lock.json, re-tighten this to `result.TokensRemoved >= 0`. Until
        // then `tokensRemoved = preCompactionTokens - postCompactionTokens` can legitimately
        // be negative when the LLM-generated summary is more verbose than the messages it
        // replaced (the SDK schema declares min(0) but the runtime does not enforce it).

        if (result.ContextWindow is { } ctx)
        {
            Assert.True(ctx.MessagesLength >= 0, "ContextWindow.MessagesLength must be non-negative");
            Assert.True(ctx.CurrentTokens >= 0, "ContextWindow.CurrentTokens must be non-negative");
            if (ctx.ConversationTokens is long convo)
            {
                Assert.True(convo >= 0, "ContextWindow.ConversationTokens must be non-negative when present");
                Assert.True(convo <= ctx.CurrentTokens, "ConversationTokens must not exceed CurrentTokens");
            }
        }

        // Session must still be usable after compaction.
        var name = await session.Rpc.Name.GetAsync();
        Assert.NotNull(name);
    }

    private string CreateUniqueDirectory()
    {
        var path = Path.GetFullPath(Path.Join(Ctx.WorkDir, $"rpc-session-state-{Guid.NewGuid():N}"));
        Directory.CreateDirectory(path);
        return path;
    }

    private static bool PathEquals(string? expected, string? actual)
    {
        var comparison = OperatingSystem.IsWindows() ? StringComparison.OrdinalIgnoreCase : StringComparison.Ordinal;
        return string.Equals(NormalizePath(expected), NormalizePath(actual), comparison);
    }

    private static string? NormalizePath(string? path)
        => path is null ? null : Path.GetFullPath(path).TrimEnd(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar);

    private static List<(string Role, string Content)> GetConversationMessages(IEnumerable<SessionEvent> events)
    {
        var messages = new List<(string Role, string Content)>();
        foreach (var evt in events)
        {
            switch (evt)
            {
                case UserMessageEvent user:
                    messages.Add(("user", user.Data.Content));
                    break;
                case AssistantMessageEvent assistant:
                    messages.Add(("assistant", assistant.Data.Content));
                    break;
            }
        }

        return messages;
    }
}
