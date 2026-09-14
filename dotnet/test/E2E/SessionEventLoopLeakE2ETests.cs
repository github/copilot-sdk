/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Test.Harness;
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

    /// <summary>
    /// Continuously scans <see cref="CopilotClient"/>'s session dictionary on a dedicated,
    /// tightly-spinning thread (not the thread pool, and no <c>await</c>-based yielding) so
    /// that even a sub-millisecond in-flight registration window — as seen with a fast local
    /// RPC failure like a nonexistent-session resume — is reliably observed.
    /// </summary>
    private sealed class SessionSniffer : IDisposable
    {
        private readonly List<CopilotSession> _seen = [];
        private readonly Thread _thread;
        private volatile bool _stop;

        public SessionSniffer(IDictionary sessions)
        {
            _thread = new Thread(() =>
            {
                var iterations = 0L;
                while (!_stop)
                {
                    iterations++;
                    foreach (CopilotSession s in sessions.Values)
                    {
                        lock (_seen)
                        {
                            if (!_seen.Contains(s)) _seen.Add(s);
                        }
                    }
                }
                Iterations = iterations;
            })
            { IsBackground = true };
            _thread.Start();
        }

        public long Iterations { get; private set; }

        public IReadOnlyList<CopilotSession> Stop()
        {
            _stop = true;
            _thread.Join();
            lock (_seen) return [.. _seen];
        }

        public void Dispose() => _stop = true;
    }

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
        var client = Ctx.CreateClient(environment: env, autoInjectGitHubToken: false);

        async Task CreateFailingAsync()
        {
            var ex = await Assert.ThrowsAnyAsync<Exception>(() => Ctx.CreateSessionAsync(client, new SessionConfig
            {
                GitHubToken = "invalid-token",
                OnPermissionRequest = PermissionHandler.ApproveAll,
            }));
            Assert.Contains("401", ex.ToString(), StringComparison.OrdinalIgnoreCase);
        }

        // Warm up: the first call establishes the CLI connection.
        await CreateFailingAsync();

        var sessions = GetSessionsMap(client);

        // The session is registered (and its event-loop consumer started) before the RPC
        // completes, and is only ever removed inside CreateSessionAsync's own catch block —
        // by the time a failed call *returns* to us, RemoveFromClient() has already run, so
        // polling the dictionary after each await observes nothing. We must instead observe
        // it concurrently, while each call is still in flight, to capture the real session
        // object and verify its event channel actually got closed.
        using var sniffer = new SessionSniffer(sessions);

        for (var i = 0; i < 20; i++)
        {
            await CreateFailingAsync();
        }

        var seen = sniffer.Stop();

        Assert.True(seen.Count > 0, "Test did not observe any in-flight session registrations; cannot validate the fix.");
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
        var client = Ctx.CreateClient();
        var sessions = GetSessionsMap(client);

        async Task ResumeNonExistentAsync()
        {
            await Assert.ThrowsAnyAsync<Exception>(() =>
                Ctx.ResumeSessionAsync(client, "non-existent-leak-check-session", new ResumeSessionConfig
                {
                    OnPermissionRequest = PermissionHandler.ApproveAll,
                }));
        }

        // Warm up: the first call establishes the CLI connection.
        await ResumeNonExistentAsync();

        // Same rationale as the CreateSessionAsync test above: the pre-registered session is
        // already removed from the dictionary by the time a failed call returns, so we must
        // observe it concurrently, while the call is still in flight, to actually validate
        // that its event channel got closed rather than just that it got unregistered.
        using var sniffer = new SessionSniffer(sessions);

        var baseline = sessions.Count;
        for (var i = 0; i < 20; i++)
        {
            await ResumeNonExistentAsync();
        }

        var seen = sniffer.Stop();

        Assert.True(seen.Count > 0, $"Test did not observe any in-flight session registrations (sniffer ran {sniffer.Iterations} iterations); cannot validate the fix.");

        Assert.True(
            sessions.Count == baseline,
            $"Expected no sessions left registered after 20 failed ResumeSessionAsync calls (baseline={baseline}), " +
            $"but found {sessions.Count}. Failed ResumeSessionAsync calls must not leak the local session registration.");

        foreach (var s in seen)
        {
            Assert.True(IsEventChannelClosed(s), "A failed ResumeSessionAsync's session had its event channel left open, leaking its background event-processing task.");
        }
    }
}
