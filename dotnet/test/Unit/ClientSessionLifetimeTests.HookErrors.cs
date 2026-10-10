/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System.Text.Json;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    public static TheoryData<string, string?, string?> HookErrorPayloads => new()
    {
        { "errorOccurred", "\"model timeout\"", "model timeout" },
        {
            "errorOccurred",
            """{"name":"Error","message":"model timeout","stack":"Error: model timeout"}""",
            "model timeout"
        },
        { "errorOccurred", """{"message":""}""", "" },
        { "errorOccurred", """{"name":"Error","code":"ECONNRESET"}""", """{"name":"Error","code":"ECONNRESET"}""" },
        { "errorOccurred", """{"message":null}""", """{"message":null}""" },
        { "errorOccurred", """{"message":42}""", """{"message":42}""" },
        { "errorOccurred", "null", null },
        { "errorOccurred", null, "" },
        { "sessionEnd", "\"model timeout\"", "model timeout" },
        { "sessionEnd", """{"message":"model timeout"}""", "model timeout" },
        { "sessionEnd", """{"name":"Error","code":"ECONNRESET"}""", """{"name":"Error","code":"ECONNRESET"}""" },
        { "sessionEnd", "null", null },
        { "sessionEnd", null, null }
    };

    [Theory]
    [MemberData(nameof(HookErrorPayloads))]
    public async Task HookErrors_InvokeCallbacksAndReturnTheirDecisions(
        string hookType, string? errorJson, string? expectedError)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        var calls = new List<string?>();
        var hooks = new SessionHooks
        {
            OnErrorOccurred = (input, invocation) =>
            {
                Assert.Equal("errorOccurred", hookType);
                Assert.Equal(input.SessionId, invocation.SessionId);
                Assert.Equal("model_call", input.ErrorContext);
                Assert.True(input.Recoverable);
                calls.Add(input.Error);
                return Task.FromResult<ErrorOccurredHookOutput?>(new ErrorOccurredHookOutput
                {
                    ErrorHandling = "abort"
                });
            },
            OnSessionEnd = (input, invocation) =>
            {
                Assert.Equal("sessionEnd", hookType);
                Assert.Equal(input.SessionId, invocation.SessionId);
                Assert.Equal("error", input.Reason);
                calls.Add(input.Error);
                return Task.FromResult<SessionEndHookOutput?>(new SessionEndHookOutput
                {
                    SessionSummary = "Failure observed"
                });
            }
        };
        await using var session = await client.CreateSessionAsync(new SessionConfig { Hooks = hooks });
        var input = new Dictionary<string, object?>
        {
            ["sessionId"] = session.SessionId,
            ["timestamp"] = 1_730_000_000_000L,
            ["cwd"] = "workdir",
            ["errorContext"] = "model_call",
            ["recoverable"] = true,
            ["reason"] = "error"
        };
        if (errorJson is not null)
        {
            using var error = JsonDocument.Parse(errorJson);
            input["error"] = error.RootElement.Clone();
        }

        var result = await server.SendRequestAsync("hooks.invoke", new Dictionary<string, object?>
        {
            ["sessionId"] = session.SessionId,
            ["hookType"] = hookType,
            ["input"] = input
        }).WaitAsync(TimeSpan.FromSeconds(5));

        Assert.Equal(expectedError, Assert.Single(calls));
        var output = result.GetProperty("output");
        if (hookType == "errorOccurred")
        {
            Assert.Equal("abort", output.GetProperty("errorHandling").GetString());
        }
        else
        {
            Assert.Equal("Failure observed", output.GetProperty("sessionSummary").GetString());
        }
    }

    [Theory]
    [InlineData("42")]
    [InlineData("true")]
    [InlineData("[]")]
    public async Task HookErrors_RejectUnsupportedPayloadsWithoutInvokingCallback(string errorJson)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        var called = false;
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            Hooks = new SessionHooks
            {
                OnErrorOccurred = (_, _) =>
                {
                    called = true;
                    return Task.FromResult<ErrorOccurredHookOutput?>(new ErrorOccurredHookOutput());
                }
            }
        });
        using var error = JsonDocument.Parse(errorJson);

        var exception = await Assert.ThrowsAnyAsync<Exception>(() => server.SendRequestAsync(
            "hooks.invoke", new Dictionary<string, object?>
            {
                ["sessionId"] = session.SessionId,
                ["hookType"] = "errorOccurred",
                ["input"] = new Dictionary<string, object?>
                {
                    ["sessionId"] = session.SessionId,
                    ["error"] = error.RootElement.Clone()
                }
            }).WaitAsync(TimeSpan.FromSeconds(5)));

        Assert.IsNotType<TimeoutException>(exception);
        Assert.Contains("error", exception.Message, StringComparison.OrdinalIgnoreCase);
        Assert.False(called);
    }
}
#endif
