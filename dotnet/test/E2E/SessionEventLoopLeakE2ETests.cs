/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Test.Harness;
using GitHub.Copilot.Rpc;
using System.Collections;
using System.Reflection;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

/// <summary>
/// Regression coverage for the goroutine/task leak fixed alongside
/// github/copilot-sdk#2360: <see cref="CopilotClient.CreateSessionAsync"/> and
/// <see cref="CopilotClient.ResumeSessionAsync"/> construct a <see cref="CopilotSession"/>
/// and start its event-dispatch consumer (<c>ProcessEventsAsync</c>) eagerly, before the
/// CLI confirms the session, so the CLI can route session-scoped requests to it while
/// session.create (or session.resume) is still being processed. Every failure path must
/// stop that consumer — otherwise each failed call leaks a background task forever, since
/// no caller ever receives the failed session to dispose it. Mirrors
/// <c>go/internal/e2e/session_event_loop_leak_e2e_test.go</c>.
/// </summary>
[Trait(E2ETestTraits.Backend, E2ETestTraits.CapiOnly)]
public class SessionEventLoopLeakE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "session-event-loop-leak", output)
{
    private static readonly FieldInfo SessionsField =
        typeof(CopilotClient).GetField("_sessions", BindingFlags.Instance | BindingFlags.NonPublic)
        ?? throw new InvalidOperationException("CopilotClient._sessions was not found.");

    private static readonly FieldInfo EventChannelField =
        typeof(CopilotSession).GetField("_eventChannel", BindingFlags.Instance | BindingFlags.NonPublic)
        ?? throw new InvalidOperationException("CopilotSession._eventChannel was not found.");

    private static IDictionary GetSessionsMap(CopilotClient client) =>
        (IDictionary)SessionsField.GetValue(client)!;

    private static bool IsEventChannelClosed(CopilotSession session)
    {
        var channel = EventChannelField.GetValue(session)!;
        var readerProperty = channel.GetType().GetProperty("Reader")
            ?? throw new InvalidOperationException("Channel<T>.Reader was not found.");
        var reader = readerProperty.GetValue(channel)!;
        var completionProperty = reader.GetType().GetProperty("Completion")
            ?? throw new InvalidOperationException("ChannelReader<T>.Completion was not found.");
        var completion = (Task)completionProperty.GetValue(reader)!;
        return completion.IsCompleted;
    }

    private static SessionFsConfig CreateSessionFsConfig() => new()
    {
        InitialWorkingDirectory = "/",
        SessionStatePath = "/session-state",
        Conventions = SessionFsSetProviderConventions.Posix,
    };

    [Fact]
    public async Task CreateSessionAsync_Failure_Does_Not_Leak_The_Session_Or_Its_Event_Loop()
    {
        // An invalid per-session GitHub token, redirected at the replaying proxy, makes
        // the real CLI reject session.create with a genuine RPC error (401 Unauthorized)
        // — the same failure path a real user would hit, not a mocked transport.
        var env = new Dictionary<string, string>(Ctx.GetEnvironment())
        {
            ["COPILOT_DEBUG_GITHUB_API_URL"] = Ctx.ProxyUrl,
        };
        var client = Ctx.CreateClient(
            options: new CopilotClientOptions { SessionFs = CreateSessionFsConfig() },
            environment: env,
            autoInjectGitHubToken: false);
        var seen = new List<CopilotSession>();

        async Task CreateFailingAsync()
        {
            var ex = await Assert.ThrowsAnyAsync<Exception>(() => Ctx.CreateSessionAsync(client, new SessionConfig
            {
                GitHubToken = "invalid-token",
                OnPermissionRequest = PermissionHandler.ApproveAll,
                CreateSessionFsProvider = session =>
                {
                    seen.Add(session);
                    return new MissingSessionFsProvider();
                },
            }));
            Assert.Contains("401", ex.ToString(), StringComparison.OrdinalIgnoreCase);
        }

        // Warm up: the first call establishes the CLI connection.
        await CreateFailingAsync();
        seen.Clear();

        var sessions = GetSessionsMap(client);

        // The public provider factory exposes each wrapper before its failing RPC,
        // without depending on a polling thread observing a transient registration.
        for (var i = 0; i < 20; i++)
        {
            await CreateFailingAsync();
        }

        Assert.Equal(20, seen.Count);
        Assert.Empty(sessions);

        foreach (var s in seen)
        {
            Assert.True(IsEventChannelClosed(s), "A failed CreateSessionAsync's session had its event channel left open, leaking its background event-processing task.");
        }
    }

    [Fact]
    public async Task ResumeSessionAsync_Failure_Does_Not_Leak_The_Session_Or_Its_Event_Loop()
    {
        // Use our own dedicated client rather than the shared fixture's ResumeSessionAsync
        // helper: that helper spins up a brand-new CopilotClient for every call it makes
        // (to support the multi-client resume scenarios it's designed for), so each call's
        // pre-registered session would land in a different, short-lived client's dictionary
        // that we'd never get to observe. A single, reused client lets us watch one
        // dictionary across all 20 failed calls.
        var client = Ctx.CreateClient(options: new CopilotClientOptions { SessionFs = CreateSessionFsConfig() });
        var sessions = GetSessionsMap(client);
        var seen = new List<CopilotSession>();

        async Task ResumeNonExistentAsync()
        {
            var exception = await Assert.ThrowsAnyAsync<Exception>(() =>
                Ctx.ResumeSessionAsync(client, "non-existent-leak-check-session", new ResumeSessionConfig
                {
                    OnPermissionRequest = PermissionHandler.ApproveAll,
                    CreateSessionFsProvider = session =>
                    {
                        seen.Add(session);
                        return new MissingSessionFsProvider();
                    },
                }));
            Assert.Contains("not found", exception.ToString(), StringComparison.OrdinalIgnoreCase);
        }

        // Warm up: the first call establishes the CLI connection.
        await ResumeNonExistentAsync();
        seen.Clear();

        var baseline = sessions.Count;
        for (var i = 0; i < 20; i++)
        {
            await ResumeNonExistentAsync();
        }

        Assert.Equal(20, seen.Count);

        Assert.True(
            sessions.Count == baseline,
            $"Expected no sessions left registered after 20 failed ResumeSessionAsync calls (baseline={baseline}), " +
            $"but found {sessions.Count}. Failed ResumeSessionAsync calls must not leak the local session registration.");

        foreach (var s in seen)
        {
            Assert.True(IsEventChannelClosed(s), "A failed ResumeSessionAsync's session had its event channel left open, leaking its background event-processing task.");
        }
    }

    private sealed class MissingSessionFsProvider : SessionFsProvider
    {
        protected override Task<string> ReadFileAsync(string path, CancellationToken cancellationToken) =>
            throw new FileNotFoundException("Session file does not exist.", path);

        protected override Task<bool> ExistsAsync(string path, CancellationToken cancellationToken) => Task.FromResult(false);

        protected override Task<SessionFsStatResult> StatAsync(string path, CancellationToken cancellationToken) =>
            throw new FileNotFoundException("Session file does not exist.", path);

        protected override Task<IList<string>> ReadDirectoryAsync(string path, CancellationToken cancellationToken) =>
            Task.FromResult<IList<string>>([]);

        protected override Task<IList<SessionFsReaddirWithTypesEntry>> ReadDirectoryWithTypesAsync(string path, CancellationToken cancellationToken) =>
            Task.FromResult<IList<SessionFsReaddirWithTypesEntry>>([]);

        protected override Task WriteFileAsync(string path, string content, int? mode, CancellationToken cancellationToken) =>
            throw new NotSupportedException();

        protected override Task AppendFileAsync(string path, string content, int? mode, CancellationToken cancellationToken) =>
            throw new NotSupportedException();

        protected override Task MakeDirectoryAsync(string path, bool recursive, int? mode, CancellationToken cancellationToken) =>
            throw new NotSupportedException();

        protected override Task RemoveAsync(string path, bool recursive, bool force, CancellationToken cancellationToken) =>
            throw new NotSupportedException();

        protected override Task RenameAsync(string src, string dest, CancellationToken cancellationToken) =>
            throw new NotSupportedException();
    }
}
