/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System.Text.Json;
using Microsoft.Extensions.AI;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    [Theory]
    [InlineData("string", "plain text")]
    [InlineData("JSON string", "plain text")]
    [InlineData("factory passing the raw return value", "plain text")]
    [InlineData("number", "42")]
    public async Task ExternalTool_Sends_Tool_Result_Text(string kind, string expected)
    {
        AIFunction tool = kind switch
        {
            "string" => new ReturningFunction("plain text"),
            "JSON string" => new ReturningFunction(JsonDocument.Parse("\"plain text\"").RootElement.Clone()),
            "factory passing the raw return value" => AIFunctionFactory.Create(
                () => "plain text",
                new AIFunctionFactoryOptions
                {
                    Name = "record_note",
                    MarshalResult = (result, _, _) => new ValueTask<object?>(result),
                }),
            _ => new ReturningFunction(42),
        };
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig { Tools = [tool] });

        await server.SendSessionEventAsync(session.SessionId, "external_tool.requested", new()
        {
            ["requestId"] = "text-request",
            ["sessionId"] = session.SessionId,
            ["toolCallId"] = "text-call",
            ["toolName"] = "record_note",
            ["arguments"] = new Dictionary<string, object?>(),
        });
        var response = await WaitForRequestAsync(server, "session.tools.handlePendingToolCall");

        Assert.Equal(expected, response.Params.GetProperty("result").GetProperty("textResultForLlm").GetString());
    }

    /// <summary>Returns a fixed value without marshalling it through a serializer.</summary>
    private sealed class ReturningFunction(object value) : AIFunction
    {
        public override string Name => "record_note";

        protected override ValueTask<object?> InvokeCoreAsync(AIFunctionArguments arguments, CancellationToken cancellationToken) =>
            new(value);
    }
}
#endif
