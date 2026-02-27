/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Test.Harness;
using Microsoft.Extensions.AI;
using System.ComponentModel;
using System.Linq;
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

        var session = await CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

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
        var session = await CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(EncryptString, "encrypt_string")],
            OnPermissionRequest = PermissionHandler.ApproveAll,
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

        var session = await CreateSessionAsync(new SessionConfig
        {
            Tools = [getUserLocation],
            OnPermissionRequest = PermissionHandler.ApproveAll,
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
        var session = await CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(PerformDbQuery, "db_query", serializerOptions: ToolsTestsJsonContext.Default.Options)],
            OnPermissionRequest = PermissionHandler.ApproveAll,
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
        var session = await CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(GetImage, "get_image")],
            OnPermissionRequest = PermissionHandler.ApproveAll,
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
    public async Task Invokes_Custom_Tool_With_Permission_Handler()
    {
        var permissionRequests = new List<PermissionRequest>();

        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(EncryptStringForPermission, "encrypt_string")],
            OnPermissionRequest = (request, invocation) =>
            {
                permissionRequests.Add(request);
                return Task.FromResult(new PermissionRequestResult { Kind = "approved" });
            },
        });

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Use encrypt_string to encrypt this string: Hello"
        });

        var assistantMessage = await TestHelper.GetFinalAssistantMessageAsync(session);
        Assert.NotNull(assistantMessage);
        Assert.Contains("HELLO", assistantMessage!.Data.Content ?? string.Empty);

        // Should have received a custom-tool permission request with the correct tool name
        var customToolRequest = permissionRequests.FirstOrDefault(r => r.Kind == "custom-tool");
        Assert.NotNull(customToolRequest);
        Assert.True(customToolRequest!.ExtensionData?.ContainsKey("toolName") ?? false);
        var toolName = ((JsonElement)customToolRequest.ExtensionData!["toolName"]).GetString();
        Assert.Equal("encrypt_string", toolName);

        [Description("Encrypts a string")]
        static string EncryptStringForPermission([Description("String to encrypt")] string input)
            => input.ToUpperInvariant();
    }

    [Fact]
    public async Task Denies_Custom_Tool_When_Permission_Denied()
    {
        var toolHandlerCalled = false;

        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(EncryptStringDenied, "encrypt_string")],
            OnPermissionRequest = (request, invocation) =>
            {
                return Task.FromResult(new PermissionRequestResult { Kind = "denied-interactively-by-user" });
            },
        });

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Use encrypt_string to encrypt this string: Hello"
        });

        await TestHelper.GetFinalAssistantMessageAsync(session);

        // The tool handler should NOT have been called since permission was denied
        Assert.False(toolHandlerCalled);

        [Description("Encrypts a string")]
        string EncryptStringDenied([Description("String to encrypt")] string input)
        {
            toolHandlerCalled = true;
            return input.ToUpperInvariant();
        }
    }
}
