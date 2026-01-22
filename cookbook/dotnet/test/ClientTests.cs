/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;

namespace GitHub.Copilot.SDK.Test;

// These tests bypass E2ETestBase because they are about how the CLI subprocess is started
// Other test classes should instead inherit from E2ETestBase
public class ClientTests : IAsyncLifetime
{
    private string _cliPath = null!;

    public Task InitializeAsync()
    {
        _cliPath = GetCliPath();
        return Task.CompletedTask;
    }

    public Task DisposeAsync() => Task.CompletedTask;

    private static string GetCliPath()
    {
        var envPath = Environment.GetEnvironmentVariable("COPILOT_CLI_PATH");
        if (!string.IsNullOrEmpty(envPath)) return envPath;

        var dir = new DirectoryInfo(AppContext.BaseDirectory);
        while (dir != null)
        {
            var path = Path.Combine(dir.FullName, "nodejs/node_modules/@github/copilot/index.js");
            if (File.Exists(path)) return path;
            dir = dir.Parent;
        }
        throw new InvalidOperationException("CLI not found. Run 'npm install' in the nodejs directory first.");
    }

    [Fact]
    public async Task Should_Start_And_Connect_To_Server_Using_Stdio()
    {
        using var client = new CopilotClient(new CopilotClientOptions { CliPath = _cliPath, UseStdio = true });

        try
        {
            await client.StartAsync();
            Assert.Equal(ConnectionState.Connected, client.State);

            var pong = await client.PingAsync("test message");
            Assert.Equal("pong: test message", pong.Message);
            Assert.True(pong.Timestamp >= 0);

            await client.StopAsync();
            Assert.Equal(ConnectionState.Disconnected, client.State);
        }
        finally
        {
            await client.ForceStopAsync();
        }
    }

    [Fact]
    public async Task Should_Start_And_Connect_To_Server_Using_Tcp()
    {
        using var client = new CopilotClient(new CopilotClientOptions { CliPath = _cliPath, UseStdio = false });

        try
        {
            await client.StartAsync();
            Assert.Equal(ConnectionState.Connected, client.State);

            var pong = await client.PingAsync("test message");
            Assert.Equal("pong: test message", pong.Message);

            await client.StopAsync();
        }
        finally
        {
            await client.ForceStopAsync();
        }
    }

    [Fact]
    public async Task Should_Force_Stop_Without_Cleanup()
    {
        using var client = new CopilotClient(new CopilotClientOptions { CliPath = _cliPath });

        await client.CreateSessionAsync();
        await client.ForceStopAsync();

        Assert.Equal(ConnectionState.Disconnected, client.State);
    }
}
