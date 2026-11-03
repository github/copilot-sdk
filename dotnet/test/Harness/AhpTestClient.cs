/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System.Diagnostics;
using System.Text.Json;
using System.Text.Json.Nodes;

namespace GitHub.Copilot.Test.Harness;

public sealed class AhpTestClient : IAsyncDisposable
{
    private readonly Process _process;
    private readonly TimeProvider _timeProvider;
    private readonly SemaphoreSlim _gate = new(1, 1);
    private Task? _pendingIo;
    private Exception? _requestFailure;
    private bool _forcedTermination;
    private bool _disposed;

    public AhpTestClient(Process process, TimeProvider? timeProvider = null)
    {
        _process = process;
        _timeProvider = timeProvider ?? TimeProvider.System;
    }

    public static async Task<AhpTestClient> StartAsync(bool authentication = false)
    {
        var directory = new DirectoryInfo(AppContext.BaseDirectory);
        while (directory is not null && !File.Exists(Path.Combine(directory.FullName, "nodejs", "test", "e2e", "harness", "ahpTestDriver.ts")))
            directory = directory.Parent;
        if (directory is null) throw new InvalidOperationException("Cannot find the standard AHP test client");
        var info = new ProcessStartInfo("node")
        {
            WorkingDirectory = Path.Combine(directory.FullName, "nodejs"),
            UseShellExecute = false,
            RedirectStandardInput = true,
            RedirectStandardOutput = true
        };
        info.ArgumentList.Add("--import");
        info.ArgumentList.Add("tsx");
        info.ArgumentList.Add(authentication
            ? Path.Combine(directory.FullName, "test", "harness", "hostAuthenticationDriver.mjs")
            : "test/e2e/harness/ahpTestDriver.ts");
        var process = Process.Start(info) ?? throw new InvalidOperationException("Could not start the AHP test client");
        var client = new AhpTestClient(process);
        try
        {
            var ready = await client.ReadAsync();
            if (!ready.GetProperty("ready").GetBoolean()) throw new InvalidOperationException("AHP client is not ready");
            return client;
        }
        catch
        {
            await client.DisposeAsync();
            throw;
        }
    }

    public async Task<JsonElement> RequestAsync(
        JsonObject command,
        Func<string, bool>? retryExpiredError = null,
        CancellationToken cancellationToken = default)
    {
        await _gate.WaitAsync(cancellationToken);
        try
        {
            if (_requestFailure is not null)
                throw new InvalidOperationException("AHP client cannot reuse a failed owned request", _requestFailure);
            var writing = _process.StandardInput.WriteLineAsync(command.ToJsonString());
            if (cancellationToken.CanBeCanceled)
                await AwaitRequestIoAsync(writing, cancellationToken, cancellationToken);
            else
                await writing;
            var flushing = _process.StandardInput.FlushAsync(CancellationToken.None);
            if (cancellationToken.CanBeCanceled)
                await AwaitRequestIoAsync(flushing, cancellationToken, cancellationToken);
            else
                await flushing;
            if (retryExpiredError is null && !cancellationToken.CanBeCanceled)
            {
                var response = await ReadAsync();
                if (response.TryGetProperty("error", out var error)) throw new InvalidOperationException(error.GetString());
                return response.GetProperty("result").Clone();
            }

            using var timeout = new CancellationTokenSource(TimeSpan.FromSeconds(60), _timeProvider);
            using var owner = CancellationTokenSource.CreateLinkedTokenSource(timeout.Token, cancellationToken);
            while (true)
            {
                var reading = _process.StandardOutput.ReadLineAsync(CancellationToken.None).AsTask();
                await AwaitRequestIoAsync(reading, owner.Token, cancellationToken);
                var response = ParseLine(await reading);
                if (!response.TryGetProperty("error", out var error))
                    return response.GetProperty("result").Clone();
                var message = error.GetString() ?? throw new JsonException("AHP error must be a string");
                if (retryExpiredError is null || !retryExpiredError(message))
                    throw new InvalidOperationException(message);
                if (owner.IsCancellationRequested)
                {
                    _requestFailure = RequestCancellation(cancellationToken);
                    throw _requestFailure;
                }
                await AwaitRequestIoAsync(_process.StandardInput.WriteLineAsync(command.ToJsonString()), owner.Token, cancellationToken);
                await AwaitRequestIoAsync(_process.StandardInput.FlushAsync(CancellationToken.None), owner.Token, cancellationToken);
            }
        }
        finally { _gate.Release(); }
    }

    private static Exception RequestCancellation(CancellationToken cancellationToken) =>
        cancellationToken.IsCancellationRequested
            ? new OperationCanceledException(cancellationToken)
            : new TimeoutException("Initial authentication exhausted its original 60-second read budget");

    private async Task AwaitRequestIoAsync(Task pending, CancellationToken owner, CancellationToken cancellationToken)
    {
        _pendingIo = pending;
        try { await pending.WaitAsync(owner); }
        catch (OperationCanceledException expired) when (owner.IsCancellationRequested)
        {
            var failure = RequestCancellation(cancellationToken);
            _requestFailure = failure;
            try
            {
                if (!_process.HasExited)
                {
                    _process.Kill();
                    _forcedTermination = true;
                }
                await _process.WaitForExitAsync();
                try { await pending; }
                catch (IOException closed) when (_forcedTermination)
                {
                    Console.Error.WriteLine($"AHP pending I/O ended after owned termination: {closed}");
                }
            }
            catch (Exception cleanup)
            {
                throw new AggregateException("AHP request and owned I/O cleanup failed", failure, expired, cleanup);
            }
            throw failure;
        }
        finally
        {
            if (pending.IsCompleted) _pendingIo = null;
        }
    }

    private async Task<JsonElement> ReadAsync()
    {
        var line = await _process.StandardOutput.ReadLineAsync().WaitAsync(TimeSpan.FromSeconds(60));
        return ParseLine(line);
    }

    private static JsonElement ParseLine(string? line)
    {
        if (line is null) throw new IOException("AHP test client closed its output unexpectedly");
        using var document = JsonDocument.Parse(line);
        return document.RootElement.Clone();
    }

    public async ValueTask DisposeAsync()
    {
        if (_disposed) return;
        var failures = new List<Exception>();
        try
        {
            try { _process.StandardInput.Close(); }
            catch (IOException closed) when (_forcedTermination)
            {
                Console.Error.WriteLine($"AHP stdin closed after owned termination: {closed}");
            }
            catch (Exception failure) { failures.Add(failure); }
            try { await _process.WaitForExitAsync().WaitAsync(TimeSpan.FromSeconds(10)); }
            catch (TimeoutException failure)
            {
                failures.Add(failure);
                try
                {
                    if (!_process.HasExited)
                    {
                        _process.Kill();
                        _forcedTermination = true;
                    }
                    await _process.WaitForExitAsync();
                }
                catch (Exception cleanup) { failures.Add(cleanup); }
            }
            catch (Exception failure) { failures.Add(failure); }
            if (_process.HasExited)
            {
                if (_process.ExitCode != 0 && !_forcedTermination)
                    failures.Add(new IOException($"AHP test client exited with status {_process.ExitCode}"));
                if (_pendingIo is { } pending)
                {
                    try { await pending; }
                    catch (IOException closed) when (_forcedTermination)
                    {
                        Console.Error.WriteLine($"AHP pending I/O joined after owned termination: {closed}");
                    }
                    catch (Exception failure) { failures.Add(failure); }
                }
            }
        }
        finally
        {
            _disposed = true;
            _process.Dispose();
            _gate.Dispose();
        }
        if (failures.Count != 0)
        {
            if (_requestFailure is not null) failures.Insert(0, _requestFailure);
            if (failures.Count == 1) throw failures[0];
            throw new AggregateException("AHP client cleanup failed", failures);
        }
    }
}
#endif
