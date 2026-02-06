/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;

namespace GitHub.Copilot.SDK.Test;

// These tests bypass E2ETestBase because they are about how the CLI subprocess is started
// Other test classes should instead inherit from E2ETestBase
public class ClientTests
{
    [Fact]
    public async Task Should_Start_And_Connect_To_Server_Using_Stdio()
    {
        using var client = new CopilotClient(new CopilotClientOptions { UseStdio = true });

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
        using var client = new CopilotClient(new CopilotClientOptions { UseStdio = false });

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
        using var client = new CopilotClient(new CopilotClientOptions());

        await client.CreateSessionAsync();
        await client.ForceStopAsync();

        Assert.Equal(ConnectionState.Disconnected, client.State);
    }

    [Fact]
    public async Task Should_Get_Status_With_Version_And_Protocol_Info()
    {
        using var client = new CopilotClient(new CopilotClientOptions { UseStdio = true });

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
        using var client = new CopilotClient(new CopilotClientOptions { UseStdio = true });

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
        using var client = new CopilotClient(new CopilotClientOptions { UseStdio = true });

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
    public void Should_Accept_GithubToken_Option()
    {
        var options = new CopilotClientOptions
        {
            GithubToken = "gho_test_token"
        };

        Assert.Equal("gho_test_token", options.GithubToken);
    }

    [Fact]
    public void Should_Default_UseLoggedInUser_To_Null()
    {
        var options = new CopilotClientOptions();

        Assert.Null(options.UseLoggedInUser);
    }

    [Fact]
    public void Should_Allow_Explicit_UseLoggedInUser_False()
    {
        var options = new CopilotClientOptions
        {
            UseLoggedInUser = false
        };

        Assert.False(options.UseLoggedInUser);
    }

    [Fact]
    public void Should_Allow_Explicit_UseLoggedInUser_True_With_GithubToken()
    {
        var options = new CopilotClientOptions
        {
            GithubToken = "gho_test_token",
            UseLoggedInUser = true
        };

        Assert.True(options.UseLoggedInUser);
    }

    [Fact]
    public void Should_Throw_When_GithubToken_Used_With_CliUrl()
    {
        Assert.Throws<ArgumentException>(() =>
        {
            _ = new CopilotClient(new CopilotClientOptions
            {
                CliUrl = "localhost:8080",
                GithubToken = "gho_test_token"
            });
        });
    }

    [Fact]
    public void Should_Throw_When_UseLoggedInUser_Used_With_CliUrl()
    {
        Assert.Throws<ArgumentException>(() =>
        {
            _ = new CopilotClient(new CopilotClientOptions
            {
                CliUrl = "localhost:8080",
                UseLoggedInUser = false
            });
        });
    }
}
