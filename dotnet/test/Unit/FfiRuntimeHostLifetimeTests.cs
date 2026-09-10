/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Microsoft.Extensions.Logging;
using System.Reflection;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed class FfiRuntimeHostLifetimeTests
{
    [Fact]
    public void Dispose_Retains_Callback_Until_Connection_Close_Succeeds()
    {
        var allowClose = false;
        var closeCalls = 0;
        var shutdownCalls = 0;
        var callbackReleaseCalls = 0;
        Func<uint, bool> connectionClose = _ =>
        {
            Interlocked.Increment(ref closeCalls);
            return Volatile.Read(ref allowClose);
        };
        Func<uint, bool> hostShutdown = _ =>
        {
            Interlocked.Increment(ref shutdownCalls);
            return true;
        };
        Action releaseCallback = () => Interlocked.Increment(ref callbackReleaseCalls);

        using var loggerFactory = LoggerFactory.Create(_ => { });
        var hostType = typeof(CopilotClient).Assembly.GetType("GitHub.Copilot.FfiRuntimeHost", throwOnError: true)!;
        var constructor = hostType.GetConstructors(BindingFlags.Instance | BindingFlags.NonPublic)
            .Single(candidate => candidate.GetParameters().Length == 8);
        var host = (IDisposable)constructor.Invoke(
            [
                "test-runtime",
                null,
                null,
                Array.Empty<string>(),
                loggerFactory.CreateLogger("FfiRuntimeHostLifetimeTests"),
                connectionClose,
                hostShutdown,
                releaseCallback,
            ]);

        hostType.GetField("_connectionId", BindingFlags.Instance | BindingFlags.NonPublic)!
            .SetValue(host, (uint)21);
        hostType.GetField("_serverId", BindingFlags.Instance | BindingFlags.NonPublic)!
            .SetValue(host, (uint)11);

        host.Dispose();

        Assert.Equal(0, Volatile.Read(ref callbackReleaseCalls));
        Assert.Equal(0, Volatile.Read(ref shutdownCalls));

        Volatile.Write(ref allowClose, true);
        Assert.True(
            SpinWait.SpinUntil(
                () => Volatile.Read(ref callbackReleaseCalls) == 1
                    && Volatile.Read(ref shutdownCalls) == 1,
                TimeSpan.FromSeconds(5)),
            "Deferred native cleanup did not complete.");

        var closeCallsAfterCleanup = Volatile.Read(ref closeCalls);
        host.Dispose();
        Thread.Sleep(50);

        Assert.Equal(closeCallsAfterCleanup, Volatile.Read(ref closeCalls));
        Assert.Equal(1, Volatile.Read(ref callbackReleaseCalls));
        Assert.Equal(1, Volatile.Read(ref shutdownCalls));
    }
}
