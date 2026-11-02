/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    [Theory]
    [InlineData("pending-user-input-1")]
    [InlineData(null)]
    public async Task UserInputRequest_Passes_Runtime_RequestId_To_Handler(string? requestId)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var received = new TaskCompletionSource<UserInputRequest>(TaskCreationOptions.RunContinuationsAsynchronously);
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnUserInputRequest = (request, _) =>
            {
                received.TrySetResult(request);
                return Task.FromResult(new UserInputResponse { Answer = "blue", WasFreeform = true });
            }
        });
        var parameters = new Dictionary<string, object?>
        {
            ["sessionId"] = session.SessionId,
            ["question"] = "Favorite color?",
            ["choices"] = new object?[] { "blue", "green" },
            ["allowFreeform"] = true
        };
        // Older runtimes omit requestId entirely.
        if (requestId is not null)
        {
            parameters["requestId"] = requestId;
        }

        var response = await server.SendRequestAsync("userInput.request", parameters).WaitAsync(TimeSpan.FromSeconds(5));

        var request = await received.Task.WaitAsync(TimeSpan.FromSeconds(5));
        Assert.Equal(requestId, request.RequestId);
        Assert.Equal("Favorite color?", request.Question);
        Assert.Equal(["blue", "green"], request.Choices);
        Assert.Equal("blue", response.GetProperty("answer").GetString());
        Assert.True(response.GetProperty("wasFreeform").GetBoolean());
    }

    [Theory]
    [InlineData("pending-exit-plan-mode-1")]
    [InlineData(null)]
    public async Task ExitPlanModeRequest_Passes_Runtime_RequestId_To_Handler(string? requestId)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var received = new TaskCompletionSource<ExitPlanModeRequest>(TaskCreationOptions.RunContinuationsAsynchronously);
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnExitPlanModeRequest = (request, _) =>
            {
                received.TrySetResult(request);
                return Task.FromResult(new ExitPlanModeResult { Approved = true, SelectedAction = "interactive" });
            }
        });
        var parameters = new Dictionary<string, object?>
        {
            ["sessionId"] = session.SessionId,
            ["summary"] = "Plan summary",
            ["planContent"] = "1. Do the thing",
            ["actions"] = new object?[] { "interactive", "autopilot" },
            ["recommendedAction"] = "interactive"
        };
        // Older runtimes omit requestId entirely.
        if (requestId is not null)
        {
            parameters["requestId"] = requestId;
        }

        var response = await server.SendRequestAsync("exitPlanMode.request", parameters).WaitAsync(TimeSpan.FromSeconds(5));

        var request = await received.Task.WaitAsync(TimeSpan.FromSeconds(5));
        Assert.Equal(requestId, request.RequestId);
        Assert.Equal("Plan summary", request.Summary);
        Assert.Equal("1. Do the thing", request.PlanContent);
        Assert.Equal(["interactive", "autopilot"], request.Actions);
        Assert.Equal("interactive", request.RecommendedAction);
        Assert.True(response.GetProperty("approved").GetBoolean());
        Assert.Equal("interactive", response.GetProperty("selectedAction").GetString());
    }
}
#endif
