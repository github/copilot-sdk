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

    [Fact]
    public async Task Should_Get_Status_With_Version_And_Protocol_Info()
    {
        using var client = new CopilotClient(new CopilotClientOptions { CliPath = _cliPath, UseStdio = true });

        try
        {
            await client.StartAsync();

            var status = await client.GetStatusAsync();
            Assert.NotNull(status.Version);
            Assert.NotEmpty(status.Version);
            Assert.True(status.ProtocolVersion >= 1);

            await client.StopAsync();
        }
        finally
        {
            await client.ForceStopAsync();
        }
    }

    [Fact]
    public async Task Should_Get_Auth_Status()
    {
        using var client = new CopilotClient(new CopilotClientOptions { CliPath = _cliPath, UseStdio = true });

        try
        {
            await client.StartAsync();

            var authStatus = await client.GetAuthStatusAsync();
            // isAuthenticated is a bool, just verify we got a response
            if (authStatus.IsAuthenticated)
            {
                Assert.NotNull(authStatus.AuthType);
                Assert.NotNull(authStatus.StatusMessage);
            }

            await client.StopAsync();
        }
        finally
        {
            await client.ForceStopAsync();
        }
    }

    [Fact]
    public async Task Should_List_Models_When_Authenticated()
    {
        using var client = new CopilotClient(new CopilotClientOptions { CliPath = _cliPath, UseStdio = true });

        try
        {
            await client.StartAsync();

            var authStatus = await client.GetAuthStatusAsync();
            if (!authStatus.IsAuthenticated)
            {
                // Skip if not authenticated - models.list requires auth
                await client.StopAsync();
                return;
            }

            var models = await client.ListModelsAsync();
            Assert.NotNull(models);
            if (models.Count > 0)
            {
                var model = models[0];
                Assert.NotNull(model.Id);
                Assert.NotEmpty(model.Id);
                Assert.NotNull(model.Name);
                Assert.NotNull(model.Capabilities);
            }

            await client.StopAsync();
        }
        finally
        {
            await client.ForceStopAsync();
        }
    }

    [Fact]
    public async Task Should_Fire_SessionCreated_When_Session_Is_Created()
    {
        using var client = new CopilotClient(new CopilotClientOptions { CliPath = _cliPath, UseStdio = true });

        try
        {
            await client.StartAsync();

            CopilotSession? createdSession = null;
            client.SessionCreated += session => createdSession = session;

            var session = await client.CreateSessionAsync();

            Assert.NotNull(createdSession);
            Assert.Equal(session.SessionId, createdSession!.SessionId);
        }
        finally
        {
            await client.ForceStopAsync();
        }
    }

    [Fact]
    public async Task Should_Fire_SessionDestroyed_When_Session_Is_Disposed()
    {
        using var client = new CopilotClient(new CopilotClientOptions { CliPath = _cliPath, UseStdio = true });

        try
        {
            await client.StartAsync();

            string? destroyedSessionId = null;
            client.SessionDestroyed += id => destroyedSessionId = id;

            var session = await client.CreateSessionAsync();
            var sessionId = session.SessionId;

            Assert.Null(destroyedSessionId);

            await session.DisposeAsync();

            Assert.NotNull(destroyedSessionId);
            Assert.Equal(sessionId, destroyedSessionId);
        }
        finally
        {
            await client.ForceStopAsync();
        }
    }

    [Fact]
    public async Task Should_Fire_Events_For_Multiple_Sessions()
    {
        using var client = new CopilotClient(new CopilotClientOptions { CliPath = _cliPath, UseStdio = true });

        try
        {
            await client.StartAsync();

            var createdIds = new List<string>();
            var destroyedIds = new List<string>();
            client.SessionCreated += session => createdIds.Add(session.SessionId);
            client.SessionDestroyed += id => destroyedIds.Add(id);

            var session1 = await client.CreateSessionAsync();
            var session2 = await client.CreateSessionAsync();

            Assert.Equal(2, createdIds.Count);
            Assert.Contains(session1.SessionId, createdIds);
            Assert.Contains(session2.SessionId, createdIds);

            await session1.DisposeAsync();
            Assert.Single(destroyedIds);
            Assert.Equal(session1.SessionId, destroyedIds[0]);

            await session2.DisposeAsync();
            Assert.Equal(2, destroyedIds.Count);
            Assert.Equal(session2.SessionId, destroyedIds[1]);
        }
        finally
        {
            await client.ForceStopAsync();
        }
    }
}
