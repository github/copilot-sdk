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
    [Fact]
    public async Task ExternalTool_Sends_Result_With_Serializable_Telemetry()
    {
        var response = await AnswerToolCallWithTelemetryAsync(new Dictionary<string, object>
        {
            ["metrics"] = JsonDocument.Parse("""{"durationMs":5}""").RootElement.Clone(),
            ["label"] = "note",
        });

        Assert.False(response.TryGetProperty("error", out _));
        var telemetry = response.GetProperty("result").GetProperty("toolTelemetry");
        Assert.Equal(5, telemetry.GetProperty("metrics").GetProperty("durationMs").GetInt32());
        Assert.Equal("note", telemetry.GetProperty("label").GetString());
    }

    [Theory]
    [InlineData("non-finite number")]
    [InlineData("type without serialization metadata")]
    public async Task ExternalTool_Answers_With_Error_When_Telemetry_Cannot_Be_Serialized(string kind)
    {
        object value = kind == "non-finite number" ? double.NaN : new TelemetryValueWithoutMetadata();

        var response = await AnswerToolCallWithTelemetryAsync(new Dictionary<string, object> { ["value"] = value });

        Assert.False(response.TryGetProperty("result", out _));
        Assert.StartsWith("failed to serialize tool telemetry: ", response.GetProperty("error").GetString());
    }

    private static async Task<JsonElement> AnswerToolCallWithTelemetryAsync(IDictionary<string, object> telemetry)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            Tools = [new ToolResultFunction(new ToolResultObject
            {
                TextResultForLlm = "ok",
                ResultType = "success",
                ToolTelemetry = telemetry,
            })],
        });

        await server.SendSessionEventAsync(session.SessionId, "external_tool.requested", new()
        {
            ["requestId"] = "telemetry-request",
            ["sessionId"] = session.SessionId,
            ["toolCallId"] = "telemetry-call",
            ["toolName"] = "record_note",
            ["arguments"] = new Dictionary<string, object?>(),
        });
        var response = await WaitForRequestAsync(server, "session.tools.handlePendingToolCall");
        return response.Params.Clone();
    }

    private sealed class TelemetryValueWithoutMetadata
    {
    }

    /// <summary>Returns a fixed tool result without marshalling it through a serializer.</summary>
    private sealed class ToolResultFunction(ToolResultObject result) : AIFunction
    {
        public override string Name => "record_note";

        protected override ValueTask<object?> InvokeCoreAsync(AIFunctionArguments arguments, CancellationToken cancellationToken) =>
            new(new ToolResultAIContent(result));
    }
}
#endif
