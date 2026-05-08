/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Test.Harness;
using GitHub.Copilot.SDK.Rpc;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test.E2E;

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
        Assert.NotNull(before.ModelId);

        var result = await session.Rpc.Model.SwitchToAsync(modelId: "gpt-4.1", reasoningEffort: "high");
        var after = await session.Rpc.Model.GetCurrentAsync();

        Assert.Equal("gpt-4.1", result.ModelId);
        Assert.Equal(before.ModelId, after.ModelId);
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
        Assert.NotEqual(Guid.Empty, workspace.Workspace.Id);
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
    public async Task Should_Fork_Session_With_Persisted_Messages()
    {
        const string sourcePrompt = "Say FORK_SOURCE_ALPHA exactly.";
        const string forkPrompt = "Now say FORK_CHILD_BETA exactly.";

        await using var session = await CreateSessionAsync();

        var initialAnswer = await session.SendAndWaitAsync(new MessageOptions { Prompt = sourcePrompt });
        Assert.Contains("FORK_SOURCE_ALPHA", initialAnswer?.Data.Content ?? string.Empty);

        var sourceConversation = GetConversationMessages(await session.GetMessagesAsync());
        Assert.Contains(sourceConversation, message => message.Role == "user" && message.Content == sourcePrompt);
        Assert.Contains(sourceConversation, message => message.Role == "assistant" && message.Content.Contains("FORK_SOURCE_ALPHA", StringComparison.Ordinal));

        var fork = await Client.Rpc.Sessions.ForkAsync(session.SessionId);
        Assert.False(string.IsNullOrWhiteSpace(fork.SessionId));
        Assert.NotEqual(session.SessionId, fork.SessionId);

        await using var forkedSession = await ResumeSessionAsync(fork.SessionId);
        var forkedConversation = GetConversationMessages(await forkedSession.GetMessagesAsync());
        Assert.Equal(sourceConversation, forkedConversation.Take(sourceConversation.Count));

        var forkAnswer = await forkedSession.SendAndWaitAsync(new MessageOptions { Prompt = forkPrompt });
        Assert.Contains("FORK_CHILD_BETA", forkAnswer?.Data.Content ?? string.Empty);

        var sourceAfterFork = GetConversationMessages(await session.GetMessagesAsync());
        Assert.DoesNotContain(sourceAfterFork, message => message.Content == forkPrompt);

        var forkAfterPrompt = GetConversationMessages(await forkedSession.GetMessagesAsync());
        Assert.Contains(forkAfterPrompt, message => message.Role == "user" && message.Content == forkPrompt);
        Assert.Contains(forkAfterPrompt, message => message.Role == "assistant" && message.Content.Contains("FORK_CHILD_BETA", StringComparison.Ordinal));
    }

    [Fact]
    public async Task Should_Report_Error_When_Forking_Session_Without_Persisted_Events()
    {
        await using var session = await CreateSessionAsync();

        var ex = await Assert.ThrowsAnyAsync<Exception>(() => Client.Rpc.Sessions.ForkAsync(session.SessionId));

        Assert.Contains("not found or has no persisted events", ex.ToString(), StringComparison.OrdinalIgnoreCase);
        Assert.DoesNotContain("Unhandled method sessions.fork", ex.ToString(), StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task Should_Fork_Session_To_Event_Id_Excluding_Boundary_Event()
    {
        const string firstPrompt = "Say FORK_BOUNDARY_FIRST exactly.";
        const string secondPrompt = "Say FORK_BOUNDARY_SECOND exactly.";

        await using var session = await CreateSessionAsync();
        await session.SendAndWaitAsync(new MessageOptions { Prompt = firstPrompt });
        await session.SendAndWaitAsync(new MessageOptions { Prompt = secondPrompt });

        var sourceEvents = await session.GetMessagesAsync();
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
        var forkedEvents = await forkedSession.GetMessagesAsync();
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
        Assert.True(metrics.SessionStartTime > 0);
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

        await session.SendAndWaitAsync(new MessageOptions { Prompt = "What is 2+2?" });

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
