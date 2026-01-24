/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Diagnostics;
using System.Net.Http.Json;
using System.Runtime.InteropServices;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Text.RegularExpressions;

namespace GitHub.Copilot.SDK.Test.Harness;

public partial class CapiProxy : IAsyncDisposable
{
    private Process? _process;
    private Task<string>? _startupTask;

    public Task<string> StartAsync()
    {
        return _startupTask ??= StartCoreAsync();

        async Task<string> StartCoreAsync()
        {
            string filename;
            string args;

            if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
            {
                filename = "cmd.exe";
                args = "/c npm.cmd run start";

            }
            else
            {
                filename = "npm";
                args = "run start";
            }

            var startInfo = new ProcessStartInfo
            {
                FileName = filename,
                WorkingDirectory = Path.Join(FindRepoRoot(), "test", "harness"),
                Arguments = args,
                UseShellExecute = false,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                CreateNoWindow = true,
            };

            _process = new Process { StartInfo = startInfo };

            var tcs = new TaskCompletionSource<string>();
            var errorOutput = new StringBuilder();

            _process.OutputDataReceived += (_, e) =>
            {
                if (e.Data == null) return;
                var match = Regex.Match(e.Data, @"Listening: (http://[^\s]+)");
                if (match.Success) tcs.TrySetResult(match.Groups[1].Value);
            };

            _process.ErrorDataReceived += (_, e) =>
            {
                if (e.Data == null) return;
                errorOutput.AppendLine(e.Data);
            };

            _process.Start();
            _process.BeginOutputReadLine();
            _process.BeginErrorReadLine();
            _ = _process.WaitForExitAsync().ContinueWith(_ =>
            {
                if (_process?.ExitCode is int exitCode && exitCode != 0)
                {
                    tcs.TrySetException(new Exception($"Proxy exited with code {_process.ExitCode}: {errorOutput}"));
                }
            });

            // Use longer timeout on Windows due to slower process startup
            var timeoutSeconds = RuntimeInformation.IsOSPlatform(OSPlatform.Windows) ? 30 : 10;
            using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(timeoutSeconds));
            cts.Token.Register(() => tcs.TrySetException(new TimeoutException("Timeout waiting for proxy")));

            return await tcs.Task;
        }
    }

    public async Task StopAsync(bool skipWritingCache = false)
    {
        if (_startupTask != null)
        {
            try
            {
                var url = await _startupTask;
                var stopUrl = skipWritingCache ? $"{url}/stop?skipWritingCache=true" : $"{url}/stop";
                using var client = new HttpClient();
                await client.PostAsync(stopUrl, null);
            }
            catch { /* Best effort */ }
        }

        if (_process is { HasExited: false })
        {
            try { _process.Kill(); await _process.WaitForExitAsync(); }
            catch { /* Ignore */ }
        }

        _process = null;
        _startupTask = null;
    }

    public async Task ConfigureAsync(string filePath, string workDir)
    {
        var url = await (_startupTask ?? throw new InvalidOperationException("Proxy not started"));

        using var client = new HttpClient();
        var response = await client.PostAsJsonAsync($"{url}/config", new ConfigureRequest(filePath, workDir), CapiProxyJsonContext.Default.ConfigureRequest);
        response.EnsureSuccessStatusCode();
    }

    private record ConfigureRequest(string FilePath, string WorkDir);

    public async Task<List<ParsedHttpExchange>> GetExchangesAsync()
    {
        var url = await (_startupTask ?? throw new InvalidOperationException("Proxy not started"));

        using var client = new HttpClient();
        return await client.GetFromJsonAsync($"{url}/exchanges", CapiProxyJsonContext.Default.ListParsedHttpExchange)
               ?? new List<ParsedHttpExchange>();
    }

    public async ValueTask DisposeAsync() => await StopAsync();

    private static string FindRepoRoot()
    {
        var dir = new DirectoryInfo(AppContext.BaseDirectory);
        while (dir != null)
        {
            if (File.Exists(Path.Combine(dir.FullName, "justfile")))
                return dir.FullName;
            dir = dir.Parent;
        }
        throw new InvalidOperationException("Could not find repository root");
    }

    [JsonSourceGenerationOptions(JsonSerializerDefaults.Web)]
    [JsonSerializable(typeof(ConfigureRequest))]
    [JsonSerializable(typeof(List<ParsedHttpExchange>))]
    private partial class CapiProxyJsonContext : JsonSerializerContext;
}

public record ParsedHttpExchange(ChatCompletionRequest Request, ChatCompletionResponse? Response);

public record ChatCompletionRequest(
    string Model,
    List<ChatCompletionMessage> Messages,
    List<ChatCompletionTool>? Tools);

public record ChatCompletionMessage(
    string Role,
    string? Content,
    [property: JsonPropertyName("tool_call_id")] string? ToolCallId,
    [property: JsonPropertyName("tool_calls")] List<ChatCompletionToolCall>? ToolCalls);

public record ChatCompletionToolCall(string Id, string Type, ChatCompletionToolCallFunction Function);

public record ChatCompletionToolCallFunction(string Name, string? Arguments);

public record ChatCompletionTool(string Type, ChatCompletionToolFunction Function);

public record ChatCompletionToolFunction(string Name, string? Description);

public record ChatCompletionResponse(string Id, string Model, List<ChatCompletionChoice> Choices);

public record ChatCompletionChoice(int Index, ChatCompletionMessage Message, [property: JsonPropertyName("finish_reason")] string FinishReason);
