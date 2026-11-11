/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System.Text.Json;
using GitHub.Copilot.Rpc;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    private static PermissionDecisionContext PersonAnswered() => new()
    {
        Source = PermissionDecisionSource.HumanResponse,
        ResponseCapability = PermissionResponseCapability.Interactive,
        Surface = PermissionDecisionSurface.Sdk,
        Outcome = PermissionDecisionOutcome.PromptedUser,
    };

    private static void AssertPersonAnsweredLabel(JsonElement decisionContext)
    {
        Assert.Equal(JsonValueKind.Object, decisionContext.ValueKind);
        var fields = decisionContext.EnumerateObject().ToDictionary(field => field.Name, field => field.Value.GetString());
        Assert.Equal(
            new Dictionary<string, string?>
            {
                ["outcome"] = "prompted_user",
                ["responseCapability"] = "interactive",
                ["source"] = "human_response",
                ["surface"] = "sdk",
            },
            fields);
    }

    [Theory]
    [InlineData(true)]
    [InlineData(false)]
    public async Task UserInputRequest_Reply_Carries_DecisionContext_Only_When_Handler_Supplies_One(bool labeled)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnUserInputRequest = (_, _) => Task.FromResult(new UserInputResponse
            {
                Answer = "Only src/app.ts",
                WasFreeform = true,
                DecisionContext = labeled ? PersonAnswered() : null,
            })
        });

        var response = await server.SendRequestAsync("userInput.request", new Dictionary<string, object?>
        {
            ["sessionId"] = session.SessionId,
            ["question"] = "Which files?",
            ["allowFreeform"] = true,
            ["requestId"] = "user-input-1"
        }).WaitAsync(TimeSpan.FromSeconds(5));

        Assert.Equal("Only src/app.ts", response.GetProperty("answer").GetString());
        Assert.True(response.GetProperty("wasFreeform").GetBoolean());
        if (labeled)
        {
            AssertPersonAnsweredLabel(response.GetProperty("decisionContext"));
        }
        else
        {
            Assert.False(response.TryGetProperty("decisionContext", out var unexpected), $"Unexpected decisionContext ({unexpected.ValueKind}): {unexpected}");
        }
    }

    [Theory]
    [InlineData(true)]
    [InlineData(false)]
    public async Task ExitPlanModeRequest_Reply_Carries_DecisionContext_Only_When_Handler_Supplies_One(bool labeled)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnExitPlanModeRequest = (_, _) => Task.FromResult(new ExitPlanModeResult
            {
                Approved = false,
                Feedback = "Split the migration first",
                DecisionContext = labeled ? PersonAnswered() : null,
            })
        });

        var response = await server.SendRequestAsync("exitPlanMode.request", new Dictionary<string, object?>
        {
            ["sessionId"] = session.SessionId,
            ["summary"] = "Plan summary",
            ["actions"] = new object?[] { "interactive", "autopilot" },
            ["recommendedAction"] = "interactive",
            ["requestId"] = "exit-plan-mode-1"
        }).WaitAsync(TimeSpan.FromSeconds(5));

        Assert.False(response.GetProperty("approved").GetBoolean());
        Assert.Equal("Split the migration first", response.GetProperty("feedback").GetString());
        if (labeled)
        {
            AssertPersonAnsweredLabel(response.GetProperty("decisionContext"));
        }
        else
        {
            Assert.False(response.TryGetProperty("decisionContext", out var unexpected), $"Unexpected decisionContext ({unexpected.ValueKind}): {unexpected}");
        }
    }

    [Theory]
    [InlineData(true)]
    [InlineData(false)]
    public async Task ElicitationAnswer_Sends_DecisionContext_As_Sibling_Of_Result_Only_When_Handler_Supplies_One(bool labeled)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = request => request.Method == "session.ui.handlePendingElicitation"
            ? new Dictionary<string, object?> { ["success"] = true }
            : throw new InvalidOperationException($"Unexpected RPC method '{request.Method}'.");
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnElicitationRequest = _ => Task.FromResult(new ElicitationResult
            {
                Action = UIElicitationResponseAction.Accept,
                Content = new Dictionary<string, object> { ["name"] = "scenario" },
                DecisionContext = labeled ? PersonAnswered() : null,
            })
        });

        await server.SendSessionEventAsync(session.SessionId, "elicitation.requested", new Dictionary<string, object?>
        {
            ["requestId"] = "elicitation-1",
            ["message"] = "Provide scenario settings",
            ["mode"] = "form",
            ["requestedSchema"] = new Dictionary<string, object?>
            {
                ["type"] = "object",
                ["properties"] = new Dictionary<string, object?>
                {
                    ["name"] = new Dictionary<string, object?> { ["type"] = "string" }
                },
                ["required"] = new object?[] { "name" }
            }
        });

        var request = await WaitForRequestAsync(server, "session.ui.handlePendingElicitation");
        Assert.Equal(session.SessionId, request.Params.GetProperty("sessionId").GetString());
        Assert.Equal("elicitation-1", request.Params.GetProperty("requestId").GetString());
        var result = request.Params.GetProperty("result");
        Assert.Equal("accept", result.GetProperty("action").GetString());
        Assert.Equal("scenario", result.GetProperty("content").GetProperty("name").GetString());
        Assert.False(result.TryGetProperty("decisionContext", out var nested), $"decisionContext must not be inside result ({nested.ValueKind}): {nested}");
        if (labeled)
        {
            AssertPersonAnsweredLabel(request.Params.GetProperty("decisionContext"));
        }
        else
        {
            Assert.False(request.Params.TryGetProperty("decisionContext", out var unexpected), $"Unexpected decisionContext ({unexpected.ValueKind}): {unexpected}");
        }
    }
}
#endif
