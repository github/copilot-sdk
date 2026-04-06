/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Test.Harness;
using Microsoft.Extensions.AI;
using System.ComponentModel;
using System.Text.Json;
using System.Text.Json.Serialization;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public partial class ToolResultsTests(E2ETestFixture fixture, ITestOutputHelper output) : E2ETestBase(fixture, "tool_results", output)
{
    [JsonSourceGenerationOptions(JsonSerializerDefaults.Web)]
    [JsonSerializable(typeof(ToolResultAIContent))]
    [JsonSerializable(typeof(ToolResultObject))]
    [JsonSerializable(typeof(JsonElement))]
    private partial class ToolResultsJsonContext : JsonSerializerContext;

    [Fact]
    public async Task Should_Handle_Structured_ToolResultObject_From_Custom_Tool()
    {
        var session = await CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(GetWeather, "get_weather", serializerOptions: ToolResultsJsonContext.Default.Options)],
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        await session.SendAsync(new MessageOptions
        {
            Prompt = "What's the weather in Paris?"
        });

        var assistantMessage = await TestHelper.GetFinalAssistantMessageAsync(session);
        Assert.NotNull(assistantMessage);
        Assert.Matches("(?i)sunny|72", assistantMessage!.Data.Content ?? string.Empty);

        [Description("Gets weather for a city")]
        static ToolResultAIContent GetWeather([Description("City name")] string city)
            => new(new()
            {
                TextResultForLlm = $"The weather in {city} is sunny and 72°F",
                ResultType = "success",
            });
    }

    [Fact]
    public async Task Should_Handle_Tool_Result_With_Failure_ResultType()
    {
        var session = await CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(CheckStatus, "check_status", serializerOptions: ToolResultsJsonContext.Default.Options)],
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Check the status of the service using check_status. If it fails, say 'service is down'."
        });

        var assistantMessage = await TestHelper.GetFinalAssistantMessageAsync(session);
        Assert.NotNull(assistantMessage);
        Assert.Contains("service is down", assistantMessage!.Data.Content?.ToLowerInvariant() ?? string.Empty);

        [Description("Checks the status of a service")]
        static ToolResultAIContent CheckStatus()
            => new(new()
            {
                TextResultForLlm = "Service unavailable",
                ResultType = "failure",
                Error = "API timeout",
            });
    }

    [Fact]
    public async Task Should_Preserve_ToolTelemetry_And_Not_Stringify_Structured_Results_For_LLM()
    {
        var session = await CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(AnalyzeCode, "analyze_code", serializerOptions: ToolResultsJsonContext.Default.Options)],
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Analyze the file main.ts for issues."
        });

        var assistantMessage = await TestHelper.GetFinalAssistantMessageAsync(session);
        Assert.NotNull(assistantMessage);
        Assert.Contains("no issues", assistantMessage!.Data.Content?.ToLowerInvariant() ?? string.Empty);

        // Verify the LLM received just textResultForLlm, not stringified JSON
        var traffic = await Ctx.GetExchangesAsync();
        var lastConversation = traffic[^1];

        var toolResults = lastConversation.Request.Messages
            .Where(m => m.Role == "tool")
            .ToList();

        Assert.Single(toolResults);
        Assert.DoesNotContain("toolTelemetry", toolResults[0].StringContent);
        Assert.DoesNotContain("resultType", toolResults[0].StringContent);

        [Description("Analyzes code for issues")]
        static ToolResultAIContent AnalyzeCode([Description("File to analyze")] string file)
            => new(new()
            {
                TextResultForLlm = $"Analysis of {file}: no issues found",
                ResultType = "success",
                ToolTelemetry = new Dictionary<string, object>
                {
                    ["metrics"] = new Dictionary<string, object> { ["analysisTimeMs"] = 150 },
                    ["properties"] = new Dictionary<string, object> { ["analyzer"] = "eslint" },
                },
            });
    }
}
