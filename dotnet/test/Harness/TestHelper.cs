/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Text.RegularExpressions;

namespace GitHub.Copilot.Test.Harness;

public static class TestHelper
{
    // Default tolerates CLI / replay-proxy cold start on Windows GitHub Actions
    // runners, where the first test in a fixture can take ~60s before the first
    // assistant message arrives. Subsequent tests in the same fixture typically
    // complete in well under a second.
    private static readonly TimeSpan DefaultEventTimeout = TimeSpan.FromSeconds(120);
    private static readonly TimeSpan DefaultPollInterval = TimeSpan.FromMilliseconds(100);

    public static async Task<AssistantMessageEvent> SendAndGetFinalAssistantMessageAsync(
        CopilotSession session,
        MessageOptions options,
        TimeSpan? timeout = null)
    {
        // Subscribe before sending: session.idle is ephemeral and cannot be backfilled.
        return await session.SendAndWaitAsync(options, timeout ?? DefaultEventTimeout)
            ?? throw new InvalidOperationException("Session became idle without an assistant message.");
    }

    public static async Task<T> GetNextEventOfTypeAsync<T>(
        CopilotSession session,
        TimeSpan? timeout = null) where T : SessionEvent
        => await GetNextEventOfTypeAsync<T>(session, static _ => true, timeout);

    public static async Task<T> GetNextEventOfTypeAsync<T>(
        CopilotSession session,
        Func<T, bool> predicate,
        TimeSpan? timeout = null,
        string? timeoutDescription = null) where T : SessionEvent
    {
        var tcs = new TaskCompletionSource<T>(TaskCreationOptions.RunContinuationsAsynchronously);
        using var cts = new CancellationTokenSource(timeout ?? DefaultEventTimeout);

        using var subscription = session.On<SessionEvent>(evt =>
        {
            if (evt is T matched && predicate(matched))
            {
                tcs.TrySetResult(matched);
            }
            else if (evt is SessionErrorEvent error)
            {
                tcs.TrySetException(new Exception(error.Data.Message ?? "session error"));
            }
        });

        cts.Token.Register(() => tcs.TrySetException(
            new TimeoutException($"Timeout waiting for {timeoutDescription ?? $"event of type '{typeof(T).Name}'"}")));

        return await tcs.Task;
    }

    public static Task WaitForConditionAsync(
        Func<bool> condition,
        TimeSpan? timeout = null,
        string? timeoutMessage = null,
        TimeSpan? pollInterval = null)
        => WaitForConditionAsync(
            () => Task.FromResult(condition()),
            timeout,
            timeoutMessage,
            transientExceptionFilter: null,
            pollInterval);

    public static async Task WaitForConditionAsync(
        Func<Task<bool>> condition,
        TimeSpan? timeout = null,
        string? timeoutMessage = null,
        Func<Exception, bool>? transientExceptionFilter = null,
        TimeSpan? pollInterval = null,
        Func<string>? timeoutMessageFactory = null)
    {
        using var cts = new CancellationTokenSource(timeout ?? DefaultEventTimeout);
        Exception? lastTransientException = null;

        while (true)
        {
            try
            {
                if (await condition())
                {
                    return;
                }

                lastTransientException = null;
            }
            catch (Exception ex) when (transientExceptionFilter?.Invoke(ex) == true)
            {
                lastTransientException = ex;
            }

            try
            {
                await Task.Delay(pollInterval ?? DefaultPollInterval, cts.Token);
            }
            catch (OperationCanceledException) when (cts.IsCancellationRequested)
            {
                break;
            }
        }

        try
        {
            if (await condition())
            {
                return;
            }
        }
        catch (Exception ex) when (transientExceptionFilter?.Invoke(ex) == true)
        {
            lastTransientException = ex;
        }

        var message = timeoutMessageFactory?.Invoke() ?? timeoutMessage ?? "Timed out waiting for condition.";
        throw lastTransientException is null
            ? new TimeoutException(message)
            : new TimeoutException(message, lastTransientException);
    }

    public static bool IsTransientFileSystemException(Exception exception)
        => exception is IOException or UnauthorizedAccessException;

    public static string ExtensionLaunchMarkers(string homeDir, string extensionId)
    {
        try
        {
            var logsDir = Path.Join(homeDir, "logs");
            if (!Directory.Exists(logsDir))
            {
                return "<no process logs>";
            }

            var extensionName = extensionId[(extensionId.LastIndexOf(':') + 1)..];
            var launches = new List<string>();
            foreach (var path in Directory.EnumerateFiles(logsDir, "process-*.log"))
            {
                using var reader = new StreamReader(path);
                if (reader.ReadLine()?.Contains(extensionName, StringComparison.Ordinal) != true)
                {
                    continue;
                }

                var markers = new List<string>();
                var errors = new HashSet<string>(StringComparer.Ordinal);
                while (reader.ReadLine() is { } line)
                {
                    if (line.StartsWith("=== ", StringComparison.Ordinal))
                    {
                        if (!line.Contains("module=", StringComparison.Ordinal))
                        {
                            markers.Add(line);
                        }
                        continue;
                    }

                    if (line.StartsWith("[extension-bootstrap] Failed to load extension:", StringComparison.Ordinal))
                    {
                        errors.Add("bootstrap import failed");
                        var joinError = Regex.Match(
                            line,
                            @"Request (session\.(?:resume|options\.update)) failed with message: (.*)$");
                        if (joinError.Success)
                        {
                            // Only emit fixed vocabulary, never the exception's paths, IDs, or credentials.
                            var terms = Regex.Matches(
                                joinError.Groups[2].Value,
                                @"\b(?:access|already|authentication|callback|cancelled|closed|connection|context|duplicate|event|failed|file|found|hook|invalid|lock|missing|model|not|owner|path|permission|policy|provider|register|registration|rejected|response|resume|session|stale|state|timeout|tool|unavailable|unknown|workspace)\b",
                                RegexOptions.IgnoreCase | RegexOptions.CultureInvariant)
                                .Cast<Match>()
                                .Select(match => match.Value.ToLowerInvariant())
                                .Take(20);
                            errors.Add($"{joinError.Groups[1].Value} reason terms: {string.Join(" ", terms)}");
                        }
                    }

                    if (line is "[sdk-extension-test] imported" or "[sdk-extension-test] joining"
                        or "[sdk-extension-test] joined" or "[sdk-extension-test] logged"
                        || Regex.IsMatch(line, @"^\[sdk-extension-test\] join failed code=(?:-?\d+|none)$"))
                    {
                        markers.Add(line);
                    }

                    var code = Regex.Match(line, @"\b(?:ERR_[A-Z0-9_]+|ECONNRESET|ECONNREFUSED|EPIPE|ENOENT|ETIMEDOUT)\b");
                    if (code.Success)
                    {
                        errors.Add(code.Value);
                    }
                    var type = Regex.Match(line, @"(?<![\w.])(?:TypeError|ReferenceError|SyntaxError|RangeError|AggregateError|AssertionError|Error)(?=:| \[)");
                    if (type.Success)
                    {
                        errors.Add(type.Value);
                    }
                }
                var summary = string.Join("; ", markers);
                if (errors.Count > 0)
                {
                    summary += $"; error categories: {string.Join(", ", errors.OrderBy(error => error, StringComparer.Ordinal))}";
                }
                launches.Add(summary.Length == 0 ? "<no lifecycle markers>" : summary);
            }

            return launches.Count == 0 ? "<no matching launch logs>" : string.Join(" | ", launches);
        }
        catch (Exception error) when (error is IOException or UnauthorizedAccessException)
        {
            return $"<launch logs unavailable: {error.GetType().Name}>";
        }
    }
}
