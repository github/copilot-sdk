/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#pragma warning disable GHCP001 // Live tool replacement is intentionally experimental.

using GitHub.Copilot.Test.Harness;
using Microsoft.Extensions.AI;
using System.ComponentModel;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class SetToolsE2ETests(E2ETestFixture fixture, ITestOutputHelper output) : E2ETestBase(fixture, "set_tools", output)
{
    private const string FruitPrompt = "Use lookup_fruit to find the fruit for code 42.";
    private const string FruitAndVegetablePrompt = "Use lookup_fruit to find the fruit for code 42 again, and use lookup_vegetable to find the vegetable for code 7.";
    private const string VegetablePrompt = "Use lookup_vegetable to find the vegetable for code 7.";

    [Fact]
    public async Task Replaces_Tools_On_A_Created_Session()
    {
        var originalLookups = new List<int>();
        var replacementLookups = new List<int>();
        var vegetableLookups = new List<int>();
        var session = await CreateSessionAsync(new SessionConfig
        {
            Tools = [LookupFruit("apple", originalLookups), RetiredLookup()],
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        var first = await TestHelper.SendAndGetFinalAssistantMessageAsync(session, new MessageOptions { Prompt = FruitPrompt });
        Assert.Contains("apple", first?.Data.Content ?? string.Empty);

        await session.SetToolsAsync([LookupFruit("dragonfruit", replacementLookups), LookupVegetable(vegetableLookups)]);

        var second = await TestHelper.SendAndGetFinalAssistantMessageAsync(session, new MessageOptions { Prompt = FruitAndVegetablePrompt });
        Assert.Contains("dragonfruit", second?.Data.Content ?? string.Empty);
        Assert.Contains("carrot", second?.Data.Content ?? string.Empty);
        Assert.Equal([42], originalLookups);
        Assert.Equal([42], replacementLookups);
        Assert.Equal([7], vegetableLookups);

        // Model requests after the replacement offer exactly the new tool set.
        var exchanges = await Ctx.GetExchangesAsync();
        var replacedFrom = IndexOfPrompt(exchanges, FruitAndVegetablePrompt);
        Assert.True(replacedFrom > 0, "Expected model requests before and after the replacement.");
        Assert.All(exchanges.Take(replacedFrom), exchange =>
        {
            var tools = GetToolNames(exchange);
            Assert.Contains("lookup_fruit", tools);
            Assert.Contains("retired_lookup", tools);
            Assert.DoesNotContain("lookup_vegetable", tools);
        });
        Assert.All(exchanges.Skip(replacedFrom), exchange =>
        {
            var tools = GetToolNames(exchange);
            Assert.Contains("lookup_fruit", tools);
            Assert.Contains("lookup_vegetable", tools);
            Assert.DoesNotContain("retired_lookup", tools);
        });
    }

    [Fact]
    public async Task Replaces_Tools_On_A_Resumed_Session()
    {
        var createdLookups = new List<int>();
        var created = await CreateSessionAsync(new SessionConfig
        {
            Tools = [LookupFruit("apple", createdLookups)],
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });
        var sessionId = created.SessionId;
        var first = await TestHelper.SendAndGetFinalAssistantMessageAsync(created, new MessageOptions { Prompt = FruitPrompt });
        Assert.Contains("apple", first?.Data.Content ?? string.Empty);
        Assert.Equal([42], createdLookups);
        await created.DisposeAsync();

        var fruitLookups = new List<int>();
        var vegetableLookups = new List<int>();
        var resumed = await Ctx.ResumeSessionAsync(Client, sessionId, new ResumeSessionConfig
        {
            Tools = [LookupFruit("apple", fruitLookups)],
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });
        await resumed.SetToolsAsync([LookupVegetable(vegetableLookups)]);

        var answer = await TestHelper.SendAndGetFinalAssistantMessageAsync(resumed, new MessageOptions { Prompt = VegetablePrompt });
        Assert.Contains("carrot", answer?.Data.Content ?? string.Empty);
        Assert.Equal([7], vegetableLookups);
        Assert.Empty(fruitLookups);

        var exchanges = await Ctx.GetExchangesAsync();
        var replacedFrom = IndexOfPrompt(exchanges, VegetablePrompt);
        Assert.True(replacedFrom > 0, "Expected model requests before and after the replacement.");
        Assert.All(exchanges.Skip(replacedFrom), exchange =>
        {
            var tools = GetToolNames(exchange);
            Assert.Contains("lookup_vegetable", tools);
            Assert.DoesNotContain("lookup_fruit", tools);
        });
    }

    [Fact]
    public async Task Keeps_The_Previous_Tools_When_A_Replacement_Is_Rejected()
    {
        var originalLookups = new List<int>();
        var replacementLookups = new List<int>();
        var session = await CreateSessionAsync(new SessionConfig
        {
            Tools = [LookupFruit("apple", originalLookups)],
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        await Assert.ThrowsAnyAsync<Exception>(() => session.SetToolsAsync([
            LookupFruit("dragonfruit", replacementLookups),
            InvalidTool(),
        ]));

        var answer = await TestHelper.SendAndGetFinalAssistantMessageAsync(session, new MessageOptions { Prompt = FruitPrompt });
        Assert.Contains("apple", answer?.Data.Content ?? string.Empty);
        Assert.Equal([42], originalLookups);
        Assert.Empty(replacementLookups);
    }

    /// <summary>Returns the index of the first model request that carries <paramref name="prompt"/> as a user message.</summary>
    private static int IndexOfPrompt(List<ParsedHttpExchange> exchanges, string prompt)
    {
        return exchanges.FindIndex(exchange => exchange.Request.Messages.Any(message =>
            message.Role == "user" && (message.Content?.ToString() ?? string.Empty).Contains(prompt, StringComparison.Ordinal)));
    }

    private static AIFunction LookupFruit(string fruit, List<int>? calls = null)
    {
        string Handler([Description("Fruit code")] int code)
        {
            calls?.Add(code);
            return fruit;
        }

        return AIFunctionFactory.Create((Func<int, string>)Handler, new AIFunctionFactoryOptions
        {
            Name = "lookup_fruit",
            Description = "Looks up the fruit for a numeric code",
        });
    }

    private static AIFunction LookupVegetable(List<int>? calls = null)
    {
        string Handler([Description("Vegetable code")] int code)
        {
            calls?.Add(code);
            return "carrot";
        }

        return AIFunctionFactory.Create((Func<int, string>)Handler, new AIFunctionFactoryOptions
        {
            Name = "lookup_vegetable",
            Description = "Looks up the vegetable for a numeric code",
        });
    }

    private static AIFunction RetiredLookup()
    {
        return AIFunctionFactory.Create(() => "retired", new AIFunctionFactoryOptions
        {
            Name = "retired_lookup",
            Description = "Looks up a retired value",
        });
    }

    private static AIFunction InvalidTool()
    {
        return AIFunctionFactory.Create(() => "never", new AIFunctionFactoryOptions
        {
            Name = "invalid.tool",
            Description = "Has a name the runtime rejects",
        });
    }
}

#pragma warning restore GHCP001
