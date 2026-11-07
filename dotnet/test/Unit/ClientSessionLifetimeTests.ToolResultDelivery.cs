/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task ExternalTool_Reports_And_Answers_With_Error_When_Result_Send_Fails(bool handlerFails)
    {
        var logger = new SkillProviderLogger();
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url), Logger = logger });
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            Tools = [handlerFails
                ? new ThrowingFunction(new InvalidOperationException("handler failed"))
                : new ToolResultFunction(new ToolResultObject { TextResultForLlm = "ok", ResultType = "success" })],
        });
        server.FailPendingToolCall();

        await server.SendSessionEventAsync(session.SessionId, "external_tool.requested", new()
        {
            ["requestId"] = "delivery-request",
            ["sessionId"] = session.SessionId,
            ["toolCallId"] = "delivery-call",
            ["toolName"] = "record_note",
            ["arguments"] = new Dictionary<string, object?>(),
        });
        await server.RequestsChanged.WaitForAsync(
            () => server.Requests.Count(request => request.Method == "session.tools.handlePendingToolCall") == 2,
            TimeSpan.FromSeconds(5),
            timeoutMessage: "The tool call was not answered after its result failed to send.");

        var answers = server.Requests
            .Where(request => request.Method == "session.tools.handlePendingToolCall")
            .Select(request => request.Params)
            .ToArray();
        Assert.Equal("delivery-request", answers[0].GetProperty("requestId").GetString());
        if (handlerFails)
        {
            Assert.Equal("handler failed", answers[0].GetProperty("error").GetString());
            Assert.False(answers[0].TryGetProperty("result", out _));
        }
        else
        {
            Assert.Equal("ok", answers[0].GetProperty("result").GetProperty("textResultForLlm").GetString());
        }
        Assert.Equal("delivery-request", answers[1].GetProperty("requestId").GetString());
        Assert.False(answers[1].TryGetProperty("result", out _));
        Assert.Equal("Communication error with Copilot CLI: tool result rejected", answers[1].GetProperty("error").GetString());
        Assert.Contains(logger.Entries, entry =>
            entry.Message.StartsWith("External tool result delivery failed.", StringComparison.Ordinal) &&
            entry.Exception is IOException { InnerException.Message: "tool result rejected" });
    }
}
#endif
