/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Microsoft.Extensions.Logging.Abstractions;
using System.Reflection;
using System.Runtime.CompilerServices;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed class FfiRuntimeHostLifetimeTests
{
    private static readonly string[] NoAutoLoginArguments = ["--no-auto-login"];

    [Fact]
    public void Open_Connection_Keeps_Unreferenced_Host_Reachable_Until_Disposed()
    {
        var homeDirectory = Path.Combine(Path.GetTempPath(), $"copilot-ffi-lifetime-{Guid.NewGuid():N}");
        Directory.CreateDirectory(homeDirectory);
        try
        {
            var host = StartUnreferencedHost(homeDirectory);
            try
            {
                CollectGarbage();
                // Native code is not a GC root. A host collected while its connection is open
                // turns the next native callback into a call on a collected delegate.
                Assert.True(host.IsAlive, "An open native connection must keep its callback target reachable.");
            }
            finally
            {
                DisposeHost(host);
            }

            CollectGarbage();
            Assert.False(host.IsAlive, "Closing the native connection must release its callback target.");
        }
        finally
        {
            try
            {
                Directory.Delete(homeDirectory, recursive: true);
            }
            catch (Exception ex) when (ex is IOException or UnauthorizedAccessException)
            {
                // Best effort: a failed assertion leaves the native host running.
            }
        }
    }

    [Fact]
    public async Task Dispose_Retains_Callback_Until_Connection_Close_Succeeds()
    {
        var allowClose = false;
        var closeCalls = 0;
        var shutdownCalls = 0;
        var callbackReleaseCalls = 0;
        var cleanupCompleted = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        Func<uint, bool> connectionClose = _ =>
        {
            Interlocked.Increment(ref closeCalls);
            return Volatile.Read(ref allowClose);
        };
        Func<uint, bool> hostShutdown = _ =>
        {
            Interlocked.Increment(ref shutdownCalls);
            cleanupCompleted.TrySetResult();
            return true;
        };
        Action releaseCallback = () => Interlocked.Increment(ref callbackReleaseCalls);

        var hostType = typeof(CopilotClient).Assembly.GetType("GitHub.Copilot.FfiRuntimeHost", throwOnError: true)!;
        var constructor = hostType.GetConstructors(BindingFlags.Instance | BindingFlags.NonPublic)
            .Single(candidate => candidate.GetParameters().Length == 8);
        using var host = (IDisposable)constructor.Invoke(
            [
                "test-runtime",
                null,
                null,
                Array.Empty<string>(),
                NullLogger.Instance,
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
        await cleanupCompleted.Task.WaitAsync(TimeSpan.FromSeconds(5));
        Assert.Equal(1, Volatile.Read(ref callbackReleaseCalls));
        Assert.Equal(1, Volatile.Read(ref shutdownCalls));

        var closeCallsAfterCleanup = Volatile.Read(ref closeCalls);
        host.Dispose();
        await Task.Delay(50);

        Assert.Equal(closeCallsAfterCleanup, Volatile.Read(ref closeCalls));
        Assert.Equal(1, Volatile.Read(ref callbackReleaseCalls));
        Assert.Equal(1, Volatile.Read(ref shutdownCalls));
    }

    [Fact]
    public void Dispose_Does_Not_Retry_Terminal_Host_Shutdown_Failure()
    {
        var closeCalls = 0;
        var shutdownCalls = 0;
        var callbackReleaseCalls = 0;

        var hostType = typeof(CopilotClient).Assembly.GetType("GitHub.Copilot.FfiRuntimeHost", throwOnError: true)!;
        var constructor = hostType.GetConstructors(BindingFlags.Instance | BindingFlags.NonPublic)
            .Single(candidate => candidate.GetParameters().Length == 8);
        using var host = (IDisposable)constructor.Invoke(
            [
                "test-runtime",
                null,
                null,
                Array.Empty<string>(),
                NullLogger.Instance,
                new Func<uint, bool>(_ =>
                {
                    Interlocked.Increment(ref closeCalls);
                    return true;
                }),
                new Func<uint, bool>(_ =>
                {
                    Interlocked.Increment(ref shutdownCalls);
                    return false;
                }),
                new Action(() => Interlocked.Increment(ref callbackReleaseCalls)),
            ]);

        hostType.GetField("_connectionId", BindingFlags.Instance | BindingFlags.NonPublic)!
            .SetValue(host, (uint)21);
        hostType.GetField("_serverId", BindingFlags.Instance | BindingFlags.NonPublic)!
            .SetValue(host, (uint)11);

        host.Dispose();
        Thread.Sleep(250);

        Assert.Equal(1, Volatile.Read(ref closeCalls));
        Assert.Equal(1, Volatile.Read(ref callbackReleaseCalls));
        Assert.Equal(1, Volatile.Read(ref shutdownCalls));
    }

    // Starts a real native host and returns only a weak reference, so nothing in managed
    // code roots it. Resolves the runtime library the way CopilotClient does, so it loads
    // the same library as the in-process E2E tests in this process.
    [MethodImpl(MethodImplOptions.NoInlining)]
    private static WeakReference StartUnreferencedHost(string homeDirectory)
    {
        var hostType = typeof(CopilotClient).Assembly.GetType("GitHub.Copilot.FfiRuntimeHost", throwOnError: true)!;
        var explicitCliPath = Environment.GetEnvironmentVariable("COPILOT_CLI_PATH");
        if (string.IsNullOrEmpty(explicitCliPath))
        {
            explicitCliPath = null;
        }

        string libraryPath;
        if (explicitCliPath is null)
        {
            object?[] arguments =
            [
                hostType.GetMethod("GetRuntimeLibraryFileName", BindingFlags.Static | BindingFlags.NonPublic)!
                    .Invoke(null, null),
                null,
            ];
            libraryPath = (string?)typeof(CopilotClient)
                .GetMethod("GetBundledNativePath", BindingFlags.Static | BindingFlags.NonPublic)!
                .Invoke(null, arguments)
                ?? throw new InvalidOperationException($"In-process FFI runtime library not found at '{arguments[1]}'.");
        }
        else
        {
            libraryPath = (string)typeof(CopilotClient)
                .GetMethod("ResolveRuntimePathForExplicitCli", BindingFlags.Static | BindingFlags.NonPublic)!
                .Invoke(null, [explicitCliPath])!;
        }

        var host = hostType.GetMethod("Create", BindingFlags.Static | BindingFlags.Public)!.Invoke(
            null,
            [
                libraryPath,
                explicitCliPath,
                new Dictionary<string, string>
                {
                    ["COPILOT_HOME"] = homeDirectory,
                    ["COPILOT_DISABLE_KEYTAR"] = "1",
                },
                NoAutoLoginArguments,
                NullLogger.Instance,
            ])!;
        ((Task)hostType.GetMethod("StartAsync")!.Invoke(host, [CancellationToken.None])!).GetAwaiter().GetResult();
        return new WeakReference(host);
    }

    [MethodImpl(MethodImplOptions.NoInlining)]
    private static void DisposeHost(WeakReference host) => (host.Target as IDisposable)?.Dispose();

    private static void CollectGarbage()
    {
        GC.Collect();
        GC.WaitForPendingFinalizers();
        GC.Collect();
    }
}
