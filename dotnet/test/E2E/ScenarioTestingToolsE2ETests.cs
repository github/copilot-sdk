/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Test.Harness;
using Microsoft.Extensions.AI;
using System.ComponentModel;
using System.Text.Json;
using System.Text.Json.Nodes;
using System.Text.Json.Serialization;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

/// <summary>
/// Representative scenario coverage for host-owned tools.
/// </summary>
public partial class ScenarioTestingToolsE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : ScenarioTestingE2ETestBase(fixture, "scenario_testing_tools", output)
{
    private static readonly TimeSpan EventTimeout = TimeSpan.FromSeconds(60);

    [JsonSourceGenerationOptions(JsonSerializerDefaults.Web)]
    [JsonSerializable(typeof(ToolResultAIContent))]
    [JsonSerializable(typeof(ToolResultObject))]
    [JsonSerializable(typeof(JsonElement))]
    private partial class ScenarioToolsJsonContext : JsonSerializerContext;

    [Fact]
    public async Task Should_Advertise_Scenario_Tool_Schema_Override_And_Availability()
    {
        var hiddenToolCalled = false;
        await using var session = await CreateSessionAsync(new SessionConfig
        {
            ClientName = "scenario-client",
            Tools =
            [
                CopilotTool.DefineTool(
                    (Func<string, int, string>)LookupIssue,
                    factoryOptions: new AIFunctionFactoryOptions
                    {
                        Name = "scenario_lookup_issue",
                        Description = "Looks up an issue in the scenario client installation.",
                    }),
                CopilotTool.DefineTool(
                    (Func<string, string>)ScenarioGrep,
                    new CopilotToolOptions { OverridesBuiltInTool = true },
                    new AIFunctionFactoryOptions
                    {
                        Name = "grep",
                        Description = "Searches the scenario-owned index.",
                    }),
                CopilotTool.DefineTool(
                    (Func<string>)HiddenAdminTool,
                    factoryOptions: new AIFunctionFactoryOptions { Name = "scenario_hidden_admin" }),
            ],
            AvailableTools = new ToolSet()
                .AddCustom("scenario_lookup_issue")
                .AddCustom("grep"),
            ExcludedTools = new ToolSet().AddCustom("scenario_hidden_admin"),
        });

        var response = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Call scenario_lookup_issue for owner octo and issue number 42. Reply with its result.",
        });

        Assert.Contains("SCENARIO_ISSUE_octo_42", response?.Data.Content ?? string.Empty, StringComparison.Ordinal);
        Assert.False(hiddenToolCalled);

        var exchange = (await Ctx.GetExchangesAsync()).Last();
        var names = GetToolNames(exchange);
        Assert.Contains("scenario_lookup_issue", names);
        Assert.Contains("grep", names);
        Assert.DoesNotContain("scenario_hidden_admin", names);
        Assert.Equal(1, names.Count(name => name == "grep"));

        var lookup = Assert.Single(exchange.Request.Tools!, tool => tool.Function.Name == "scenario_lookup_issue");
        Assert.Equal("Looks up an issue in the scenario client installation.", lookup.Function.Description);
        var parameters = lookup.Function.Parameters!.Value;
        Assert.Equal("object", parameters.GetProperty("type").GetString());
        Assert.Equal("string", parameters.GetProperty("properties").GetProperty("owner").GetProperty("type").GetString());
        Assert.Equal("integer", parameters.GetProperty("properties").GetProperty("number").GetProperty("type").GetString());

        static string LookupIssue(
            [Description("Repository owner")] string owner,
            [Description("Issue number")] int number) =>
            $"SCENARIO_ISSUE_{owner}_{number}";

        static string ScenarioGrep([Description("Search query")] string query) => $"SCENARIO_GREP_{query}";

        string HiddenAdminTool()
        {
            hiddenToolCalled = true;
            return "SHOULD_NOT_RUN";
        }
    }

    [Fact]
    public async Task Should_Preserve_Scenario_Tool_Invocation_Identity_Arguments_And_Text()
    {
        ToolInvocation? observedInvocation = null;
        var toolCompleted = new TaskCompletionSource<ToolExecutionCompleteEvent>(
            TaskCreationOptions.RunContinuationsAsynchronously);

        await using var session = await CreateSessionAsync(new SessionConfig
        {
            ClientName = "scenario-client",
            Tools =
            [
                CopilotTool.DefineTool(
                    (Func<string, ToolInvocation, TextContent>)SearchPullRequests,
                    factoryOptions: new AIFunctionFactoryOptions
                    {
                        Name = "scenario_search_pull_requests",
                        Description = "Searches pull requests visible to the scenario client.",
                    }),
            ],
        });
        using var subscription = session.On<ToolExecutionCompleteEvent>(evt =>
            toolCompleted.TrySetResult(evt));

        var response = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Call scenario_search_pull_requests with query is:open label:bug. Reply with its result.",
        });
        var completed = await toolCompleted.Task.WaitAsync(EventTimeout);

        Assert.NotNull(observedInvocation);
        Assert.Equal(session.SessionId, observedInvocation!.SessionId);
        Assert.Equal("scenario_search_pull_requests", observedInvocation.ToolName);
        Assert.False(string.IsNullOrWhiteSpace(observedInvocation.ToolCallId));
        Assert.Equal("is:open label:bug", observedInvocation.Arguments!.Value.GetProperty("query").GetString());
        Assert.Equal(observedInvocation.ToolCallId, completed.Data.ToolCallId);
        Assert.True(completed.Data.Success);
        Assert.Contains("SCENARIO_SEARCH_TEXT", response?.Data.Content ?? string.Empty, StringComparison.Ordinal);

        TextContent SearchPullRequests(
            [Description("GitHub search query")] string query,
            ToolInvocation invocation)
        {
            observedInvocation = invocation;
            return new TextContent($"SCENARIO_SEARCH_TEXT:{query}");
        }
    }

    [Fact]
    public async Task Should_Deliver_Expanded_Scenario_Tool_Result_To_The_Model()
    {
        await using var session = await CreateSessionAsync(new SessionConfig
        {
            ClientName = "scenario-client",
            Tools =
            [
                AIFunctionFactory.Create(
                    GetDeployment,
                    "scenario_get_deployment",
                    serializerOptions: ScenarioToolsJsonContext.Default.Options),
            ],
        });

        var response = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Call scenario_get_deployment for environment production. Reply with its result.",
        });

        Assert.Contains("SCENARIO_DEPLOYMENT_READY", response?.Data.Content ?? string.Empty, StringComparison.Ordinal);
        var exchange = (await Ctx.GetExchangesAsync()).Last();
        var toolResult = Assert.Single(exchange.Request.Messages, message => message.Role == "tool");
        Assert.Equal("SCENARIO_DEPLOYMENT_READY:production", toolResult.StringContent);
        Assert.DoesNotContain("toolTelemetry", toolResult.StringContent, StringComparison.Ordinal);
        Assert.DoesNotContain("resultType", toolResult.StringContent, StringComparison.Ordinal);

        [Description("Gets deployment state from the scenario client")]
        static ToolResultAIContent GetDeployment([Description("Deployment environment")] string environment) =>
            new(new ToolResultObject
            {
                TextResultForLlm = $"SCENARIO_DEPLOYMENT_READY:{environment}",
                ResultType = "success",
                SessionLog = "scenario client deployment lookup completed.",
                ToolTelemetry = new Dictionary<string, object>
                {
                    ["source"] = JsonValue.Create("scenario-client")!,
                },
            });
    }

    [Fact]
    public async Task Should_Isolate_Scenario_Tool_Handler_Error()
    {
        var toolCompleted = new TaskCompletionSource<ToolExecutionCompleteEvent>(
            TaskCreationOptions.RunContinuationsAsynchronously);
        await using var session = await CreateSessionAsync(new SessionConfig
        {
            ClientName = "scenario-client",
            Tools = [AIFunctionFactory.Create(FailingLookup, "scenario_failing_lookup")],
        });
        using var subscription = session.On<ToolExecutionCompleteEvent>(evt =>
            toolCompleted.TrySetResult(evt));

        var response = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Call scenario_failing_lookup. If it fails, reply with exactly SCENARIO_LOOKUP_UNAVAILABLE.",
        });
        var completed = await toolCompleted.Task.WaitAsync(EventTimeout);

        Assert.False(completed.Data.Success);
        Assert.DoesNotContain("SCENARIO_PRIVATE_HANDLER_DETAIL", completed.Data.Error?.Message ?? string.Empty, StringComparison.Ordinal);
        Assert.Contains("SCENARIO_LOOKUP_UNAVAILABLE", response?.Data.Content ?? string.Empty, StringComparison.Ordinal);
        Assert.DoesNotContain("SCENARIO_PRIVATE_HANDLER_DETAIL", response?.Data.Content ?? string.Empty, StringComparison.Ordinal);

        static string FailingLookup() => throw new InvalidOperationException("SCENARIO_PRIVATE_HANDLER_DETAIL");
    }

    [Fact]
    public async Task Should_Cancel_Scenario_Tool_Handler_When_Session_Disposes()
    {
        var started = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);
        var cancelled = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var release = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);

        var session = await CreateSessionAsync(new SessionConfig
        {
            ClientName = "scenario-client",
            Tools = [AIFunctionFactory.Create(WaitForScenarioAsync, "scenario_wait_for_operation")],
        });

        _ = session.SendAsync(new MessageOptions
        {
            Prompt = "Call scenario_wait_for_operation with operation sync-installation.",
        });

        Assert.Equal("sync-installation", await started.Task.WaitAsync(EventTimeout));
        await session.DisposeAsync();
        await cancelled.Task.WaitAsync(EventTimeout);
        release.TrySetResult("RELEASED_AFTER_DISPOSE");

        [Description("Waits for a scenario-owned operation")]
        async Task<string> WaitForScenarioAsync(
            [Description("Operation name")] string operation,
            CancellationToken cancellationToken)
        {
            started.TrySetResult(operation);
            try
            {
                return await release.Task.WaitAsync(Timeout.InfiniteTimeSpan, cancellationToken);
            }
            catch (OperationCanceledException)
            {
                cancelled.TrySetResult();
                throw;
            }
        }
    }
}
