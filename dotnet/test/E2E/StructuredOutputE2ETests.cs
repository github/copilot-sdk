/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using System.Text.Json;
using System.Text.Json.Serialization;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public partial class StructuredOutputE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "structured_output", output)
{
    private SessionConfig StructuredSessionConfig() => new()
    {
        Model = "gpt-4.1",
        AvailableTools = [],
        Provider = new ProviderConfig
        {
            Type = "openai",
            WireApi = "completions",
            BaseUrl = Ctx.ProxyUrl,
            ModelId = "gpt-4.1",
            WireModel = "gpt-4.1",
            ApiKey = Environment.GetEnvironmentVariable("GITHUB_ACTIONS") == "true"
                ? "fake-token-for-e2e-tests"
                : Environment.GetEnvironmentVariable("GITHUB_TOKEN") ?? "fake-token-for-e2e-tests",
            Headers = new Dictionary<string, string>
            {
                ["Copilot-Integration-Id"] = "copilot-developer-cli",
                ["Copilot-Harness-Id"] = "copilot-sdk",
                ["X-GitHub-Api-Version"] = "2026-08-01",
            },
        },
    };

    [Fact]
    public async Task Infers_Typed_Result_After_Custom_Tool()
    {
        var calls = 0;
        var config = StructuredSessionConfig();
        config.Tools =
        [
            CopilotTool.DefineTool(() =>
            {
                calls++;
                return "The inventory contains 42 red widgets.";
            }, factoryOptions: new() { Name = "get_inventory", Description = "Get the current widget inventory." }),
        ];
        var session = await CreateSessionAsync(config);

        var result = await session.SendAndWaitAsync<Inventory>(
            "Call get_inventory, then report the widget count and color.",
            StructuredOutputE2EJsonContext.Default.Options,
            TimeSpan.FromMinutes(3));
        Assert.True(calls > 0);
        Assert.Equal(42, result.Count);
        Assert.Equal("red", result.Color);

        var ordinary = await session.SendAndWaitAsync(
            "Now reply with exactly the plain text HELLO, not JSON.",
            TimeSpan.FromMinutes(3));
        Assert.NotNull(ordinary);
        Assert.Equal("HELLO", ordinary.Data.Content.Trim());
    }

    [Fact]
    public async Task Sends_Explicit_Schema_For_Message_And_Batch()
    {
        var session = await CreateSessionAsync(StructuredSessionConfig());
        var completion = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var replies = new System.Collections.Concurrent.ConcurrentQueue<AssistantMessageEvent>();
        using var subscription = session.On<SessionEvent>(evt =>
        {
            if (!string.IsNullOrEmpty(evt.AgentId)) return;
            switch (evt)
            {
                case AssistantMessageEvent message:
                    replies.Enqueue(message);
                    break;
                case SessionIdleEvent:
                    completion.TrySetResult();
                    break;
                case SessionErrorEvent error:
                    completion.TrySetException(new InvalidOperationException(error.Data.Message));
                    break;
            }
        });
        using var schema = JsonDocument.Parse(
            """{"type":"object","properties":{"count":{"type":"integer"},"color":{"type":"string"}},"required":["count","color"],"additionalProperties":false}""");
        var accepted = await session.Rpc.SendMessagesAsync(
            [new() { Prompt = "There are 42 red widgets in stock." }, new() { Prompt = "Report the widget count and color." }],
            responseFormat: new ResponseFormat
            {
                Type = "json_schema",
                JsonSchema = new JsonSchemaResponseFormat
                {
                    Name = "inventory",
                    Schema = schema.RootElement.Clone(),
                    Strict = true,
                    Description = "The widget inventory",
                },
            });
        using var cts = new CancellationTokenSource(TimeSpan.FromMinutes(3));
        await completion.Task.WaitAsync(cts.Token);
        var message = replies.Last(message => message.Data.OriginatingMessageId == accepted.MessageIds.Last());
        Assert.Equal(accepted.MessageIds.Last(), message.Data.OriginatingMessageId);
        Assert.Empty(message.Data.ToolRequests ?? []);
        var result = JsonSerializer.Deserialize(message.Data.Content, StructuredOutputE2EJsonContext.Default.Inventory);
        Assert.NotNull(result);
        Assert.Equal(42, result.Count);
        Assert.Equal("red", result.Color);

        var raw = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "The inventory now has 21 blue widgets. Report the new count and color.",
            ResponseSchema = schema.RootElement.Clone(),
        }, TimeSpan.FromMinutes(3));
        Assert.NotNull(raw);
        var updated = JsonSerializer.Deserialize(raw.Data.Content, StructuredOutputE2EJsonContext.Default.Inventory);
        Assert.NotNull(updated);
        Assert.Equal(21, updated.Count);
        Assert.Equal("blue", updated.Color);
    }

    [Fact]
    public async Task Send_Selects_Correlated_Response_After_Idle()
    {
        var hookEntered = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseHook = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var config = StructuredSessionConfig();
        config.Tools =
        [
            CopilotTool.DefineTool(() => "The inventory contains 42 red widgets.",
                factoryOptions: new() { Name = "read_inventory", Description = "Read the current widget count and color." }),
        ];
        config.Hooks = new SessionHooks
        {
            OnAgentStop = async (_, _) =>
            {
                hookEntered.TrySetResult();
                await releaseHook.Task;
                return null;
            },
        };
        var session = await CreateSessionAsync(config);
        var idleReceived = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var replies = new System.Collections.Concurrent.ConcurrentQueue<AssistantMessageEvent>();
        using var subscription = session.On<SessionEvent>(evt =>
        {
            if (!string.IsNullOrEmpty(evt.AgentId)) return;
            switch (evt)
            {
                case AssistantMessageEvent message:
                    replies.Enqueue(message);
                    break;
                case SessionErrorEvent error:
                    idleReceived.TrySetException(new InvalidOperationException(error.Data.Message));
                    break;
                case SessionIdleEvent:
                    idleReceived.TrySetResult();
                    break;
            }
        });
        using var cts = new CancellationTokenSource(TimeSpan.FromMinutes(3));
        using var schema = JsonDocument.Parse(
            """{"type":"object","properties":{"count":{"type":"integer"},"color":{"type":"string"}},"required":["count","color"],"additionalProperties":false}""");
        try
        {
            var messageId = await session.SendAsync(new MessageOptions
            {
                Prompt = "Call read_inventory once, then report the current widget count and color.",
                ResponseSchema = schema.RootElement.Clone(),
            }, cts.Token);
            await hookEntered.Task.WaitAsync(cts.Token);
            Assert.False(idleReceived.Task.IsCompleted);
            releaseHook.TrySetResult();
            await idleReceived.Task.WaitAsync(cts.Token);
            var reply = replies.Last(message => message.Data.OriginatingMessageId == messageId);
            Assert.Equal(messageId, reply.Data.OriginatingMessageId);
            var result = JsonSerializer.Deserialize(reply.Data.Content, StructuredOutputE2EJsonContext.Default.Inventory);
            Assert.NotNull(result);
            Assert.Equal(42, result.Count);
            Assert.Equal("red", result.Color);
            Assert.Contains(replies, message => message.Data.ToolRequests is { Length: > 0 });
            Assert.Empty(reply.Data.ToolRequests ?? []);
            Assert.Same(reply, replies.Last());
        }
        finally
        {
            releaseHook.TrySetResult();
        }
    }

    [Fact]
    public async Task Typed_Wait_Returns_Stop_Hook_Correction()
    {
        var stops = 0;
        var config = StructuredSessionConfig();
        config.Hooks = new SessionHooks
        {
            OnAgentStop = (_, _) => Task.FromResult<AgentStopHookOutput?>(
                Interlocked.Increment(ref stops) == 1
                    ? new() { Decision = "block", Reason = "Correct the answer to 99, not 42. Do not use tools." }
                    : null),
        };
        var session = await CreateSessionAsync(config);
        var replies = new System.Collections.Concurrent.ConcurrentQueue<AssistantMessageEvent>();
        using var subscription = session.On<AssistantMessageEvent>(message =>
        {
            if (string.IsNullOrEmpty(message.AgentId)) replies.Enqueue(message);
        });
        var result = await session.SendAndWaitAsync<CorrectionResult>(
            "What is 19 + 23? Do not use tools.",
            StructuredOutputE2EJsonContext.Default.Options,
            TimeSpan.FromMinutes(3));
        Assert.Equal(99, result.Answer);
        Assert.Equal(2, stops);
        Assert.Equal(2, replies.Count);
        Assert.False(string.IsNullOrEmpty(replies.First().Data.OriginatingMessageId));
        Assert.Equal(replies.First().Data.OriginatingMessageId, replies.Last().Data.OriginatingMessageId);
        Assert.Equal([42, 99], replies.Select(message =>
            JsonSerializer.Deserialize(message.Data.Content, StructuredOutputE2EJsonContext.Default.CorrectionResult)!.Answer));
    }

    [Fact]
    public async Task Typed_Wait_Returns_Late_Steering_Response()
    {
        var stops = 0;
        string? steeringId = null;
        CopilotSession? session = null;
        var config = StructuredSessionConfig();
        config.Hooks = new SessionHooks
        {
            OnAgentStop = async (_, _) =>
            {
                if (Interlocked.Increment(ref stops) == 1)
                {
                    // The final model request has finished, but this run still admits steering.
                    steeringId = await session!.SendAsync(new MessageOptions
                    {
                        Prompt = "Change the answer to 99. Do not use tools.",
                        Mode = "immediate",
                    });
                }
                return null;
            },
        };
        session = await CreateSessionAsync(config);
        var replies = new System.Collections.Concurrent.ConcurrentQueue<AssistantMessageEvent>();
        using var subscription = session.On<AssistantMessageEvent>(message =>
        {
            if (string.IsNullOrEmpty(message.AgentId)) replies.Enqueue(message);
        });
        var result = await session.SendAndWaitAsync<CorrectionResult>(
            "What is 19 + 23? Do not use tools.",
            StructuredOutputE2EJsonContext.Default.Options,
            TimeSpan.FromMinutes(3));
        Assert.Equal(99, result.Answer);
        Assert.Equal(2, stops);
        Assert.Equal(2, replies.Count);
        Assert.False(string.IsNullOrEmpty(steeringId));
        Assert.False(string.IsNullOrEmpty(replies.First().Data.OriginatingMessageId));
        Assert.NotEqual(steeringId, replies.First().Data.OriginatingMessageId);
        Assert.Equal(replies.First().Data.OriginatingMessageId, replies.Last().Data.OriginatingMessageId);
        Assert.Equal([42, 99], replies.Select(message =>
            JsonSerializer.Deserialize(message.Data.Content, StructuredOutputE2EJsonContext.Default.CorrectionResult)!.Answer));
    }

    [Fact]
    public async Task Typed_Wait_Returns_Stop_Hook_Correction_After_Terminal_Tool()
    {
        var calls = 0;
        var stops = 0;
        var config = StructuredSessionConfig();
        config.Tools =
        [
            CopilotTool.DefineTool(() =>
            {
                Interlocked.Increment(ref calls);
                return 58;
            }, new CopilotToolOptions { IsTerminal = true, SkipPermission = true },
                new() { Name = "lookup_number", Description = "Return the number needed for the calculation." }),
        ];
        config.Hooks = new SessionHooks
        {
            OnAgentStop = (_, _) => Task.FromResult<AgentStopHookOutput?>(
                Interlocked.Increment(ref stops) == 1
                    ? new() { Decision = "block", Reason = "Correct the answer to 99, not 63. Do not use tools." }
                    : null),
        };
        var session = await CreateSessionAsync(config);
        var replies = new System.Collections.Concurrent.ConcurrentQueue<AssistantMessageEvent>();
        using var subscription = session.On<AssistantMessageEvent>(message =>
        {
            if (string.IsNullOrEmpty(message.AgentId)) replies.Enqueue(message);
        });
        var result = await session.SendAndWaitAsync<CorrectionResult>(
            "Call lookup_number exactly once, then add 5 to the returned number. Do not guess its result.",
            StructuredOutputE2EJsonContext.Default.Options,
            TimeSpan.FromMinutes(3));
        Assert.Equal(99, result.Answer);
        Assert.Equal(1, calls);
        Assert.Equal(2, stops);
        var answers = replies.Where(message => message.Data.ToolRequests is not { Length: > 0 }).ToArray();
        Assert.Equal([63, 99], answers.Select(message =>
            JsonSerializer.Deserialize(message.Data.Content, StructuredOutputE2EJsonContext.Default.CorrectionResult)!.Answer));
        Assert.False(string.IsNullOrEmpty(answers[0].Data.OriginatingMessageId));
        Assert.Equal(answers[0].Data.OriginatingMessageId, answers[1].Data.OriginatingMessageId);
        var exchanges = await Ctx.GetExchangesAsync();
        Assert.Equal(3, exchanges.Count);
        Assert.Equal("none", exchanges[1].Request.ToolChoice?.GetString());
    }

    [Fact]
    public async Task Rejects_Unsupported_Or_Oversized_Schemas_Before_Admission()
    {
        var environment = Ctx.GetEnvironment();
        environment["COPILOT_CLI_ENABLED_FEATURE_FLAGS"] = "HYDRAFUSION,HYDRAFUSION_ROLLOUT";
        await using var client = Ctx.CreateClient(environment: environment);
        foreach (var model in new[] { "gpt-4.1", "hydrafusion" })
        {
            var config = StructuredSessionConfig();
            config.Model = model;
            config.OnPermissionRequest = PermissionHandler.ApproveAll;
            await using var session = await client.CreateSessionAsync(config);
            using var schema = JsonDocument.Parse(
                "{\"type\":\"object\",\"description\":\"" +
                (model == "gpt-4.1" ? new string('x', 32 * 1024 * 1024) : "Small schema") + "\"}");
            var message = model == "gpt-4.1" ? "32 MiB" : "HydraFusion";
            var error = await Assert.ThrowsAnyAsync<Exception>(() =>
                session.SendAndWaitAsync(new MessageOptions
                {
                    Prompt = "Must not be admitted",
                    ResponseSchema = schema.RootElement,
                }));
            Assert.Contains(message, error.Message);
            error = await Assert.ThrowsAnyAsync<Exception>(() =>
                session.Rpc.SendMessagesAsync([], responseFormat: new ResponseFormat
                {
                    Type = "json_schema",
                    JsonSchema = new() { Name = "response", Schema = schema.RootElement },
                }));
            Assert.Contains(message, error.Message);
            Assert.Empty((await session.Rpc.Queue.PendingItemsAsync()).Items);
            Assert.DoesNotContain(await session.GetEventsAsync(),
                evt => evt is UserMessageEvent or SessionErrorEvent);
        }
        Assert.Empty(await Ctx.GetExchangesAsync());
    }

    [Fact]
    public async Task Concurrent_Typed_Sends_Return_Their_Own_Results()
    {
        var toolEntered = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseTool = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var config = StructuredSessionConfig();
        config.Tools =
        [
            CopilotTool.DefineTool(async () =>
            {
                toolEntered.TrySetResult();
                await releaseTool.Task;
                return 42;
            }, factoryOptions: new() { Name = "first_number", Description = "Get the number for the first question." }),
        ];
        var session = await CreateSessionAsync(config);
        var serializerOptions = JsonSerializer.IsReflectionEnabledByDefault
            ? null
            : StructuredOutputE2EJsonContext.Default.Options;
        var first = session.SendAndWaitAsync<FirstAnswer>(
            "Call first_number exactly once and report its returned number.",
            serializerOptions,
            TimeSpan.FromMinutes(3));
        try
        {
            var entered = await Task.WhenAny(toolEntered.Task, first).WaitAsync(TimeSpan.FromMinutes(3));
            if (entered == first)
            {
                await first;
                throw new InvalidOperationException("First run completed without calling first_number.");
            }
            const string secondPrompt = "What is 30 + 7? Do not use tools.";
            var second = session.SendAndWaitAsync<SecondAnswer>(
                secondPrompt, serializerOptions, TimeSpan.FromMinutes(3));
            await TestHelper.WaitForConditionAsync(
                async () => (await session.Rpc.Queue.PendingItemsAsync()).Items.Any(
                    item => item.DisplayText.Contains(secondPrompt, StringComparison.Ordinal)),
                timeoutMessage: "Second structured send was not queued behind the tool call.");
            releaseTool.TrySetResult();
            Assert.Equal(42, (await first).First);
            Assert.Equal(37, (await second).Second);
        }
        finally
        {
            releaseTool.TrySetResult();
        }
    }

    public sealed class FirstAnswer
    {
        public required int First { get; set; }
    }

    public sealed class SecondAnswer
    {
        public required int Second { get; set; }
    }

    public sealed class Inventory
    {
        public required int Count { get; set; }
        public required string Color { get; set; }
    }

    public sealed class CorrectionResult
    {
        public required int Answer { get; set; }
    }

    [JsonSourceGenerationOptions(PropertyNamingPolicy = JsonKnownNamingPolicy.CamelCase)]
    [JsonSerializable(typeof(Inventory))]
    [JsonSerializable(typeof(FirstAnswer))]
    [JsonSerializable(typeof(SecondAnswer))]
    [JsonSerializable(typeof(CorrectionResult))]
    internal sealed partial class StructuredOutputE2EJsonContext : JsonSerializerContext;
}
