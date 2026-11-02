/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Diagnostics;
using System.Globalization;
using Microsoft.Extensions.Logging;

namespace GitHub.Copilot.Test.Harness;

internal sealed class RuntimeStartupProcessObserver(string pidPath, bool useStdio, ILogger? logger = null)
    : ILogger, IDisposable
{
    private Process? _process;

    public Process Process => _process
        ?? throw new InvalidOperationException("The runtime process was not observed before startup failed.");

    public IDisposable? BeginScope<TState>(TState state) where TState : notnull => logger?.BeginScope(state);

    public bool IsEnabled(LogLevel logLevel) => true;

    public void Log<TState>(LogLevel logLevel, EventId eventId, TState state, Exception? exception,
        Func<TState, Exception?, string> formatter)
    {
        if (_process is null && state is IReadOnlyList<KeyValuePair<string, object?>> properties &&
            (useStdio
                ? properties.Any(entry => entry is { Key: "Method", Value: "connect" }) &&
                  properties.Any(entry => entry is { Key: "Status", Value: "Succeeded" })
                : properties.Any(entry => entry is { Key: "Line", Value: string line } &&
                    line.StartsWith("listening on port ", StringComparison.Ordinal))))
        {
            // These callbacks run before protocol validation or the TCP connect attempt.
            // Open the OS handle now: acquiring it after failure can adopt a reused PID.
            var process = System.Diagnostics.Process.GetProcessById(
                int.Parse(File.ReadAllText(pidPath), CultureInfo.InvariantCulture));
            try
            {
                _ = process.Handle;
                if (process.HasExited)
                {
                    throw new InvalidOperationException("The runtime exited before its startup process was observed.");
                }
                _process = process;
            }
            catch
            {
                process.Dispose();
                throw;
            }
        }

        if (logger?.IsEnabled(logLevel) == true)
        {
            logger.Log(logLevel, eventId, state, exception, formatter);
        }
    }

    public void Dispose() => _process?.Dispose();
}
