/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

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

    private static readonly byte[] Png1X1 = Convert.FromBase64String(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

    [Fact]
    public async Task VisionDisabledThenEnabledViaSetModel()
    {
        await File.WriteAllBytesAsync(Path.Combine(Ctx.WorkDir, "test.png"), Png1X1);

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
    public async Task VisionEnabledThenDisabledViaSetModel()
    {
        await File.WriteAllBytesAsync(Path.Combine(Ctx.WorkDir, "test.png"), Png1X1);

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

    /// <summary>
    /// Checks whether any user message contains an image_url content part.
    /// Content can be a string (no images) or a JSON array of content parts.
    /// </summary>
    private static bool HasImageUrlContent(List<ChatCompletionMessage> messages)
    {
        foreach (var message in messages)
        {
            if (message.Role != "user" || message.Content is not { } content)
                continue;

            if (content.ValueKind != JsonValueKind.Array)
                continue;

            foreach (var part in content.EnumerateArray())
            {
                if (part.TryGetProperty("type", out var typeProp) &&
                    typeProp.ValueKind == JsonValueKind.String &&
                    typeProp.GetString() == "image_url")
                {
                    return true;
                }
            }
        }

        return false;
    }
}
