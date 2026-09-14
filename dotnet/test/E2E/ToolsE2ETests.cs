/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using Microsoft.Extensions.AI;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Text.Json;
using System.Text.Json.Nodes;
using System.Text.Json.Serialization;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public partial class ToolsE2ETests(E2ETestFixture fixture, ITestOutputHelper output) : E2ETestBase(fixture, "tools", output)
{
    private const string ApplyPatchInput = "*** Begin Patch\n*** End Patch";
    private const string ApplyPatchResult = "patched by the host";

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
    public async Task Low_Level_Tool_Definition()
    {
        string currentPhase = string.Empty;

        var session = await CreateSessionAsync(new SessionConfig
        {
            Tools =
            [
                AIFunctionFactory.Create(SetCurrentPhase, new AIFunctionFactoryOptions
                {
                    Name = "set_current_phase",
                    Description = "Sets the current phase of the agent",
                }),
                AIFunctionFactory.Create(SearchItems, new AIFunctionFactoryOptions
                {
                    Name = "search_items",
                    Description = "Search for items by keyword",
                }),
            ],
            AvailableTools = new ToolSet().AddCustom("*").AddBuiltIn("web_fetch"),
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        await session.SendAsync(new MessageOptions
        {
            Prompt = "First, set the current phase to 'analyzing'. Then search for items with keyword 'copilot'. Report the phase and search results."
        });

        var assistantMessage = await TestHelper.GetFinalAssistantMessageAsync(session);

        Assert.NotNull(assistantMessage);
        var content = assistantMessage!.Data.Content ?? string.Empty;
        Assert.NotEmpty(content);
        Assert.Contains("analyzing", content, StringComparison.OrdinalIgnoreCase);
        Assert.True(content.Contains("item_alpha", StringComparison.OrdinalIgnoreCase)
            || content.Contains("item_beta", StringComparison.OrdinalIgnoreCase),
            $"Expected content to mention item_alpha or item_beta, got: {content}");
        Assert.Equal("analyzing", currentPhase);

        Task<string> SetCurrentPhase(string phase)
        {
            currentPhase = phase;
            return Task.FromResult($"Phase set to {phase}");
        }

        Task<string> SearchItems(AIFunctionArguments args)
        {
            Assert.Equal("copilot", args["keyword"]?.ToString());
            return Task.FromResult("Found: item_alpha, item_beta");
        }
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
        Assert.DoesNotContain("Melbourne", toolResult.StringContent);

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
            Assert.Equal([12, 19], query.Ids);
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

    [Fact]
    public async Task Overrides_Built_In_Tool_With_Custom_Tool()
    {
        var session = await CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create((Delegate)CustomGrep, new AIFunctionFactoryOptions
            {
                Name = "grep",
                AdditionalProperties = new ReadOnlyDictionary<string, object?>(
                    new Dictionary<string, object?> { ["is_override"] = true })
            })],
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Use grep to search for the word 'hello'"
        });

        var assistantMessage = await TestHelper.GetFinalAssistantMessageAsync(session);
        Assert.NotNull(assistantMessage);
        Assert.Contains("CUSTOM_GREP_RESULT", assistantMessage!.Data.Content ?? string.Empty);

        [Description("A custom grep implementation that overrides the built-in")]
        static string CustomGrep([Description("Search query")] string query)
            => $"CUSTOM_GREP_RESULT: {query}";
    }

    [Theory]
    [InlineData("string")]
    [InlineData("object")]
    [InlineData("JsonElement")]
    [InlineData("JsonNode")]
    [Trait(E2ETestTraits.Backend, E2ETestTraits.SelfConfiguredBackend)]
    public async Task ApplyPatch_Override_Receives_Freeform_Input_Shapes(string parameterType)
    {
        foreach (var useCustomToolCall in new[] { true, false })
        {
            object? receivedInput = null;
            var invocationCount = 0;
            var handler = new ApplyPatchOverrideRequestHandler(useCustomToolCall);
            await using var client = Ctx.CreateClient(options: new CopilotClientOptions
            {
                Connection = RuntimeConnection.ForStdio(),
                RequestHandler = handler,
            });
            await client.StartAsync();

            string CaptureInput(object input)
            {
                receivedInput = input;
                invocationCount++;
                return ApplyPatchResult;
            }

            Delegate applyPatch = parameterType switch
            {
                "string" => (string input) => CaptureInput(input),
                "object" => (object input) => CaptureInput(input),
                "JsonElement" => (JsonElement input) => CaptureInput(input),
                "JsonNode" => (JsonNode input) => CaptureInput(input),
                _ => throw new ArgumentOutOfRangeException(nameof(parameterType)),
            };
            var tool = CopilotTool.DefineTool(
                applyPatch,
                new CopilotToolOptions
                {
                    OverridesBuiltInTool = true,
                    SkipPermission = true,
                },
                new AIFunctionFactoryOptions
                {
                    Name = "apply_patch",
                    Description = "Host-implemented apply_patch",
                });

            await using var session = await Ctx.CreateSessionAsync(client, new SessionConfig
            {
                Model = "gpt-4o-mini",
                Provider = new ProviderConfig
                {
                    Type = "openai",
                    WireApi = "completions",
                    BaseUrl = "https://apply-patch.invalid/v1",
                    ApiKey = "test-key",
                    ModelId = "gpt-4o-mini",
                    WireModel = "gpt-4o-mini",
                },
                Streaming = true,
                Tools = [tool],
                OnPermissionRequest = PermissionHandler.ApproveAll,
            });

            var message = await session.SendAndWaitAsync(new MessageOptions { Prompt = "Use apply_patch" });

            var input = receivedInput switch
            {
                string value => value,
                JsonElement value => value.GetString(),
                JsonNode value => value.GetValue<string>(),
                _ => null,
            };
            Assert.Equal(ApplyPatchInput, input);
            Assert.Equal(1, invocationCount);
            Assert.Equal("override complete", message?.Data.Content);

            var requests = handler.InferenceRequests;
            Assert.Equal(2, requests.Count);
            AssertApplyPatchOverrideAdvertised(requests[0], parameterType);
            AssertApplyPatchResultReachedModel(requests[1]);
        }
    }

    [Fact]
    public async Task SkipPermission_Sent_In_Tool_Definition()
    {
        [Description("A tool that skips permission")]
        static string SafeLookup([Description("Lookup ID")] string id)
            => $"RESULT: {id}";

        var tool = AIFunctionFactory.Create((Delegate)SafeLookup, new AIFunctionFactoryOptions
        {
            Name = "safe_lookup",
            AdditionalProperties = new ReadOnlyDictionary<string, object?>(
                new Dictionary<string, object?> { ["skip_permission"] = true })
        });

        var didRunPermissionRequest = false;
        var session = await CreateSessionAsync(new SessionConfig
        {
            Tools = [tool],
            OnPermissionRequest = (_, _) =>
            {
                didRunPermissionRequest = true;
                return Task.FromResult<PermissionDecision>(PermissionDecision.NoResult());
            }
        });

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Use safe_lookup to look up 'test123'"
        });

        var assistantMessage = await TestHelper.GetFinalAssistantMessageAsync(session);
        Assert.NotNull(assistantMessage);
        Assert.Contains("RESULT", assistantMessage!.Data.Content ?? string.Empty);
        Assert.False(didRunPermissionRequest);
    }

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

        static ToolResultAIContent GetImage() => new(new()
        {
            BinaryResultsForLlm = [new() {
                // 2x2 yellow square
                Data = "iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAADklEQVR4nGP4/5/h/38GABkAA/0k+7UAAAAASUVORK5CYII=",
                Type = ToolBinaryResultType.Image,
                MimeType = "image/png",
            }],
            SessionLog = "Returned an image",
        });
    }

    [Fact]
    public async Task Invokes_Custom_Tool_With_Permission_Handler()
    {
        var permissionRequests = new List<PermissionRequest>();

        var session = await Ctx.CreateSessionAsync(Client, new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(EncryptStringForPermission, "encrypt_string")],
            OnPermissionRequest = (request, invocation) =>
            {
                permissionRequests.Add(request);
                return Task.FromResult<PermissionDecision>(PermissionDecision.ApproveOnce());
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
        var customToolRequest = permissionRequests.OfType<PermissionRequestCustomTool>().FirstOrDefault();
        Assert.NotNull(customToolRequest);
        Assert.Equal("encrypt_string", customToolRequest!.ToolName);

        [Description("Encrypts a string")]
        static string EncryptStringForPermission([Description("String to encrypt")] string input)
            => input.ToUpperInvariant();
    }

    [Fact]
    public async Task Denies_Custom_Tool_When_Permission_Denied()
    {
        var toolHandlerCalled = false;

        var session = await Ctx.CreateSessionAsync(Client, new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(EncryptStringDenied, "encrypt_string")],
            OnPermissionRequest = async (request, invocation) => PermissionDecision.Reject(),
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

    [Fact]
    public async Task Should_Execute_Multiple_Custom_Tools_In_Parallel_Single_Turn()
    {
        var toolACalled = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);
        var toolBCalled = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);

        var session = await CreateSessionAsync(new SessionConfig
        {
            Tools =
            [
                AIFunctionFactory.Create(LookupCity, "lookup_city"),
                AIFunctionFactory.Create(LookupCountry, "lookup_country"),
            ],
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Use lookup_city with 'Paris' and lookup_country with 'France' at the same time, then combine both results in your reply."
        });

        // Both tools should have been called
        var cityResult = await toolACalled.Task.WaitAsync(TimeSpan.FromSeconds(60));
        var countryResult = await toolBCalled.Task.WaitAsync(TimeSpan.FromSeconds(60));
        Assert.Equal("Paris", cityResult);
        Assert.Equal("France", countryResult);

        var assistantMessage = await TestHelper.GetFinalAssistantMessageAsync(session);
        Assert.NotNull(assistantMessage);
        var content = assistantMessage!.Data.Content ?? string.Empty;
        Assert.Contains("CITY_PARIS", content);
        Assert.Contains("COUNTRY_FRANCE", content);

        [Description("Looks up city information")]
        string LookupCity([Description("City name")] string city)
        {
            toolACalled.TrySetResult(city);
            return $"CITY_{city.ToUpperInvariant()}";
        }

        [Description("Looks up country information")]
        string LookupCountry([Description("Country name")] string country)
        {
            toolBCalled.TrySetResult(country);
            return $"COUNTRY_{country.ToUpperInvariant()}";
        }
    }

    [Fact]
    public async Task Should_Respect_AvailableTools_And_ExcludedTools_Combined()
    {
        bool excludedToolCalled = false;

        var session = await CreateSessionAsync(new SessionConfig
        {
            Tools =
            [
                AIFunctionFactory.Create(AllowedTool, "allowed_tool"),
                AIFunctionFactory.Create(ExcludedTool, "excluded_tool"),
            ],
            AvailableTools = ["allowed_tool", "excluded_tool"],
            ExcludedTools = ["excluded_tool"],
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        var result = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Use the allowed_tool with input 'test'. Do NOT use excluded_tool.",
        });

        Assert.NotNull(result);
        Assert.Contains("ALLOWED_TEST", result!.Data.Content ?? string.Empty);
        Assert.False(excludedToolCalled, "Excluded tool should not have been called");

        [Description("An allowed tool")]
        string AllowedTool([Description("Input value")] string input) => $"ALLOWED_{input.ToUpperInvariant()}";

        [Description("A tool that should be excluded")]
        string ExcludedTool([Description("Input value")] string input)
        {
            excludedToolCalled = true;
            return $"EXCLUDED_{input.ToUpperInvariant()}";
        }
    }

    private static void AssertApplyPatchOverrideAdvertised(string requestBody, string parameterType)
    {
        using var request = JsonDocument.Parse(requestBody);
        var applyPatchTools = request.RootElement.GetProperty("tools")
            .EnumerateArray()
            .Where(tool =>
                tool.TryGetProperty("function", out var function)
                    && function.TryGetProperty("name", out var functionName)
                    && functionName.GetString() == "apply_patch"
                || tool.TryGetProperty("custom", out var custom)
                    && custom.TryGetProperty("name", out var customName)
                    && customName.GetString() == "apply_patch")
            .ToArray();

        var applyPatch = Assert.Single(applyPatchTools);
        Assert.Equal("function", applyPatch.GetProperty("type").GetString());
        var definition = applyPatch.GetProperty("function");
        Assert.Equal("apply_patch", definition.GetProperty("name").GetString());

        var parameters = definition.GetProperty("parameters");
        Assert.Equal("object", parameters.GetProperty("type").GetString());
        var inputSchema = parameters.GetProperty("properties").GetProperty("input");
        if (parameterType == "string")
        {
            Assert.Equal("string", inputSchema.GetProperty("type").GetString());
        }
        else
        {
            Assert.Equal(JsonValueKind.True, inputSchema.ValueKind);
        }
        Assert.Contains(parameters.GetProperty("required").EnumerateArray(), item => item.GetString() == "input");
    }

    private static void AssertApplyPatchResultReachedModel(string requestBody)
    {
        using var request = JsonDocument.Parse(requestBody);
        var toolResult = Assert.Single(
            request.RootElement.GetProperty("messages").EnumerateArray(),
            message =>
                message.GetProperty("role").GetString() == "tool"
                && message.GetProperty("tool_call_id").GetString() == "call-1");
        Assert.Equal(ApplyPatchResult, toolResult.GetProperty("content").GetString());
    }

    private sealed class ApplyPatchOverrideRequestHandler(bool useCustomToolCall) : CopilotRequestHandler
    {
        private const string CustomToolCallResponse =
            "data: {\"id\":\"chatcmpl-tool\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"gpt-4o-mini\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"tool_calls\":[{\"index\":0,\"id\":\"call-1\",\"type\":\"custom\",\"custom\":{\"name\":\"apply_patch\",\"input\":\"*** Begin Patch\\n*** End Patch\"}}]},\"finish_reason\":\"tool_calls\"}]}\n\n" +
            "data: [DONE]\n\n";

        private const string FunctionToolCallResponse =
            "data: {\"id\":\"chatcmpl-tool\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"gpt-4o-mini\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"tool_calls\":[{\"index\":0,\"id\":\"call-1\",\"type\":\"function\",\"function\":{\"name\":\"apply_patch\",\"arguments\":\"{\\\"input\\\":\\\"*** Begin Patch\\\\n*** End Patch\\\"}\"}}]},\"finish_reason\":\"tool_calls\"}]}\n\n" +
            "data: [DONE]\n\n";

        private const string FinalResponse =
            "data: {\"id\":\"chatcmpl-final\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"gpt-4o-mini\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"content\":\"override complete\"},\"finish_reason\":null}]}\n\n" +
            "data: {\"id\":\"chatcmpl-final\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"gpt-4o-mini\",\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}]}\n\n" +
            "data: [DONE]\n\n";

        private readonly object _lock = new();
        private readonly List<string> _inferenceRequests = [];

        internal IReadOnlyList<string> InferenceRequests
        {
            get
            {
                lock (_lock)
                {
                    return [.. _inferenceRequests];
                }
            }
        }

        protected override async Task<HttpResponseMessage> SendRequestAsync(HttpRequestMessage request, CopilotRequestContext ctx)
        {
            var url = request.RequestUri!.ToString();
            if (!RecordingRequestHandler.IsInferenceUrl(url))
            {
                return RecordingRequestHandler.BuildNonInferenceResponse(url);
            }

            var requestBody = request.Content is null
                ? string.Empty
#if NET8_0_OR_GREATER
                : await request.Content.ReadAsStringAsync(ctx.CancellationToken).ConfigureAwait(false);
#else
                : await request.Content.ReadAsStringAsync().ConfigureAwait(false);
#endif

            int requestNumber;
            lock (_lock)
            {
                _inferenceRequests.Add(requestBody);
                requestNumber = _inferenceRequests.Count;
            }

            return requestNumber switch
            {
                1 => Sse(useCustomToolCall ? CustomToolCallResponse : FunctionToolCallResponse),
                2 => Sse(FinalResponse),
                _ => throw new InvalidOperationException($"Unexpected inference request #{requestNumber}."),
            };
        }

        private static HttpResponseMessage Sse(string body) => new(HttpStatusCode.OK)
        {
            Content = new StringContent(body, Encoding.UTF8, "text/event-stream"),
        };
    }
}
