/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using System.Text.Json;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class RpcTasksAndHandlersE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rpc_tasks_and_handlers", output)
{
    [Fact]
    public async Task Should_Round_Trip_Rpc_Elicitation_Through_Config_Handler()
    {
        var handlerContext = new TaskCompletionSource<ElicitationContext>(TaskCreationOptions.RunContinuationsAsynchronously);
        var session = await CreateSessionAsync(new SessionConfig
        {
            OnElicitationRequest = context =>
            {
                handlerContext.TrySetResult(context);
                return Task.FromResult(new ElicitationResult
                {
                    Action = UIElicitationResponseAction.Accept,
                    Content = new Dictionary<string, object>
                    {
                        ["answer"] = "from handler",
                        ["confirmed"] = true,
                    },
                });
            },
        });

        var schema = new UIElicitationSchema
        {
            Type = "object",
            Properties = new Dictionary<string, JsonElement>
            {
                ["answer"] = ParseJsonElement("""{"type":"string"}"""),
                ["confirmed"] = ParseJsonElement("""{"type":"boolean"}"""),
            },
            Required = ["answer"],
        };

        var response = await session.Rpc.Ui.ElicitationAsync("Need details", schema);
        var context = await handlerContext.Task.WaitAsync(TimeSpan.FromSeconds(30));

        Assert.Equal(session.SessionId, context.SessionId);
        Assert.Equal("Need details", context.Message);
        Assert.NotNull(context.RequestedSchema);
        Assert.Equal("object", context.RequestedSchema.Type);
        Assert.Contains("answer", context.RequestedSchema.Properties.Keys);
        Assert.Contains("confirmed", context.RequestedSchema.Properties.Keys);
        Assert.Equal(["answer"], context.RequestedSchema.Required);

        Assert.Equal(UIElicitationResponseAction.Accept, response.Action);
        Assert.NotNull(response.Content);
        Assert.Equal("from handler", response.Content["answer"].GetString());
        Assert.True(response.Content["confirmed"].GetBoolean());
    }

    private static JsonElement ParseJsonElement(string json)
    {
        using var document = JsonDocument.Parse(json);
        return document.RootElement.Clone();
    }
}
