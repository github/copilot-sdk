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
        var session = await CreateSessionAsync(new SessionConfig { Model = "claude-sonnet-4.5" });

        var result = await session.Rpc.Model.GetCurrentAsync();

        Assert.NotNull(result.ModelId);
        Assert.NotEmpty(result.ModelId);
        // Strengthen: verify the configured model is actually in effect, not just any model
        Assert.Equal("claude-sonnet-4.5", result.ModelId);
    }

    [Fact]
    public async Task Should_Call_Session_Rpc_Model_SwitchTo()
    {
        var session = await CreateSessionAsync(new SessionConfig { Model = "claude-sonnet-4.5" });

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
        var session = await CreateSessionAsync();

        var initial = await session.Rpc.Mode.GetAsync();
        Assert.Equal(SessionMode.Interactive, initial);

        await session.Rpc.Mode.SetAsync(SessionMode.Plan);
        Assert.Equal(SessionMode.Plan, await session.Rpc.Mode.GetAsync());

        await session.Rpc.Mode.SetAsync(SessionMode.Interactive);
        Assert.Equal(SessionMode.Interactive, await session.Rpc.Mode.GetAsync());
    }

    [Fact]
    public async Task Should_Read_Update_And_Delete_Plan()
    {
        var session = await CreateSessionAsync();

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
        var session = await CreateSessionAsync();

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

    [Fact]
    public async Task Should_Get_And_Set_Session_Metadata()
    {
        var session = await CreateSessionAsync();

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

        var session = await CreateSessionAsync();

        var initialAnswer = await session.SendAndWaitAsync(new MessageOptions { Prompt = sourcePrompt });
        Assert.Contains("FORK_SOURCE_ALPHA", initialAnswer?.Data.Content ?? string.Empty);

        var sourceConversation = GetConversationMessages(await session.GetMessagesAsync());
        Assert.Contains(sourceConversation, message => message.Role == "user" && message.Content == sourcePrompt);
        Assert.Contains(sourceConversation, message => message.Role == "assistant" && message.Content.Contains("FORK_SOURCE_ALPHA", StringComparison.Ordinal));

        var fork = await Client.Rpc.Sessions.ForkAsync(session.SessionId);
        Assert.False(string.IsNullOrWhiteSpace(fork.SessionId));
        Assert.NotEqual(session.SessionId, fork.SessionId);

        var forkedSession = await ResumeSessionAsync(fork.SessionId);
        var forkedConversation = GetConversationMessages(await forkedSession.GetMessagesAsync());
        Assert.Equal(sourceConversation, forkedConversation.Take(sourceConversation.Count));

        var forkAnswer = await forkedSession.SendAndWaitAsync(new MessageOptions { Prompt = forkPrompt });
        Assert.Contains("FORK_CHILD_BETA", forkAnswer?.Data.Content ?? string.Empty);

        var sourceAfterFork = GetConversationMessages(await session.GetMessagesAsync());
        Assert.DoesNotContain(sourceAfterFork, message => message.Content == forkPrompt);

        var forkAfterPrompt = GetConversationMessages(await forkedSession.GetMessagesAsync());
        Assert.Contains(forkAfterPrompt, message => message.Role == "user" && message.Content == forkPrompt);
        Assert.Contains(forkAfterPrompt, message => message.Role == "assistant" && message.Content.Contains("FORK_CHILD_BETA", StringComparison.Ordinal));

        await forkedSession.DisposeAsync();
    }

    [Fact]
    public async Task Should_Report_Error_When_Forking_Session_Without_Persisted_Events()
    {
        var session = await CreateSessionAsync();

        var ex = await Assert.ThrowsAnyAsync<Exception>(() => Client.Rpc.Sessions.ForkAsync(session.SessionId));

        Assert.Contains("not found or has no persisted events", ex.ToString(), StringComparison.OrdinalIgnoreCase);
        Assert.DoesNotContain("Unhandled method sessions.fork", ex.ToString(), StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task Should_Call_Session_Usage_And_Permission_Rpcs()
    {
        var session = await CreateSessionAsync();

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
        var session = await CreateSessionAsync();

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
        var session = await CreateSessionAsync();

        await session.SendAndWaitAsync(new MessageOptions { Prompt = "What is 2+2?" });

        var result = await session.Rpc.History.CompactAsync();

        Assert.NotNull(result);
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
