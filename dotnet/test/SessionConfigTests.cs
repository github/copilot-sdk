/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Linq;
using System.Text.Json;
using GitHub.Copilot.SDK.Rpc;
using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public class SessionConfigTests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "session_config", output)
{
    private const string ViewImagePrompt = "Use the view tool to look at the file test.png and describe what you see";
    private const string ProviderHeaderName = "x-copilot-sdk-provider-header";
    private const string ClientName = "csharp-public-surface-client";

    private static readonly byte[] Png1X1 = Convert.FromBase64String(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

    [Fact]
    public async Task Vision_Disabled_Then_Enabled_Via_SetModel()
    {
        await File.WriteAllBytesAsync(Path.Join(Ctx.WorkDir, "test.png"), Png1X1);

        var session = await CreateSessionAsync(new SessionConfig
        {
            Model = "claude-sonnet-4.5",
            ModelCapabilities = new ModelCapabilitiesOverride
            {
                Supports = new ModelCapabilitiesOverrideSupports { Vision = false },
            },
        });

        // Turn 1: vision off — no image_url expected
        await session.SendAndWaitAsync(new MessageOptions { Prompt = ViewImagePrompt });
        var trafficAfterT1 = await Ctx.GetExchangesAsync();
        var t1Messages = trafficAfterT1.SelectMany(e => e.Request.Messages).ToList();
        Assert.False(HasImageUrlContent(t1Messages), "Expected no image_url content when vision is disabled");

        // Switch vision on
        await session.SetModelAsync(
            "claude-sonnet-4.5",
            reasoningEffort: null,
            modelCapabilities: new ModelCapabilitiesOverride
            {
                Supports = new ModelCapabilitiesOverrideSupports { Vision = true },
            });

        // Turn 2: vision on — image_url expected
        await session.SendAndWaitAsync(new MessageOptions { Prompt = ViewImagePrompt });
        var trafficAfterT2 = await Ctx.GetExchangesAsync();
        var newExchanges = trafficAfterT2.Skip(trafficAfterT1.Count).ToList();
        Assert.NotEmpty(newExchanges);
        var t2Messages = newExchanges.SelectMany(e => e.Request.Messages).ToList();
        Assert.True(HasImageUrlContent(t2Messages), "Expected image_url content when vision is enabled");

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Vision_Enabled_Then_Disabled_Via_SetModel()
    {
        await File.WriteAllBytesAsync(Path.Join(Ctx.WorkDir, "test.png"), Png1X1);

        var session = await CreateSessionAsync(new SessionConfig
        {
            Model = "claude-sonnet-4.5",
            ModelCapabilities = new ModelCapabilitiesOverride
            {
                Supports = new ModelCapabilitiesOverrideSupports { Vision = true },
            },
        });

        // Turn 1: vision on — image_url expected
        await session.SendAndWaitAsync(new MessageOptions { Prompt = ViewImagePrompt });
        var trafficAfterT1 = await Ctx.GetExchangesAsync();
        var t1Messages = trafficAfterT1.SelectMany(e => e.Request.Messages).ToList();
        Assert.True(HasImageUrlContent(t1Messages), "Expected image_url content when vision is enabled");

        // Switch vision off
        await session.SetModelAsync(
            "claude-sonnet-4.5",
            reasoningEffort: null,
            modelCapabilities: new ModelCapabilitiesOverride
            {
                Supports = new ModelCapabilitiesOverrideSupports { Vision = false },
            });

        // Turn 2: vision off — no image_url expected in new exchanges
        await session.SendAndWaitAsync(new MessageOptions { Prompt = ViewImagePrompt });
        var trafficAfterT2 = await Ctx.GetExchangesAsync();
        var newExchanges = trafficAfterT2.Skip(trafficAfterT1.Count).ToList();
        Assert.NotEmpty(newExchanges);
        var t2Messages = newExchanges.SelectMany(e => e.Request.Messages).ToList();
        Assert.False(HasImageUrlContent(t2Messages), "Expected no image_url content when vision is disabled");

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Use_Custom_SessionId()
    {
        var requestedSessionId = Guid.NewGuid().ToString();

        var session = await CreateSessionAsync(new SessionConfig
        {
            SessionId = requestedSessionId,
        });

        Assert.Equal(requestedSessionId, session.SessionId);

        var messages = await session.GetMessagesAsync();
        var startEvent = Assert.IsType<SessionStartEvent>(messages[0]);
        Assert.Equal(requestedSessionId, startEvent.Data.SessionId);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Forward_ClientName_In_UserAgent()
    {
        var session = await CreateSessionAsync(new SessionConfig
        {
            ClientName = ClientName,
        });

        await session.SendAndWaitAsync(new MessageOptions { Prompt = "What is 1+1?" });

        var exchange = Assert.Single(await Ctx.GetExchangesAsync());
        AssertHeaderContains(exchange.RequestHeaders, "user-agent", ClientName);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Forward_Custom_Provider_Headers_On_Create()
    {
        var session = await CreateSessionAsync(new SessionConfig
        {
            Model = "claude-sonnet-4.5",
            Provider = CreateProxyProvider("create-provider-header"),
        });

        var message = await session.SendAndWaitAsync(new MessageOptions { Prompt = "What is 1+1?" });
        Assert.Contains("2", message?.Data.Content ?? string.Empty);

        var exchange = Assert.Single(await Ctx.GetExchangesAsync());
        AssertHeaderContains(exchange.RequestHeaders, "authorization", "Bearer test-provider-key");
        AssertHeaderContains(exchange.RequestHeaders, ProviderHeaderName, "create-provider-header");

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Forward_Custom_Provider_Headers_On_Resume()
    {
        var session1 = await CreateSessionAsync();
        var sessionId = session1.SessionId;

        var session2 = await ResumeSessionAsync(sessionId, new ResumeSessionConfig
        {
            Model = "claude-sonnet-4.5",
            Provider = CreateProxyProvider("resume-provider-header"),
        });

        var message = await session2.SendAndWaitAsync(new MessageOptions { Prompt = "What is 2+2?" });
        Assert.Contains("4", message?.Data.Content ?? string.Empty);

        var exchange = Assert.Single(await Ctx.GetExchangesAsync());
        AssertHeaderContains(exchange.RequestHeaders, "authorization", "Bearer test-provider-key");
        AssertHeaderContains(exchange.RequestHeaders, ProviderHeaderName, "resume-provider-header");

        await session2.DisposeAsync();
    }

    [Fact]
    public async Task Should_Use_WorkingDirectory_For_Tool_Execution()
    {
        var subDir = Path.Join(Ctx.WorkDir, "subproject");
        Directory.CreateDirectory(subDir);
        await File.WriteAllTextAsync(Path.Join(subDir, "marker.txt"), "I am in the subdirectory");

        var session = await CreateSessionAsync(new SessionConfig
        {
            WorkingDirectory = subDir,
        });

        var message = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Read the file marker.txt and tell me what it says",
        });

        Assert.Contains("subdirectory", message?.Data.Content ?? string.Empty);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Apply_WorkingDirectory_On_Session_Resume()
    {
        var subDir = Path.Join(Ctx.WorkDir, "resume-subproject");
        Directory.CreateDirectory(subDir);
        await File.WriteAllTextAsync(Path.Join(subDir, "resume-marker.txt"), "I am in the resume working directory");

        var session1 = await CreateSessionAsync();
        var sessionId = session1.SessionId;

        var session2 = await ResumeSessionAsync(sessionId, new ResumeSessionConfig
        {
            WorkingDirectory = subDir,
        });

        var message = await session2.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Read the file resume-marker.txt and tell me what it says",
        });

        Assert.Contains("resume working directory", message?.Data.Content ?? string.Empty);

        await session2.DisposeAsync();
    }

    [Fact]
    public async Task Should_Apply_SystemMessage_On_Session_Resume()
    {
        var session1 = await CreateSessionAsync();
        var sessionId = session1.SessionId;

        var resumeInstruction = "End the response with RESUME_SYSTEM_MESSAGE_SENTINEL.";
        var session2 = await ResumeSessionAsync(sessionId, new ResumeSessionConfig
        {
            SystemMessage = new SystemMessageConfig
            {
                Mode = SystemMessageMode.Append,
                Content = resumeInstruction,
            },
        });

        var message = await session2.SendAndWaitAsync(new MessageOptions { Prompt = "What is 1+1?" });
        Assert.Contains("RESUME_SYSTEM_MESSAGE_SENTINEL", message?.Data.Content ?? string.Empty);

        var exchange = Assert.Single(await Ctx.GetExchangesAsync());
        Assert.Contains(resumeInstruction, GetSystemMessage(exchange));

        await session2.DisposeAsync();
    }

    [Fact]
    public async Task Should_Apply_AvailableTools_On_Session_Resume()
    {
        var session1 = await CreateSessionAsync();
        var sessionId = session1.SessionId;

        var session2 = await ResumeSessionAsync(sessionId, new ResumeSessionConfig
        {
            AvailableTools = ["view"],
        });

        await session2.SendAndWaitAsync(new MessageOptions { Prompt = "What is 1+1?" });

        var exchange = Assert.Single(await Ctx.GetExchangesAsync());
        Assert.Equal(["view"], GetToolNames(exchange));

        await session2.DisposeAsync();
    }

    /// <summary>
    /// Checks whether any user message contains an image_url content part.
    /// Content can be a string (no images) or a JSON array of content parts.
    /// </summary>
    private static bool HasImageUrlContent(List<ChatCompletionMessage> messages)
    {
        return messages
            .Where(m => m.Role == "user" && m.Content is { ValueKind: JsonValueKind.Array })
            .Any(m => m.Content!.Value.EnumerateArray().Any(part =>
                part.TryGetProperty("type", out var typeProp) &&
                typeProp.ValueKind == JsonValueKind.String &&
                typeProp.GetString() == "image_url"));
    }

    private ProviderConfig CreateProxyProvider(string headerValue)
    {
        return new ProviderConfig
        {
            Type = "openai",
            BaseUrl = Ctx.ProxyUrl,
            ApiKey = "test-provider-key",
            Headers = new Dictionary<string, string>
            {
                [ProviderHeaderName] = headerValue,
            },
        };
    }

    private static void AssertHeaderContains(
        Dictionary<string, JsonElement>? headers,
        string expectedName,
        string expectedValue)
    {
        Assert.NotNull(headers);
        var header = headers.FirstOrDefault(
            pair => string.Equals(pair.Key, expectedName, StringComparison.OrdinalIgnoreCase));

        var actualHeaders = string.Join(", ", headers.Select(pair => $"{pair.Key}={HeaderValueAsString(pair.Value)}"));
        Assert.False(
            string.IsNullOrEmpty(header.Key),
            $"Expected header '{expectedName}' to be present. Actual headers: {actualHeaders}");
        Assert.Contains(expectedValue, HeaderValueAsString(header.Value), StringComparison.Ordinal);
    }

    private static string HeaderValueAsString(JsonElement value)
    {
        return value.ValueKind switch
        {
            JsonValueKind.String => value.GetString() ?? string.Empty,
            JsonValueKind.Array => string.Join(",", value.EnumerateArray().Select(HeaderValueAsString)),
            _ => value.ToString(),
        };
    }
}
