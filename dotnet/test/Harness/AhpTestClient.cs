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
    private readonly SemaphoreSlim _gate = new(1, 1);

    private AhpTestClient(Process process) => _process = process;

    public static async Task<AhpTestClient> StartAsync()
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
        info.ArgumentList.Add("test/e2e/harness/ahpTestDriver.ts");
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

    public async Task<JsonElement> RequestAsync(JsonObject command)
    {
        await _gate.WaitAsync();
        try
        {
            await _process.StandardInput.WriteLineAsync(command.ToJsonString());
            await _process.StandardInput.FlushAsync();
            var response = await ReadAsync();
            if (response.TryGetProperty("error", out var error)) throw new InvalidOperationException(error.GetString());
            return response.GetProperty("result").Clone();
        }
        finally { _gate.Release(); }
    }

    private async Task<JsonElement> ReadAsync()
    {
        var line = await _process.StandardOutput.ReadLineAsync().WaitAsync(TimeSpan.FromSeconds(60));
        if (line is null) throw new IOException("AHP test client closed its output unexpectedly");
        using var document = JsonDocument.Parse(line);
        return document.RootElement.Clone();
    }

    public async ValueTask DisposeAsync()
    {
        try
        {
            _process.StandardInput.Close();
            try { await _process.WaitForExitAsync().WaitAsync(TimeSpan.FromSeconds(10)); }
            catch (TimeoutException)
            {
                _process.Kill();
                await _process.WaitForExitAsync();
                throw;
            }
            if (_process.ExitCode != 0) throw new IOException($"AHP test client exited with status {_process.ExitCode}");
        }
        finally { _process.Dispose(); _gate.Dispose(); }
    }
}
#endif
