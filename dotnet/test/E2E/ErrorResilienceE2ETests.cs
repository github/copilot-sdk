/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test.E2E;

/// <summary>
/// Verifies the SDK's behavior at the edges of the session lifecycle: sending or
/// reading messages from a disposed session, idempotent abort, and resuming a
/// session that no longer exists. Mirrors
/// <c>nodejs/test/e2e/error_resilience.e2e.test.ts</c>.
/// </summary>
public class ErrorResilienceE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "error_resilience", output)
{
    [Fact]
    public async Task Should_Throw_When_Sending_To_Disconnected_Session()
    {
        var session = await CreateSessionAsync();
        await session.DisposeAsync();

        await Assert.ThrowsAnyAsync<Exception>(() =>
            session.SendAndWaitAsync(new MessageOptions { Prompt = "Hello" }));
    }

    [Fact]
    public async Task Should_Throw_When_Getting_Messages_From_Disconnected_Session()
    {
        var session = await CreateSessionAsync();
        await session.DisposeAsync();

        await Assert.ThrowsAnyAsync<Exception>(() => session.GetMessagesAsync());
    }

    [Fact]
    public async Task Should_Handle_Double_Abort_Without_Error()
    {
        var session = await CreateSessionAsync();

        // First abort should be fine
        await session.AbortAsync();
        // Second abort should not throw
        await session.AbortAsync();

        // Session should still be disposable
        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Throw_When_Resuming_Non_Existent_Session()
    {
        await Assert.ThrowsAnyAsync<Exception>(() =>
            ResumeSessionAsync("non-existent-session-id-12345"));
    }
}
