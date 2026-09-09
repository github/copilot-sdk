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
    : E2ETestBase(fixture, "structured_output_dotnet", output)
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
        var completion = new TaskCompletionSource<AssistantMessageEvent>(TaskCreationOptions.RunContinuationsAsynchronously);
        using var subscription = session.On<AssistantMessageEvent>(message =>
        {
            if (message.Data.ToolRequests is not { Length: > 0 })
            {
                completion.TrySetResult(message);
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
        var message = await completion.Task.WaitAsync(cts.Token);
        Assert.Equal(accepted.MessageIds.Last(), message.Data.OriginatingMessageId);
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

    [JsonSourceGenerationOptions(PropertyNamingPolicy = JsonKnownNamingPolicy.CamelCase)]
    [JsonSerializable(typeof(Inventory))]
    [JsonSerializable(typeof(FirstAnswer))]
    [JsonSerializable(typeof(SecondAnswer))]
    internal sealed partial class StructuredOutputE2EJsonContext : JsonSerializerContext;
}
