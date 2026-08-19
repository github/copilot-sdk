/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot;
using GitHub.Copilot.Rpc;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

/// <summary>
/// E2E coverage for the session-scoped <c>session.ui.ephemeralQuery</c> RPC method. Unlike the
/// other newly covered methods this one is model-backed: the runtime runs a transient, no-tools
/// model completion against the current conversation context and returns the assistant's answer
/// without recording it in the conversation history. The exchange is served from a recorded
/// snapshot so the assertion on the answer text is deterministic.
/// </summary>
public class RpcUiEphemeralQueryE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rpc_ui_ephemeral_query", output)
{
    [Fact]
    public async Task Should_Answer_Ephemeral_Query()
    {
        await using var session = await CreateSessionAsync();

        // A fresh session has no prior turns, so the ephemeral query is sent to the model as a
        // single user message with the runtime's transient "quick side question" system prompt.
        // The recorded snapshot supplies a canned answer, letting us assert a meaningful value.
        var result = await session.Rpc.Ui.EphemeralQueryAsync(
            "In one word, what is the primary color of a clear daytime sky?");

        Assert.NotNull(result);
        Assert.False(string.IsNullOrWhiteSpace(result.Answer));
        Assert.Contains("blue", result.Answer, StringComparison.OrdinalIgnoreCase);
    }
}
