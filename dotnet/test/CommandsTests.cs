/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public class CommandsTests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "commands", output)
{
    [Fact]
    public async Task Session_With_Commands_Creates_Successfully()
    {
        var session = await CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            Commands =
            [
                new CommandDefinition { Name = "deploy", Description = "Deploy the app", Handler = _ => Task.CompletedTask },
                new CommandDefinition { Name = "rollback", Handler = _ => Task.CompletedTask },
            ],
        });

        // Session should be created successfully with commands
        Assert.NotNull(session);
        Assert.NotNull(session.SessionId);
        await session.DisposeAsync();
    }

    [Fact]
    public async Task Session_With_Commands_Resumes_Successfully()
    {
        var session1 = await CreateSessionAsync();
        var sessionId = session1.SessionId;

        var session2 = await ResumeSessionAsync(sessionId, new ResumeSessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            Commands =
            [
                new CommandDefinition { Name = "deploy", Description = "Deploy", Handler = _ => Task.CompletedTask },
            ],
        });

        Assert.NotNull(session2);
        Assert.Equal(sessionId, session2.SessionId);
        await session2.DisposeAsync();
    }

    [Fact]
    public void CommandDefinition_Has_Required_Properties()
    {
        var cmd = new CommandDefinition
        {
            Name = "deploy",
            Description = "Deploy the app",
            Handler = _ => Task.CompletedTask,
        };

        Assert.Equal("deploy", cmd.Name);
        Assert.Equal("Deploy the app", cmd.Description);
        Assert.NotNull(cmd.Handler);
    }

    [Fact]
    public void CommandContext_Has_All_Properties()
    {
        var ctx = new CommandContext
        {
            SessionId = "session-1",
            Command = "/deploy production",
            CommandName = "deploy",
            Args = "production",
        };

        Assert.Equal("session-1", ctx.SessionId);
        Assert.Equal("/deploy production", ctx.Command);
        Assert.Equal("deploy", ctx.CommandName);
        Assert.Equal("production", ctx.Args);
    }

    [Fact]
    public async Task Session_With_No_Commands_Creates_Successfully()
    {
        var session = await CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        Assert.NotNull(session);
        await session.DisposeAsync();
    }

    [Fact]
    public async Task Session_Config_Commands_Are_Cloned()
    {
        var config = new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            Commands =
            [
                new CommandDefinition { Name = "deploy", Handler = _ => Task.CompletedTask },
            ],
        };

        var clone = config.Clone();

        Assert.NotNull(clone.Commands);
        Assert.Single(clone.Commands!);
        Assert.Equal("deploy", clone.Commands![0].Name);

        // Verify collections are independent
        clone.Commands!.Add(new CommandDefinition { Name = "rollback", Handler = _ => Task.CompletedTask });
        Assert.Single(config.Commands!);
    }

    [Fact]
    public void Resume_Config_Commands_Are_Cloned()
    {
        var config = new ResumeSessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            Commands =
            [
                new CommandDefinition { Name = "deploy", Handler = _ => Task.CompletedTask },
            ],
        };

        var clone = config.Clone();

        Assert.NotNull(clone.Commands);
        Assert.Single(clone.Commands!);
        Assert.Equal("deploy", clone.Commands![0].Name);
    }
}
