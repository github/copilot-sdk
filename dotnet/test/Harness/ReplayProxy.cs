/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Microsoft.Win32.SafeHandles;
using System.Diagnostics;
using System.Net.Http;
using System.Net.Http.Json;
using System.Runtime.InteropServices;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Text.RegularExpressions;

namespace GitHub.Copilot.Test.Harness;

public sealed partial class ReplayProxy : IAsyncDisposable
{
    private Process? _process;
    private SafeJobHandle? _processJob;
    private Task<string>? _startupTask;

    public string? ConnectProxyUrl { get; private set; }
    public string? CaFilePath { get; private set; }

    internal static HttpClient CreateExchangeHttpClient(CancellationToken cancellationToken)
    {
        var client = new HttpClient();
        if (cancellationToken.CanBeCanceled)
        {
            // Only remove the competing HTTP deadline when the caller owns cancellation.
            client.Timeout = Timeout.InfiniteTimeSpan;
        }
        return client;
    }

    public Task<string> StartAsync()
    {
        return _startupTask ??= StartCoreAsync();

        async Task<string> StartCoreAsync()
        {
            // Spawn the server directly so a stalled npm/cmd wrapper cannot strand startup or cleanup.
            var startInfo = new ProcessStartInfo
            {
                FileName = "node",
                WorkingDirectory = Path.Join(FindRepoRoot(), "test", "harness"),
                Arguments = "startup.mjs",
                UseShellExecute = false,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                CreateNoWindow = true,
            };

            var process = new Process { StartInfo = startInfo };
            var diagnostics = new StartupDiagnostics();
            var elapsed = Stopwatch.StartNew();

            process.OutputDataReceived += (_, e) =>
            {
                if (e.Data == null) return;
                diagnostics.AppendOutput(e.Data);
                diagnostics.PublishReadiness();
            };

            process.ErrorDataReceived += (_, e) =>
            {
                if (e.Data == null) return;
                diagnostics.AppendError(e.Data);
                if (!e.Data.StartsWith("[SDK proxy startup]", StringComparison.Ordinal))
                {
                    Console.Error.WriteLine(e.Data);
                }
            };

            try
            {
                process.Start();
            }
            catch
            {
                process.Dispose();
                throw;
            }
            _processJob = TryBindToKillOnCloseJob(process);
            _process = process;
            process.BeginOutputReadLine();
            process.BeginErrorReadLine();
            _ = process.WaitForExitAsync().ContinueWith(_ =>
            {
                if (!diagnostics.Readiness.IsCompleted)
                {
                    diagnostics.FailStartup(new Exception(
                        $"Proxy exited before readiness. {diagnostics.Describe(process.Id, elapsed.Elapsed, process.ExitCode)}"));
                }
            });

            // Use longer timeout on Windows due to slower process startup
            var timeoutSeconds = RuntimeInformation.IsOSPlatform(OSPlatform.Windows) ? 30 : 10;
            using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(timeoutSeconds));
            using var registration = cts.Token.Register(() => diagnostics.TimeOutStartup(new TimeoutException(
                $"Timeout waiting for proxy. {diagnostics.Describe(process.Id, elapsed.Elapsed, process.HasExited ? process.ExitCode : null)}")));

            var readiness = await diagnostics.Readiness;
            ConnectProxyUrl = readiness.ConnectProxyUrl;
            CaFilePath = readiness.CaFilePath;
            return readiness.Url;
        }
    }

    internal sealed record StartupReadiness(string Url, string ConnectProxyUrl, string CaFilePath);

    internal sealed class StartupDiagnostics
    {
        private const int OutputLimit = 4096;
        private readonly object _gate = new();
        private readonly StringBuilder _stdout = new();
        private readonly StringBuilder _stderr = new();
        private readonly TaskCompletionSource<StartupReadiness> _readiness =
            new(TaskCreationOptions.RunContinuationsAsynchronously);
        private string? _listeningLine;

        public Task<StartupReadiness> Readiness => _readiness.Task;

        public void AppendOutput(string line) => Append(_stdout, line, captureReadiness: true);

        public void AppendError(string line) => Append(_stderr, line, captureReadiness: false);

        private void Append(StringBuilder output, string line, bool captureReadiness)
        {
            lock (_gate)
            {
                if (captureReadiness && line.Contains("Listening: ", StringComparison.Ordinal))
                {
                    _listeningLine ??= line;
                }
                output.Append(line, Math.Max(0, line.Length - OutputLimit), Math.Min(line.Length, OutputLimit));
                output.AppendLine();
                if (output.Length > OutputLimit)
                {
                    output.Remove(0, output.Length - OutputLimit);
                }
            }
        }

        public void PublishReadiness()
        {
            if (_readiness.Task.IsCompleted) return;
            string? line;
            lock (_gate)
            {
                line = _listeningLine;
            }
            if (line is null) return;
            var match = Regex.Match(line, @"Listening: (?<url>http://[^\s]+)\s+(?<metadata>\{.*\})$");
            if (!match.Success)
            {
                FailStartup(new InvalidOperationException(
                    $"Proxy startup line missing CONNECT proxy metadata: {line}"));
                return;
            }
            ProxyStartupMetadata? metadata;
            try
            {
                metadata = JsonSerializer.Deserialize(
                    match.Groups["metadata"].Value,
                    ReplayProxyJsonContext.Default.ProxyStartupMetadata);
            }
            catch (Exception ex) when (ex is JsonException or NotSupportedException)
            {
                FailStartup(new InvalidOperationException(
                    $"Failed to parse proxy startup metadata: {match.Groups["metadata"].Value}", ex));
                return;
            }
            if (metadata is not
                {
                    ConnectProxyUrl: { Length: > 0 } connectProxyUrl,
                    CaFilePath: { Length: > 0 } caFilePath,
                })
            {
                FailStartup(new InvalidOperationException(
                    $"Proxy startup metadata missing CONNECT proxy details: {line}"));
                return;
            }
            _readiness.TrySetResult(new StartupReadiness(
                match.Groups["url"].Value, connectProxyUrl, caFilePath));
        }

        public bool FailStartup(Exception error) => _readiness.TrySetException(error);

        public bool TimeOutStartup(TimeoutException error)
        {
            lock (_gate)
            {
                if (_listeningLine is null)
                {
                    return FailStartup(error);
                }
            }
            PublishReadiness();
            return FailStartup(error);
        }

        public string Describe(int processId, TimeSpan elapsed, int? exitCode)
        {
            lock (_gate)
            {
                var state = exitCode is int code ? $"exited with code {code}" : "running";
                return $"PID {processId}, {state}, elapsed {elapsed.TotalSeconds:F1}s." +
                    $"\n--- stdout tail ---\n{_stdout}\n--- stderr tail ---\n{_stderr}";
            }
        }
    }

    public async Task StopAsync(bool skipWritingCache = false)
    {
        Exception? shutdownFailure = null;
        string? url = null;
        if (_startupTask != null)
        {
            try
            {
                url = await _startupTask;
            }
            catch { /* Startup failure is reported by StartAsync; cleanup still owns the process. */ }
        }

        if (url != null)
        {
            try
            {
                var stopUrl = skipWritingCache ? $"{url}/stop?skipWritingCache=true" : $"{url}/stop";
                using var client = new HttpClient();
                using var response = await client.PostAsync(stopUrl, null);
                if (!response.IsSuccessStatusCode)
                {
                    shutdownFailure = new HttpRequestException(
                        $"Proxy shutdown failed: HTTP {(int)response.StatusCode}: {await response.Content.ReadAsStringAsync()}");
                }
            }
            catch (Exception ex) when (ex is HttpRequestException or OperationCanceledException or IOException)
            {
                shutdownFailure = ex;
            }
        }

        try
        {
            if (_process is { HasExited: false })
            {
                using var timeout = new CancellationTokenSource(TimeSpan.FromSeconds(5));
                try
                {
                    await _process.WaitForExitAsync(timeout.Token);
                }
                catch (OperationCanceledException)
                {
                    _process.Kill(entireProcessTree: true);
                    await _process.WaitForExitAsync();
                }
            }
            if (url != null && _process is { HasExited: true, ExitCode: not 0 })
            {
                shutdownFailure ??= new InvalidOperationException($"Proxy exited with code {_process.ExitCode}");
            }
        }
        catch (InvalidOperationException)
        {
            // Process.Start failed before this object was associated with a child.
        }

        finally
        {
            _process?.Dispose();
            _process = null;
            _processJob?.Dispose();
            _processJob = null;
            _startupTask = null;
        }
        if (shutdownFailure != null)
        {
            throw shutdownFailure;
        }
    }

    public async Task ConfigureAsync(string filePath, string workDir, string backend, bool replayOnly = false)
    {
        var url = await (_startupTask ?? throw new InvalidOperationException("Proxy not started"));

        using var client = new HttpClient();
        var response = await client.PostAsJsonAsync(
            $"{url}/config",
            new ConfigureRequest(filePath, workDir, backend, replayOnly),
            ReplayProxyJsonContext.Default.ConfigureRequest);
        response.EnsureSuccessStatusCode();
    }

    private record ConfigureRequest(string FilePath, string WorkDir, string Backend, bool ReplayOnly);

    private record ProxyStartupMetadata(string? ConnectProxyUrl, string? CaFilePath);

    public async Task<List<ParsedHttpExchange>> GetExchangesAsync(CancellationToken cancellationToken = default)
    {
        var url = await (_startupTask ?? throw new InvalidOperationException("Proxy not started"))
            .WaitAsync(Timeout.InfiniteTimeSpan, cancellationToken);

        using var client = CreateExchangeHttpClient(cancellationToken);
        return await client.GetFromJsonAsync($"{url}/exchanges", ReplayProxyJsonContext.Default.ListParsedHttpExchange, cancellationToken)
               ?? [];
    }

    public async Task SetCopilotUserByTokenAsync(string token, CopilotUserConfig response)
    {
        var url = await (_startupTask ?? throw new InvalidOperationException("Proxy not started"));

        using var client = new HttpClient();
        var payload = new CopilotUserByTokenRequest(token, response);
        var resp = await client.PostAsJsonAsync($"{url}/copilot-user-config", payload, ReplayProxyJsonContext.Default.CopilotUserByTokenRequest);
        resp.EnsureSuccessStatusCode();
    }

    public async ValueTask DisposeAsync()
    {
        await StopAsync();
    }

    private static string FindRepoRoot()
    {
        var dir = new DirectoryInfo(AppContext.BaseDirectory);
        while (dir != null)
        {
            if (File.Exists(Path.Combine(dir.FullName, "test", "harness", "package.json")))
                return dir.FullName;
            dir = dir.Parent;
        }
        throw new InvalidOperationException("Could not find repository root");
    }

    // On Windows, puts the proxy and everything it starts in a job that Windows terminates when
    // its last handle closes. Only this process holds that handle (it is not inheritable), so the
    // proxy dies with the test host even when the host crashes. Otherwise the orphaned proxy keeps
    // the host's inherited stdout/stderr pipes open and `dotnet test` waits on them indefinitely.
    private static SafeJobHandle? TryBindToKillOnCloseJob(Process process)
    {
        if (!RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
        {
            return null;
        }

        var job = CreateJobObjectW(IntPtr.Zero, IntPtr.Zero);
        var limits = new JobObjectExtendedLimitInformation();
        limits.BasicLimitInformation.LimitFlags = JobObjectLimitKillOnJobClose;
        if (job.IsInvalid
            || !SetInformationJobObject(
                job,
                JobObjectExtendedLimitInformationClass,
                ref limits,
                (uint)Marshal.SizeOf<JobObjectExtendedLimitInformation>())
            || !AssignProcessToJobObject(job, process.SafeHandle))
        {
            var error = Marshal.GetLastWin32Error();
            job.Dispose();
            Console.Error.WriteLine(
                $"[SDK proxy] Could not bind PID {process.Id} to a kill-on-close job (Win32 error {error}); it can outlive a crashed test host.");
            return null;
        }

        return job;
    }

    private const int JobObjectExtendedLimitInformationClass = 9;
    private const uint JobObjectLimitKillOnJobClose = 0x2000;

    [DllImport("kernel32", SetLastError = true)]
    private static extern SafeJobHandle CreateJobObjectW(IntPtr jobAttributes, IntPtr name);

    [DllImport("kernel32", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool SetInformationJobObject(
        SafeJobHandle job,
        int infoClass,
        ref JobObjectExtendedLimitInformation info,
        uint infoLength);

    [DllImport("kernel32", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool AssignProcessToJobObject(SafeJobHandle job, SafeHandle process);

    [DllImport("kernel32", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool CloseHandle(IntPtr handle);

    private sealed class SafeJobHandle : SafeHandleZeroOrMinusOneIsInvalid
    {
        public SafeJobHandle()
            : base(ownsHandle: true)
        {
        }

        protected override bool ReleaseHandle() => CloseHandle(handle);
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct JobObjectBasicLimitInformation
    {
        public long PerProcessUserTimeLimit;
        public long PerJobUserTimeLimit;
        public uint LimitFlags;
        public UIntPtr MinimumWorkingSetSize;
        public UIntPtr MaximumWorkingSetSize;
        public uint ActiveProcessLimit;
        public UIntPtr Affinity;
        public uint PriorityClass;
        public uint SchedulingClass;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct IoCounters
    {
        public ulong ReadOperationCount;
        public ulong WriteOperationCount;
        public ulong OtherOperationCount;
        public ulong ReadTransferCount;
        public ulong WriteTransferCount;
        public ulong OtherTransferCount;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct JobObjectExtendedLimitInformation
    {
        public JobObjectBasicLimitInformation BasicLimitInformation;
        public IoCounters IoInfo;
        public UIntPtr ProcessMemoryLimit;
        public UIntPtr JobMemoryLimit;
        public UIntPtr PeakProcessMemoryUsed;
        public UIntPtr PeakJobMemoryUsed;
    }

    [JsonSourceGenerationOptions(JsonSerializerDefaults.Web)]
    [JsonSerializable(typeof(ConfigureRequest))]
    [JsonSerializable(typeof(List<ParsedHttpExchange>))]
    [JsonSerializable(typeof(CopilotUserByTokenRequest))]
    [JsonSerializable(typeof(Dictionary<string, CopilotUserQuotaSnapshot>))]
    [JsonSerializable(typeof(ProxyStartupMetadata))]
    private partial class ReplayProxyJsonContext : JsonSerializerContext;
}

public record CopilotUserByTokenRequest(string Token, CopilotUserConfig Response);

public record CopilotUserConfig(
    string Login,
    [property: JsonPropertyName("copilot_plan")]
    string CopilotPlan,
    CopilotUserEndpoints Endpoints,
    [property: JsonPropertyName("analytics_tracking_id")]
    string AnalyticsTrackingId,
    [property: JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [property: JsonPropertyName("quota_snapshots")]
    IReadOnlyDictionary<string, CopilotUserQuotaSnapshot>? QuotaSnapshots = null,
    [property: JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    long? Id = null);

public record CopilotUserEndpoints(string Api, string Telemetry);

public record CopilotUserQuotaSnapshot(
    [property: JsonPropertyName("entitlement")]
    int Entitlement,
    [property: JsonPropertyName("overage_count")]
    int OverageCount,
    [property: JsonPropertyName("overage_permitted")]
    bool OveragePermitted,
    [property: JsonPropertyName("percent_remaining")]
    double PercentRemaining,
    [property: JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [property: JsonPropertyName("timestamp_utc")]
    string? TimestampUtc = null,
    [property: JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [property: JsonPropertyName("unlimited")]
    bool? Unlimited = null);

public record ParsedHttpExchange(
    ChatCompletionRequest Request,
    ChatCompletionResponse? Response,
    Dictionary<string, JsonElement>? RequestHeaders,
    CompactionProviderUsage? CompactionUsage = null);

public record CompactionProviderUsage(string InteractionId, string Summary, int ResponseCount, long? InputTokens);

public record ChatCompletionRequest(
    string Model,
    List<ChatCompletionMessage> Messages,
    List<ChatCompletionTool>? Tools,
    [property: JsonPropertyName("tool_choice")] JsonElement? ToolChoice = null,
    [property: JsonPropertyName("response_format")] JsonElement? ResponseFormat = null);

public record ChatCompletionMessage(
    string Role,
    JsonElement? Content,
    [property: JsonPropertyName("tool_call_id")] string? ToolCallId,
    [property: JsonPropertyName("tool_calls")] List<ChatCompletionToolCall>? ToolCalls)
{
    /// <summary>
    /// Returns Content as a string when the JSON value is a string, or null otherwise.
    /// </summary>
    [JsonIgnore]
    public string? StringContent => Content is { ValueKind: JsonValueKind.String } c ? c.GetString() : null;
}

public record ChatCompletionToolCall(string Id, string Type, ChatCompletionToolCallFunction Function);

public record ChatCompletionToolCallFunction(string Name, string? Arguments);

public record ChatCompletionTool(string Type, ChatCompletionToolFunction Function);

public record ChatCompletionToolFunction(string Name, string? Description, JsonElement? Parameters);

public record ChatCompletionResponse(string Id, string Model, List<ChatCompletionChoice> Choices);

public record ChatCompletionChoice(int Index, ChatCompletionMessage Message, [property: JsonPropertyName("finish_reason")] string FinishReason);
