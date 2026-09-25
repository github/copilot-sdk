/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using System.Text.Json;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class ScenarioTestingServerControlE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : ScenarioTestingE2ETestBase(fixture, "scenario_testing_server_control", output)
{
    [Theory]
    [InlineData("all")]
    [InlineData("mcp")]
    [InlineData("skills")]
    public async Task Should_Search_Server_Catalog_With_Category_Contract(string category)
    {
        var (cliPath, capturePath) = await ScenarioTestingTestCli.CreateAsync(Ctx);
        await using var client = Ctx.CreateClient(options: CreateFakeCliOptions(cliPath, capturePath));
        await client.StartAsync();

        var kinds = category switch
        {
            "all" => new[] { CatalogCandidateKind.McpServer, CatalogCandidateKind.AiSkill },
            "mcp" => [CatalogCandidateKind.McpServer],
            "skills" => [CatalogCandidateKind.AiSkill],
            _ => throw new ArgumentOutOfRangeException(nameof(category)),
        };
        var capabilities = category switch
        {
            "all" => new[] { "mcp-server-card", "ai-skill-discovery" },
            "mcp" => ["mcp-server-card"],
            "skills" => ["ai-skill-discovery"],
            _ => throw new ArgumentOutOfRangeException(nameof(category)),
        };

        var result = await client.Rpc.Catalog.SearchAsync(
            new CatalogClientContract
            {
                ProtocolVersion = 3,
                RequiredCapabilities = capabilities,
            },
            query: "scenario search",
            limit: 50,
            kinds: kinds);

        var succeeded = Assert.IsType<CatalogSearchResultSucceeded>(result);
        Assert.Empty(succeeded.Candidates);
        Assert.Equal("scenario-search", succeeded.SearchId);
        Assert.False(succeeded.Truncated);
        Assert.Equal(3, succeeded.Negotiated.RuntimeProtocolVersion);
        Assert.Equal(capabilities, succeeded.Negotiated.GrantedCapabilities.Select(capability => capability.Value));

        var request = Assert.Single(
            await ReadRequestsAsync(capturePath, "catalog.search")).GetProperty("params");
        Assert.Equal("scenario search", request.GetProperty("query").GetString());
        Assert.Equal(50, request.GetProperty("limit").GetInt32());
        Assert.Equal(3, request.GetProperty("contract").GetProperty("protocolVersion").GetInt32());
        Assert.Equal(
            capabilities,
            request.GetProperty("contract").GetProperty("requiredCapabilities")
                .EnumerateArray().Select(item => item.GetString()));
        Assert.Equal(
            kinds.Select(kind => kind.Value),
            request.GetProperty("kinds").EnumerateArray().Select(item => item.GetString()));
    }

    [Fact]
    public async Task Should_Observe_Page_And_Cancel_Factory_Run()
    {
        var (cliPath, capturePath) = await ScenarioTestingTestCli.CreateAsync(Ctx);
        await using var client = Ctx.CreateClient(options: CreateFakeCliOptions(cliPath, capturePath));
        await using var session = await Ctx.CreateSessionAsync(client, new SessionConfig());

        var runs = await session.Rpc.Workflow.ListRunsAsync(afterSeq: 3, beforeSeq: 20, limit: 10);
        var summary = Assert.Single(runs.Runs);
        Assert.Equal("workflow-run-1", summary.RunId);
        Assert.Equal("scenario-workflow", summary.WorkflowName);
        Assert.Equal(WorkflowRunStatus.Running, summary.Status);
        Assert.Equal(7, runs.OldestSeq);
        Assert.Equal(7, runs.NewestSeq);
        Assert.False(runs.HasMoreNewer);

        var detail = await session.Rpc.Workflow.GetRunDetailAsync(summary.RunId);
        Assert.Equal(summary.RunId, detail.RunId);
        Assert.Equal(summary.WorkflowName, detail.WorkflowName);
        Assert.Equal(WorkflowRunStatus.Running, detail.Status);
        Assert.Equal(4, detail.Revision);

        var progress = await session.Rpc.Workflow.GetRunProgressAsync(
            summary.RunId,
            phaseId: "verify",
            afterSeq: 5,
            beforeSeq: 20,
            limit: 25);
        var line = Assert.Single(progress.Records);
        Assert.Equal(12, line.Seq);
        Assert.Equal("verify", line.PhaseId);
        Assert.Equal(WorkflowLogLineKind.Log, line.Kind);
        Assert.Equal("Validation complete", line.Text);

        var cancelled = await session.Rpc.Workflow.CancelAsync(summary.RunId);
        Assert.Equal(summary.RunId, cancelled.RunId);
        Assert.Equal(WorkflowRunStatus.Cancelled, cancelled.Status);
        Assert.Equal("cancelled by user", cancelled.Reason);

        var requests = await ScenarioTestingTestCli.ReadRequestsAsync(capturePath);
        var list = Assert.Single(requests, request => GetMethod(request) == "session.workflow.listRuns")
            .GetProperty("params");
        Assert.Equal(3, list.GetProperty("afterSeq").GetInt64());
        Assert.Equal(20, list.GetProperty("beforeSeq").GetInt64());
        Assert.Equal(10, list.GetProperty("limit").GetInt32());

        var progressRequest = Assert.Single(
            requests,
            request => GetMethod(request) == "session.workflow.getRunProgress").GetProperty("params");
        Assert.Equal("workflow-run-1", progressRequest.GetProperty("runId").GetString());
        Assert.Equal("verify", progressRequest.GetProperty("phaseId").GetString());
        Assert.Equal(5, progressRequest.GetProperty("afterSeq").GetInt64());
        Assert.Equal(20, progressRequest.GetProperty("beforeSeq").GetInt64());
        Assert.Equal(25, progressRequest.GetProperty("limit").GetInt32());
    }

    [Theory]
    [InlineData("on", true)]
    [InlineData("export", false)]
    public async Task Should_Read_Autopilot_State_And_Enable_Remote_Mode(
        string remoteMode,
        bool expectedSteerable)
    {
        var (cliPath, capturePath) = await ScenarioTestingTestCli.CreateAsync(Ctx);
        await using var client = Ctx.CreateClient(options: CreateFakeCliOptions(cliPath, capturePath));
        await using var session = await Ctx.CreateSessionAsync(client, new SessionConfig());

        var objective = (await session.Rpc.AutopilotObjective.GetStateAsync()).State;
        Assert.NotNull(objective);
        Assert.Equal(17, objective.Id);
        Assert.Equal("Ship the scenario.", objective.Objective);
        Assert.Equal(AutopilotObjectiveStatus.Active, objective.Status);
        Assert.Equal(3, objective.TurnCount);
        Assert.Equal("1250000000", objective.CreditCountNanoAiu);
        Assert.Equal(5, objective.CreditLimit!.Credits);
        Assert.Equal(1.25, objective.CreditLimit.CreditsUsed);
        Assert.Equal("1250000000", objective.CreditLimit.CreditsUsedNanoAiu);

        var enabled = await session.Rpc.Remote.EnableAsync(new RemoteSessionMode(remoteMode));
        Assert.Equal(expectedSteerable, enabled.RemoteSteerable);
        Assert.Equal($"https://example.test/sessions/{session.SessionId}", enabled.Url);

        var remoteRequest = Assert.Single(
            await ReadRequestsAsync(capturePath, "session.remote.enable")).GetProperty("params");
        Assert.Equal(session.SessionId, remoteRequest.GetProperty("sessionId").GetString());
        Assert.Equal(remoteMode, remoteRequest.GetProperty("mode").GetString());
    }

    [Fact]
    public async Task Should_Edit_Reorder_Duplicate_Remove_And_Send_Queued_Items()
    {
        var (cliPath, capturePath) = await ScenarioTestingTestCli.CreateAsync(Ctx);
        await using var client = Ctx.CreateClient(options: CreateFakeCliOptions(cliPath, capturePath));
        await using var session = await Ctx.CreateSessionAsync(client, new SessionConfig());

        await session.Rpc.Queue.SetDrainPausedAsync(true);
        var first = await session.Rpc.Queue.InsertAtAsync(
            0,
            new QueueInsertMessage
            {
                Prompt = "First hidden prompt",
                DisplayPrompt = "First visible prompt",
                AgentMode = SendAgentMode.Interactive,
            });
        var second = await session.Rpc.Queue.InsertAtAsync(
            1,
            new QueueInsertMessage
            {
                Prompt = "Second hidden prompt",
                DisplayPrompt = "Second visible prompt",
                AgentMode = SendAgentMode.Plan,
            });

        Assert.True(await UpdateTextAsync());
        var duplicate = await session.Rpc.Queue.DuplicateAtAsync(first.Id);
        Assert.NotEqual(first.Id, duplicate.Id);
        Assert.True((await session.Rpc.Queue.MoveItemAsync(second.Id, 0)).Changed);

        var reordered = await session.Rpc.Queue.PendingItemsAsync();
        Assert.Equal([second.Id, first.Id, duplicate.Id], reordered.Items.Select(item => item.Id));
        Assert.Equal("Updated visible prompt", reordered.Items[1].DisplayText);
        Assert.Equal(SendAgentMode.Interactive, reordered.Items[1].AgentMode);

        Assert.True((await session.Rpc.Queue.SendNowAsync(second.Id)).Steered);
        Assert.True((await session.Rpc.Queue.RemoveAtAsync(duplicate.Id)).Removed);

        var remaining = Assert.Single((await session.Rpc.Queue.PendingItemsAsync()).Items);
        Assert.Equal(first.Id, remaining.Id);
        Assert.Equal("Updated visible prompt", remaining.DisplayText);
        await session.Rpc.Queue.SetDrainPausedAsync(false);

        var pauseRequests = await ReadRequestsAsync(capturePath, "session.queue.setDrainPaused");
        Assert.Equal([true, false], pauseRequests.Select(
            request => request.GetProperty("params").GetProperty("paused").GetBoolean()));

        async Task<bool> UpdateTextAsync()
        {
            var result = await session.Rpc.Queue.UpdateTextAsync(
                first.Id,
                "Updated hidden prompt",
                "Updated visible prompt");
            return result.Updated;
        }
    }

    private static CopilotClientOptions CreateFakeCliOptions(string cliPath, string capturePath) => new()
    {
        Connection = RuntimeConnection.ForStdio(
            path: cliPath,
            args: ["--capture-file", capturePath, "--behavior", "control-rpcs"]),
        UseLoggedInUser = false,
    };

    private static async Task<JsonElement[]> ReadRequestsAsync(string capturePath, string method) =>
        (await ScenarioTestingTestCli.ReadRequestsAsync(capturePath))
            .Where(request => GetMethod(request) == method)
            .ToArray();

    private static string? GetMethod(JsonElement request) =>
        request.GetProperty("method").GetString();
}
