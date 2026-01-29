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

public partial class ToolsTests(E2ETestFixture fixture, ITestOutputHelper output) : E2ETestBase(fixture, "tools", output)
{
    [Fact]
    public async Task Invokes_Built_In_Tools()
    {
        await File.WriteAllTextAsync(
            Path.Combine(Ctx.WorkDir, "README.md"),
            "# ELIZA, the only chatbot you'll ever need");

        var session = await Client.CreateSessionAsync();

        await session.SendAsync(new MessageOptions
        {
            Prompt = "What's the first line of README.md in this directory?"
        });

        var assistantMessage = await TestHelper.GetFinalAssistantMessageAsync(session);
        Assert.NotNull(assistantMessage);
        Assert.Contains("ELIZA", assistantMessage!.Data.Content ?? string.Empty);
    }

    [Fact]
    public async Task Invokes_Custom_Tool()
    {
        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(EncryptString, "encrypt_string")],
        });

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Use encrypt_string to encrypt this string: Hello"
        });

        var assistantMessage = await TestHelper.GetFinalAssistantMessageAsync(session);
        Assert.NotNull(assistantMessage);
        Assert.Contains("HELLO", assistantMessage!.Data.Content ?? string.Empty);

        [Description("Encrypts a string")]
        static string EncryptString([Description("String to encrypt")] string input)
            => input.ToUpperInvariant();
    }

    [Fact]
    public async Task Handles_Tool_Calling_Errors()
    {
        var getUserLocation = AIFunctionFactory.Create(
            () => { throw new Exception("Melbourne"); }, "get_user_location", "Gets the user's location");

        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            Tools = [getUserLocation]
        });

        await session.SendAsync(new MessageOptions { Prompt = "What is my location? If you can't find out, just say 'unknown'." });
        var answer = await TestHelper.GetFinalAssistantMessageAsync(session);

        // Check the underlying traffic
        var traffic = await Ctx.GetExchangesAsync();
        var lastConversation = traffic[^1];

        var toolCalls = lastConversation.Request.Messages
            .Where(m => m.Role == "assistant" && m.ToolCalls != null)
            .SelectMany(m => m.ToolCalls!)
            .ToList();

        Assert.Single(toolCalls);
        var toolCall = toolCalls[0];
        Assert.Equal("function", toolCall.Type);
        Assert.Equal("get_user_location", toolCall.Function.Name);

        var toolResults = lastConversation.Request.Messages
            .Where(m => m.Role == "tool")
            .ToList();

        Assert.Single(toolResults);
        var toolResult = toolResults[0];
        Assert.Equal(toolCall.Id, toolResult.ToolCallId);
        Assert.DoesNotContain("Melbourne", toolResult.Content);

        // Importantly, we're checking that the assistant does not see the
        // exception information as if it was the tool's output.
        Assert.DoesNotContain("Melbourne", answer?.Data.Content);
        Assert.Contains("unknown", answer?.Data.Content?.ToLowerInvariant());
    }

    [Fact]
    public async Task Can_Receive_And_Return_Complex_Types()
    {
        ToolInvocation? receivedInvocation = null;
        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(PerformDbQuery, "db_query", serializerOptions: ToolsTestsJsonContext.Default.Options)],
        });

        await session.SendAsync(new MessageOptions
        {
            Prompt =
                "Perform a DB query for the 'cities' table using IDs 12 and 19, sorting ascending. " +
                "Reply only with lines of the form: [cityname] [population]"
        });

        var assistantMessage = await TestHelper.GetFinalAssistantMessageAsync(session);
        var responseContent = assistantMessage?.Data.Content!;
        Assert.NotNull(assistantMessage);
        Assert.NotEmpty(responseContent);
        Assert.Contains("Passos", responseContent);
        Assert.Contains("San Lorenzo", responseContent);
        Assert.Contains("135460", responseContent.Replace(",", ""));
        Assert.Contains("204356", responseContent.Replace(",", ""));

        // We can access the raw invocation if needed
        Assert.Equal(session.SessionId, receivedInvocation!.SessionId);

        City[] PerformDbQuery(DbQueryOptions query, AIFunctionArguments rawArgs)
        {
            Assert.Equal("cities", query.Table);
            Assert.Equal(new[] { 12, 19 }, query.Ids);
            Assert.True(query.SortAscending);
            receivedInvocation = (ToolInvocation)rawArgs.Context![typeof(ToolInvocation)]!;
            return [new(19, "Passos", 135460), new(12, "San Lorenzo", 204356)];
        }
    }

    record DbQueryOptions(string Table, int[] Ids, bool SortAscending);
    record City(int CountryId, string CityName, int Population);

    [JsonSourceGenerationOptions(JsonSerializerDefaults.Web)]
    [JsonSerializable(typeof(DbQueryOptions))]
    [JsonSerializable(typeof(City[]))]
    [JsonSerializable(typeof(JsonElement))]
    private partial class ToolsTestsJsonContext : JsonSerializerContext;

    [Fact(Skip = "Behaves as if no content was in the result. Likely that binary results aren't fully implemented yet.")]
    public async Task Can_Return_Binary_Result()
    {
        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(GetImage, "get_image")],
        });

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Use get_image. What color is the square in the image?"
        });

        var assistantMessage = await TestHelper.GetFinalAssistantMessageAsync(session);
        Assert.NotNull(assistantMessage);

        Assert.Contains("yellow", assistantMessage!.Data.Content?.ToLowerInvariant() ?? string.Empty);

        static ToolResultAIContent GetImage() => new ToolResultAIContent(new()
        {
            BinaryResultsForLlm = [new() {
                // 2x2 yellow square
                Data = "iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAADklEQVR4nGP4/5/h/38GABkAA/0k+7UAAAAASUVORK5CYII=",
                Type = "base64",
                MimeType = "image/png",
            }],
            SessionLog = "Returned an image",
        });
    }

    [Fact]
    public async Task Requests_Permission_For_Tools_With_RequiresApproval()
    {
        var permissionRequested = false;
        string? permissionToolName = null;

        var getWeather = new CopilotTool
        {
            Function = AIFunctionFactory.Create(
                ([Description("The city name")] string city) =>
                {
                    return new { city, temperature = "72°F", condition = "sunny" };
                },
                "get_weather",
                "Get the current weather for a city"),
            RequiresApproval = true
        };

        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            Tools = [getWeather],
            OnPermissionRequest = (request, invocation) =>
            {
                if (request.Kind == "tool")
                {
                    permissionRequested = true;
                    permissionToolName = request.ExtensionData?.GetValueOrDefault("toolName")?.ToString();
                }
                return Task.FromResult(new PermissionRequestResult { Kind = "approved" });
            }
        });

        await session.SendAsync(new MessageOptions
        {
            Prompt = "What's the weather in Seattle?"
        });

        var assistantMessage = await TestHelper.GetFinalAssistantMessageAsync(session);

        Assert.True(permissionRequested, "Permission should have been requested");
        Assert.Equal("get_weather", permissionToolName);
        Assert.Contains("72", assistantMessage?.Data.Content ?? string.Empty);
    }

    [Fact]
    public async Task Denies_Tool_Execution_When_Permission_Denied()
    {
        var deleteFile = new CopilotTool
        {
            Function = AIFunctionFactory.Create(
                ([Description("File path")] string path) => $"Deleted {path}",
                "delete_file",
                "Deletes a file"),
            RequiresApproval = true
        };

        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            Tools = [deleteFile],
            OnPermissionRequest = (request, invocation) =>
            {
                if (request.Kind == "tool")
                {
                    return Task.FromResult(new PermissionRequestResult
                    {
                        Kind = "denied-interactively-by-user"
                    });
                }
                return Task.FromResult(new PermissionRequestResult { Kind = "approved" });
            }
        });

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Delete the file test.txt"
        });

        var assistantMessage = await TestHelper.GetFinalAssistantMessageAsync(session);
        var content = assistantMessage?.Data.Content?.ToLowerInvariant() ?? string.Empty;

        Assert.True(
            content.Contains("denied") || content.Contains("cannot") || content.Contains("unable"),
            "Assistant should indicate the tool was denied");
    }

    [Fact]
    public async Task Executes_Tools_Without_Permission_When_RequiresApproval_False()
    {
        var permissionRequested = false;

        var addNumbers = new CopilotTool
        {
            Function = AIFunctionFactory.Create(
                (int a, int b) => a + b,
                "add_numbers",
                "Adds two numbers"),
            RequiresApproval = false
        };

        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            Tools = [addNumbers],
            OnPermissionRequest = (request, invocation) =>
            {
                if (request.Kind == "tool")
                {
                    permissionRequested = true;
                }
                return Task.FromResult(new PermissionRequestResult { Kind = "approved" });
            }
        });

        await session.SendAsync(new MessageOptions
        {
            Prompt = "What is 5 + 3?"
        });

        var assistantMessage = await TestHelper.GetFinalAssistantMessageAsync(session);

        Assert.False(permissionRequested, "Permission should not have been requested");
        Assert.Contains("8", assistantMessage?.Data.Content ?? string.Empty);
    }
}
