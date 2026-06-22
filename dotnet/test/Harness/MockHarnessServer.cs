/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Diagnostics;
using System.Net.Http;
using System.Runtime.InteropServices;
using System.Text;
using System.Text.Json;
using System.Text.RegularExpressions;

namespace GitHub.Copilot.Test.Harness;

/// <summary>
/// Base class that spawns one of the shared mock servers in <c>test/harness</c>
/// (via its <c>npm run start:*</c> script, mirroring <see cref="CapiProxy"/>) and
/// parses its single-line <c>Listening: {json}</c> startup banner. The parsed
/// connection info is exposed as a <see cref="JsonElement"/> so subclasses can
/// pull whatever fields their endpoint advertises.
///
/// These servers live in the shared harness so every SDK language drives the
/// exact same mock endpoints rather than re-implementing them per language.
/// </summary>
public abstract class MockHarnessServer : IAsyncDisposable
{
    private Process? _process;
    private Task<JsonElement>? _startupTask;

    /// <summary>The npm script (in <c>test/harness/package.json</c>) that launches this server.</summary>
    protected abstract string NpmScript { get; }

    /// <summary>Human-readable name used in error messages.</summary>
    protected abstract string DisplayName { get; }

    /// <summary>Parsed <c>Listening:</c> banner; available after <see cref="StartAsync"/> completes.</summary>
    protected JsonElement Info { get; private set; }

    private static readonly HttpClient HttpClient = new();

    public Task StartAsync()
    {
        return EnsureStartedAsync();
    }

    protected async Task<JsonElement> EnsureStartedAsync()
    {
        Info = await (_startupTask ??= StartCoreAsync());
        return Info;
    }

    private async Task<JsonElement> StartCoreAsync()
    {
        string filename;
        string args;
        if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
        {
            filename = "cmd.exe";
            args = $"/c npm.cmd run {NpmScript}";
        }
        else
        {
            filename = "npm";
            args = $"run {NpmScript}";
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

        var tcs = new TaskCompletionSource<JsonElement>();
        var errorOutput = new StringBuilder();

        _process.OutputDataReceived += (_, e) =>
        {
            if (e.Data == null) return;
            var match = Regex.Match(e.Data, @"^Listening: (?<json>\{.*\})$");
            if (!match.Success)
            {
                return;
            }
            try
            {
                using var doc = JsonDocument.Parse(match.Groups["json"].Value);
                tcs.TrySetResult(doc.RootElement.Clone());
            }
            catch (Exception ex) when (ex is JsonException or NotSupportedException)
            {
                tcs.TrySetException(
                    new InvalidOperationException(
                        $"Failed to parse {DisplayName} startup metadata: {match.Groups["json"].Value}",
                        ex));
            }
        };

        _process.ErrorDataReceived += (_, e) =>
        {
            if (e.Data == null) return;
            errorOutput.AppendLine(e.Data);
            Console.Error.WriteLine(e.Data);
        };

        _process.Start();
        _process.BeginOutputReadLine();
        _process.BeginErrorReadLine();
        _ = _process.WaitForExitAsync().ContinueWith(_ =>
        {
            if (_process?.ExitCode is int exitCode && exitCode != 0)
            {
                tcs.TrySetException(
                    new Exception($"{DisplayName} exited with code {exitCode}: {errorOutput}"));
            }
        });

        // Longer timeout on Windows due to slower process startup (matches CapiProxy).
        var timeoutSeconds = RuntimeInformation.IsOSPlatform(OSPlatform.Windows) ? 30 : 10;
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(timeoutSeconds));
        cts.Token.Register(() => tcs.TrySetException(new TimeoutException($"Timeout waiting for {DisplayName}")));

        return await tcs.Task;
    }

    /// <summary>Returns a required string field from the startup banner.</summary>
    protected string InfoString(string property)
    {
        return Info.GetProperty(property).GetString()
            ?? throw new InvalidOperationException($"{DisplayName} banner missing string '{property}'");
    }

    /// <summary>GETs the given control URL and returns the parsed JSON document.</summary>
    protected static async Task<JsonDocument> GetJsonAsync(string url)
    {
        var json = await HttpClient.GetStringAsync(url);
        return JsonDocument.Parse(json);
    }

    /// <summary>POSTs to the given control URL with an optional JSON body.</summary>
    protected static async Task PostAsync(string url, string? jsonBody = null)
    {
        using var content = jsonBody == null
            ? null
            : new StringContent(jsonBody, Encoding.UTF8, "application/json");
        using var response = await HttpClient.PostAsync(url, content);
        response.EnsureSuccessStatusCode();
    }

    public async Task StopAsync()
    {
        if (_startupTask != null)
        {
            try
            {
                var info = await _startupTask;
                if (info.TryGetProperty("stopUrl", out var stopUrl) && stopUrl.GetString() is string url)
                {
                    await PostAsync(url);
                }
            }
            catch { /* best effort; fall through to killing the process */ }
        }

        if (_process is { HasExited: false })
        {
            try { _process.Kill(entireProcessTree: true); await _process.WaitForExitAsync(); }
            catch { /* ignore */ }
        }

        _process?.Dispose();
        _process = null;
        _startupTask = null;
    }

    public async ValueTask DisposeAsync()
    {
        await StopAsync();
        GC.SuppressFinalize(this);
    }

    private static string FindRepoRoot()
    {
        var dir = new DirectoryInfo(AppContext.BaseDirectory);
        while (dir != null)
        {
            if (Directory.Exists(Path.Combine(dir.FullName, "nodejs")))
                return dir.FullName;
            dir = dir.Parent;
        }
        throw new InvalidOperationException("Could not find repository root");
    }
}

/// <summary>
/// Spawns the shared mock Azure managed identity token endpoint
/// (<c>test/harness/mockIdentityServer.ts</c>) and exposes its endpoint + control
/// URLs. The C# reference consumer of the shared endpoint; mirrors the Node
/// <c>MockIdentityServer</c> wrapper.
/// </summary>
public sealed class MockIdentityServer : MockHarnessServer
{
    protected override string NpmScript => "start:mock-identity";
    protected override string DisplayName => "mock identity server";

    /// <summary>Value to assign to the <c>IDENTITY_ENDPOINT</c> env var.</summary>
    public string Endpoint => InfoString("endpoint");

    /// <summary>Secret to assign to the <c>IDENTITY_HEADER</c> env var.</summary>
    public string Header => InfoString("header");

    /// <summary>Fake bearer token the runtime injects (<c>Authorization: Bearer &lt;token&gt;</c>).</summary>
    public string Token => InfoString("token");

    /// <summary>Token requests recorded by the endpoint so far, in arrival order.</summary>
    public async Task<List<RecordedIdentityRequest>> GetRecordedRequestsAsync()
    {
        using var doc = await GetJsonAsync(InfoString("recordedUrl"));
        var result = new List<RecordedIdentityRequest>();
        foreach (var element in doc.RootElement.EnumerateArray())
        {
            var identityParams = new Dictionary<string, string>(StringComparer.Ordinal);
            if (element.TryGetProperty("identityParams", out var p) && p.ValueKind == JsonValueKind.Object)
            {
                foreach (var prop in p.EnumerateObject())
                {
                    identityParams[prop.Name] = prop.Value.GetString() ?? string.Empty;
                }
            }
            result.Add(new RecordedIdentityRequest(
                Resource: GetNullableString(element, "resource"),
                IdentityHeader: GetNullableString(element, "identityHeader"),
                IdentityParams: identityParams,
                IssuedToken: GetNullableString(element, "issuedToken") ?? string.Empty));
        }
        return result;
    }

    /// <summary>Clears recorded requests and restores the default token behaviour.</summary>
    public Task ResetAsync() => PostAsync(InfoString("resetUrl"));

    /// <summary>Sets the endpoint's token lifetime / rotation behaviour.</summary>
    public Task ConfigureAsync(int? expiresInSeconds = null, bool? rotateTokens = null)
    {
        var fields = new List<string>();
        if (expiresInSeconds.HasValue)
        {
            fields.Add($"\"expiresInSeconds\":{expiresInSeconds.Value}");
        }
        if (rotateTokens.HasValue)
        {
            fields.Add($"\"rotateTokens\":{(rotateTokens.Value ? "true" : "false")}");
        }
        return PostAsync(InfoString("configureUrl"), "{" + string.Join(",", fields) + "}");
    }

    private static string? GetNullableString(JsonElement element, string property)
    {
        return element.TryGetProperty(property, out var value) && value.ValueKind == JsonValueKind.String
            ? value.GetString()
            : null;
    }
}

/// <summary>
/// Spawns the shared mock BYOK model (OpenAI-compatible) endpoint
/// (<c>test/harness/mockModelServer.ts</c>) and exposes its base + control URLs.
/// The C# reference consumer of the shared endpoint; mirrors the Node
/// <c>MockModelServer</c> wrapper.
/// </summary>
public sealed class MockModelServer : MockHarnessServer
{
    protected override string NpmScript => "start:mock-model";
    protected override string DisplayName => "mock model server";

    /// <summary>Base URL to assign as the BYOK provider's <c>baseUrl</c>.</summary>
    public string BaseUrl => InfoString("baseUrl");

    /// <summary>Inference requests recorded by the endpoint so far, in arrival order.</summary>
    public async Task<List<RecordedModelRequest>> GetRecordedRequestsAsync()
    {
        using var doc = await GetJsonAsync(InfoString("recordedUrl"));
        var result = new List<RecordedModelRequest>();
        foreach (var element in doc.RootElement.EnumerateArray())
        {
            result.Add(new RecordedModelRequest(
                Authorization: element.TryGetProperty("authorization", out var a) && a.ValueKind == JsonValueKind.String
                    ? a.GetString()
                    : null,
                Path: element.TryGetProperty("path", out var p) ? p.GetString() ?? string.Empty : string.Empty,
                Method: element.TryGetProperty("method", out var m) ? m.GetString() ?? string.Empty : string.Empty));
        }
        return result;
    }

    /// <summary>Clears the endpoint's recorded inference requests.</summary>
    public Task ResetAsync() => PostAsync(InfoString("resetUrl"));
}

/// <summary>A token request the runtime made to the mock identity endpoint.</summary>
public sealed record RecordedIdentityRequest(
    string? Resource,
    string? IdentityHeader,
    Dictionary<string, string> IdentityParams,
    string IssuedToken);

/// <summary>An inference request the runtime made to the mock model endpoint.</summary>
public sealed record RecordedModelRequest(
    string? Authorization,
    string Path,
    string Method);
