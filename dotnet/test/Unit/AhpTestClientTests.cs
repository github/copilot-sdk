/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System.Diagnostics;
using GitHub.Copilot.Test.Harness;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed class AhpTestClientTests
{
    private sealed class OwnedClock : TimeProvider
    {
        private sealed class Timer(OwnedClock clock, TimerCallback callback, object? state) : ITimer
        {
            private TimeSpan _due;
            private bool _disposed;
            public bool Change(TimeSpan dueTime, TimeSpan period)
            {
                Assert.Equal(Timeout.InfiniteTimeSpan, period);
                clock.Schedules++;
                _due = clock.Elapsed + dueTime;
                return !_disposed;
            }
            public void Advance()
            {
                if (!_disposed && clock.Elapsed >= _due)
                {
                    _disposed = true;
                    callback(state);
                }
            }
            public void Dispose() => _disposed = true;
            public ValueTask DisposeAsync() { Dispose(); return ValueTask.CompletedTask; }
        }

        private readonly List<Timer> _timers = [];
        public TimeSpan Elapsed { get; private set; }
        public int Schedules { get; private set; }
        public override ITimer CreateTimer(TimerCallback callback, object? state, TimeSpan dueTime, TimeSpan period)
        {
            Assert.Equal(TimeSpan.FromSeconds(60), dueTime);
            var timer = new Timer(this, callback, state);
            timer.Change(dueTime, period);
            _timers.Add(timer);
            return timer;
        }
        public void AdvanceTo(TimeSpan elapsed)
        {
            Elapsed = elapsed;
            foreach (var timer in _timers) timer.Advance();
        }
    }

    private static Process StartPeer(string script)
    {
        var info = new ProcessStartInfo("node")
        {
            UseShellExecute = false,
            RedirectStandardInput = true,
            RedirectStandardOutput = true,
            RedirectStandardError = true,
        };
        info.ArgumentList.Add("-e");
        info.ArgumentList.Add(script);
        return Process.Start(info) ?? throw new InvalidOperationException("Could not start the owned AHP peer");
    }

    private static async Task CancelAndJoinRequestAsync(Task request, CancellationTokenSource owner)
    {
        owner.Cancel();
        if (!request.IsCompleted)
        {
            try { await request; }
            catch (OperationCanceledException) when (owner.IsCancellationRequested) { }
        }
        _ = request.Exception;
    }

    private static async Task AssertAndJoinRequestAsync(
        Task request, CancellationTokenSource owner, Func<Task> assertions)
    {
        Exception? bodyFailure = null;
        try { await assertions(); }
        catch (Exception failure)
        {
            bodyFailure = failure;
        }
        try { await CancelAndJoinRequestAsync(request, owner); }
        catch (Exception cleanup)
        {
            if (bodyFailure is not null)
                throw new AggregateException("AHP fixture assertions and request cleanup failed", bodyFailure, cleanup);
            throw;
        }
        if (bodyFailure is not null)
            System.Runtime.ExceptionServices.ExceptionDispatchInfo.Throw(bodyFailure);
    }

    [Fact]
    public async Task EligibleRepliesRepeatOnlyTheSameRequest()
    {
        var process = StartPeer("""
            let attempts = 0;
            require('node:readline').createInterface({input: process.stdin}).on('line', line => {
                const command = JSON.parse(line);
                if (command.op !== 'authenticate' || command.token !== 'original')
                    throw new Error(`unexpected command: ${line}`);
                console.log(JSON.stringify(++attempts < 3
                    ? {error: 'expired fixture'}
                    : {result: {attempts}}));
            });
            """);
        await using var client = new AhpTestClient(process);
        var observed = new List<string>();
        var result = await client.RequestAsync(new() { ["op"] = "authenticate", ["token"] = "original" }, error =>
        {
            observed.Add(error);
            return error == "expired fixture";
        });
        Assert.Equal(3, result.GetProperty("attempts").GetInt32());
        Assert.Collection(observed,
            error => Assert.Equal("expired fixture", error),
            error => Assert.Equal("expired fixture", error));
    }

    [Fact]
    public async Task UnmatchedFailureIsNotRetried()
    {
        var process = StartPeer("""
            let requests = 0;
            require('node:readline').createInterface({input: process.stdin}).on('line', () => {
                if (++requests !== 1) throw new Error('unexpected retry');
                console.log(JSON.stringify({error: 'transport failure'}));
            });
            """);
        await using var client = new AhpTestClient(process);
        var calls = 0;
        var error = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            client.RequestAsync(new() { ["op"] = "authenticate" }, error =>
            {
                calls++;
                return error == "expired fixture";
            }));
        Assert.Equal("transport failure", error.Message);
        Assert.Equal(1, calls);
    }

    [Theory]
    [InlineData(false, false)]
    [InlineData(false, true)]
    [InlineData(true, true)]
    public async Task CallerCancellationJoinsTheOriginalPendingRead(bool afterResubmission, bool recovery)
    {
        var process = StartPeer($$"""
            let requests = 0;
            require('node:readline').createInterface({input: process.stdin}).on('line', () => {
                ++requests;
                if ({{(afterResubmission ? "true" : "false")}} && requests === 1)
                    console.log(JSON.stringify({error: 'expired fixture'}));
                else
                    console.error(`held ${requests}`);
            });
            """);
        await using var client = new AhpTestClient(process);
        using var cancellation = new CancellationTokenSource();
        var request = client.RequestAsync(
            new() { ["op"] = "authenticate" },
            recovery ? error => error == "expired fixture" : null,
            cancellation.Token);
        var admission = process.StandardError.ReadLineAsync();
        await Task.WhenAny(admission, request);
        if (!admission.IsCompleted)
        {
            await request;
            Assert.Fail("The request completed without reaching the owned peer checkpoint");
        }
        Assert.Equal(afterResubmission ? "held 2" : "held 1", await admission);
        cancellation.Cancel();
        var original = await Assert.ThrowsAsync<OperationCanceledException>(() => request);
        Assert.True(process.HasExited);
        var reuse = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            client.RequestAsync(new() { ["op"] = "authenticate" }));
        Assert.Same(original, reuse.InnerException);
        await client.DisposeAsync();
    }

    [Fact]
    public async Task InitialAuthenticationKeepsOneOriginalSixtySecondOwner()
    {
        var process = StartPeer("""
            let requests = 0;
            require('node:readline').createInterface({input: process.stdin}).on('line', () => {
                if (++requests === 1)
                    console.log(JSON.stringify({error: 'expired fixture'}));
                else
                    console.error(`held ${requests}`);
            });
            """);
        var clock = new OwnedClock();
        using var realOwner = new CancellationTokenSource(TimeSpan.FromSeconds(60));
        await using var client = new AhpTestClient(process, clock);
        var request = client.RequestAsync(new() { ["op"] = "authenticate" }, error =>
        {
            Assert.Equal("expired fixture", error);
            clock.AdvanceTo(TimeSpan.FromSeconds(59));
            return true;
        }, realOwner.Token);
        await AssertAndJoinRequestAsync(request, realOwner, async () =>
        {
            var admission = process.StandardError.ReadLineAsync();
            await Task.WhenAny(admission, request);
            if (!admission.IsCompleted)
            {
                await request;
                Assert.Fail("The request completed without reaching the owned peer checkpoint");
            }
            Assert.Equal("held 2", await admission);
            Assert.Equal(1, clock.Schedules);
            clock.AdvanceTo(TimeSpan.FromSeconds(61));
            var failure = await Assert.ThrowsAsync<TimeoutException>(() => request);
            Assert.Contains("original 60-second read budget", failure.Message);
            Assert.True(process.HasExited);
            Assert.Equal(1, clock.Schedules);
        });
    }

    [Fact]
    public async Task OwnerCancellationStopsAndJoinsAPartialResubmission()
    {
        var process = StartPeer("""
            let first = '';
            let resubmission = false;
            process.stdin.on('data', chunk => {
                if (resubmission) {
                    process.stdin.pause();
                    // Paused stdin does not keep Node alive; the owner must terminate this peer.
                    setInterval(() => {}, 1000);
                    console.error('partial resubmission');
                    return;
                }
                first += chunk;
                if (first.endsWith('\n')) {
                    if (JSON.parse(first).op !== 'authenticate') throw new Error('wrong operation');
                    first = '';
                    resubmission = true;
                    console.log(JSON.stringify({error: 'expired fixture'}));
                }
            });
            """);
        var clock = new OwnedClock();
        using var realOwner = new CancellationTokenSource(TimeSpan.FromSeconds(60));
        await using var client = new AhpTestClient(process, clock);
        var request = client.RequestAsync(
            new() { ["op"] = "authenticate", ["payload"] = new string('x', 4 * 1024 * 1024) },
            error =>
            {
                Assert.Equal("expired fixture", error);
                clock.AdvanceTo(TimeSpan.FromSeconds(59));
                return true;
            }, realOwner.Token);
        await AssertAndJoinRequestAsync(request, realOwner, async () =>
        {
            var admission = process.StandardError.ReadLineAsync();
            await Task.WhenAny(admission, request);
            if (!admission.IsCompleted)
            {
                await request;
                Assert.Fail("The request completed without reaching the owned peer checkpoint");
            }
            Assert.Equal("partial resubmission", await admission);
            Assert.Equal(1, clock.Schedules);
            clock.AdvanceTo(TimeSpan.FromSeconds(61));
            await Assert.ThrowsAsync<TimeoutException>(() => request);
            Assert.True(process.HasExited);
            Assert.Equal(1, clock.Schedules);
            await client.DisposeAsync();
        });
    }
}
#endif
