/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test.E2E;

/// <summary>
/// Verifies that information produced in one turn (e.g., the contents of a file
/// just read or written) is available to subsequent turns in the same session.
/// Mirrors <c>nodejs/test/e2e/multi_turn.e2e.test.ts</c>.
/// </summary>
public class MultiTurnE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "multi_turn", output)
{
    [Fact]
    public async Task Should_Use_Tool_Results_From_Previous_Turns()
    {
        // Write a file, then ask the model to read it and reason about its content
        await File.WriteAllTextAsync(Path.Join(Ctx.WorkDir, "secret.txt"), "The magic number is 42.");
        var session = await CreateSessionAsync();

        var msg1 = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Read the file 'secret.txt' and tell me what the magic number is.",
        });
        Assert.Contains("42", msg1?.Data.Content ?? string.Empty);

        // Follow-up that requires context from the previous turn
        var msg2 = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "What is that magic number multiplied by 2?",
        });
        Assert.Contains("84", msg2?.Data.Content ?? string.Empty);
    }

    [Fact]
    public async Task Should_Handle_File_Creation_Then_Reading_Across_Turns()
    {
        var session = await CreateSessionAsync();

        // First turn: create a file
        await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Create a file called 'greeting.txt' with the content 'Hello from multi-turn test'.",
        });

        // Second turn: read the file
        var msg = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Read the file 'greeting.txt' and tell me its exact contents.",
        });
        Assert.Contains("Hello from multi-turn test", msg?.Data.Content ?? string.Empty);
    }
}
