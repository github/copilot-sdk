/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System.Text.Json;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    [Fact]
    public async Task Session_Events_Nested_Past_Serializer_Default_Depth_Are_Delivered_And_Listed()
    {
        const int levels = 200;
        var argumentsJson = string.Concat(Enumerable.Repeat("""{"n":""", levels)) + "42" + new string('}', levels);
        using var arguments = JsonDocument.Parse(argumentsJson, new JsonDocumentOptions { MaxDepth = levels });
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        var delivered = new TaskCompletionSource<ToolExecutionStartEvent>(TaskCreationOptions.RunContinuationsAsynchronously);
        using var subscription = session.On<ToolExecutionStartEvent>(evt => delivered.TrySetResult(evt));

        await server.SendSessionEventAsync(session.SessionId, "tool.execution_start", new()
        {
            ["toolCallId"] = "call-1",
            ["toolName"] = "nested_tool",
            ["arguments"] = arguments.RootElement,
        });

        var live = await delivered.Task.WaitAsync(TimeSpan.FromSeconds(5));
        Assert.Equal(argumentsJson, live.Data.Arguments?.GetRawText());
        var listed = Assert.Single((await session.GetEventsAsync()).OfType<ToolExecutionStartEvent>());
        Assert.Equal(argumentsJson, listed.Data.Arguments?.GetRawText());
        var restored = Assert.IsType<ToolExecutionStartEvent>(SessionEvent.FromJson(listed.ToJson()));
        Assert.Equal(argumentsJson, restored.Data.Arguments?.GetRawText());
    }

    [Theory]
    [InlineData("preToolUse")]
    [InlineData("postToolUse")]
    public async Task Hook_Arguments_Nested_Past_Serializer_Default_Depth_Reach_Handler(string hookType)
    {
        const int levels = 200;
        var argumentsJson = string.Concat(Enumerable.Repeat("""{"n":""", levels)) + "42" + new string('}', levels);
        using var arguments = JsonDocument.Parse(argumentsJson, new JsonDocumentOptions { MaxDepth = levels });
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        JsonElement? received = null;
        var hooks = new SessionHooks
        {
            OnPreToolUse = (input, _) =>
            {
                received = input.ToolArgs;
                return Task.FromResult<PreToolUseHookOutput?>(null);
            },
            OnPostToolUse = (input, _) =>
            {
                received = input.ToolArgs;
                return Task.FromResult<PostToolUseHookOutput?>(null);
            },
        };
        await using var session = await client.CreateSessionAsync(new SessionConfig { Hooks = hooks });

        await server.SendRequestAsync("hooks.invoke", new()
        {
            ["sessionId"] = session.SessionId,
            ["hookType"] = hookType,
            ["input"] = new Dictionary<string, object?>
            {
                ["sessionId"] = session.SessionId,
                ["timestamp"] = 1_730_000_000_000L,
                ["cwd"] = "workdir",
                ["toolName"] = "nested_tool",
                ["toolArgs"] = arguments.RootElement,
            },
        }).WaitAsync(TimeSpan.FromSeconds(5));

        Assert.Equal(argumentsJson, received?.GetRawText());
    }
}
#endif
