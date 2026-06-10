/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class CommandsE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "commands", output)
{
    private static readonly string[] KnownBuiltinCommands = ["help", "model", "compact"];

    [Fact]
    public async Task Session_Commands_List_Returns_Builtins_And_Respects_Client_Command_Filter()
    {
        var session = await CreateSessionAsync(new SessionConfig
        {
            Commands =
            [
                new CommandDefinition { Name = "deploy", Description = "Deploy the app", Handler = _ => Task.CompletedTask },
                new CommandDefinition { Name = "rollback", Description = "Rollback the app", Handler = _ => Task.CompletedTask },
            ],
        });

        CommandList? clientCommands = null;
        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                clientCommands = await session.Rpc.Commands.ListAsync(new CommandsListRequest
                {
                    IncludeBuiltins = false,
                    IncludeClientCommands = true,
                    IncludeSkills = false,
                });
                return clientCommands.Commands.Any(c => IsCommand(c, "deploy", SlashCommandKind.Client)) &&
                    clientCommands.Commands.Any(c => IsCommand(c, "rollback", SlashCommandKind.Client));
            },
            timeout: TimeSpan.FromSeconds(30),
            timeoutMessage: "Timed out waiting for client commands to be listed.");
        Assert.Contains(clientCommands!.Commands, c => IsCommand(c, "deploy", SlashCommandKind.Client));
        Assert.Contains(clientCommands.Commands, c => IsCommand(c, "rollback", SlashCommandKind.Client));
        Assert.DoesNotContain(clientCommands.Commands, c => c.Kind == SlashCommandKind.Builtin);

        var builtinCommands = await session.Rpc.Commands.ListAsync(new CommandsListRequest
        {
            IncludeBuiltins = true,
            IncludeClientCommands = false,
            IncludeSkills = false,
        });
        Assert.True(
            builtinCommands.Commands.Any(IsKnownBuiltin),
            $"Expected a known built-in command. Actual commands: {FormatCommands(builtinCommands.Commands)}");
        Assert.DoesNotContain(builtinCommands.Commands, c => string.Equals(c.Name, "deploy", StringComparison.OrdinalIgnoreCase));

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Session_Commands_Invoke_Known_Builtin_Returns_Expected_Result()
    {
        var session = await CreateSessionAsync();

        var builtinCommands = await session.Rpc.Commands.ListAsync(new CommandsListRequest
        {
            IncludeBuiltins = true,
            IncludeClientCommands = false,
            IncludeSkills = false,
        });
        var commandName = KnownBuiltinCommands.FirstOrDefault(name =>
            builtinCommands.Commands.Any(c => IsCommand(c, name, SlashCommandKind.Builtin)));
        Assert.NotNull(commandName);

        var result = await session.Rpc.Commands.InvokeAsync(commandName);

        switch (result)
        {
            case SlashCommandInvocationResultText text:
                Assert.False(string.IsNullOrWhiteSpace(text.Text));
                break;

            case SlashCommandInvocationResultSelectSubcommand select:
                Assert.False(string.IsNullOrWhiteSpace(select.Title));
                Assert.NotEmpty(select.Options);
                break;

            case SlashCommandInvocationResultAgentPrompt prompt:
                Assert.False(string.IsNullOrWhiteSpace(prompt.DisplayPrompt));
                Assert.False(string.IsNullOrWhiteSpace(prompt.Prompt));
                break;

            case SlashCommandInvocationResultCompleted completed:
                Assert.True(completed.Message is null || !string.IsNullOrWhiteSpace(completed.Message));
                break;

            default:
                Assert.Fail($"Unexpected invocation result: {result.GetType().Name}");
                break;
        }

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Session_Commands_Execute_Runs_Registered_Command_Handler()
    {
        CommandContext? capturedContext = null;
        var session = await CreateSessionAsync(new SessionConfig
        {
            Commands =
            [
                new CommandDefinition
                {
                    Name = "deploy",
                    Description = "Deploy the app",
                    Handler = ctx =>
                    {
                        capturedContext = ctx;
                        return Task.CompletedTask;
                    },
                },
            ],
        });

        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                var commands = await session.Rpc.Commands.ListAsync(new CommandsListRequest
                {
                    IncludeBuiltins = false,
                    IncludeClientCommands = true,
                    IncludeSkills = false,
                });
                return commands.Commands.Any(c => IsCommand(c, "deploy", SlashCommandKind.Client));
            },
            timeout: TimeSpan.FromSeconds(30),
            timeoutMessage: "Timed out waiting for registered command to be listed.");

        var result = await session.Rpc.Commands.ExecuteAsync("deploy", "production");

        Assert.Null(result.Error);
        await TestHelper.WaitForConditionAsync(
            () => capturedContext is not null,
            timeout: TimeSpan.FromSeconds(10),
            timeoutMessage: "Timed out waiting for command handler execution.");
        Assert.Equal(session.SessionId, capturedContext!.SessionId);
        Assert.Equal("/deploy production", capturedContext.Command);
        Assert.Equal("deploy", capturedContext.CommandName);
        Assert.Equal("production", capturedContext.Args);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Session_Commands_Enqueue_Accepts_Deterministic_Command()
    {
        var session = await CreateSessionAsync();

        var result = await session.Rpc.Commands.EnqueueAsync("/help");

        Assert.True(result.Queued);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Session_Commands_RespondToQueuedCommand_Returns_False_For_Unknown_RequestId()
    {
        var session = await CreateSessionAsync();

        var result = await session.Rpc.Commands.RespondToQueuedCommandAsync(
            "missing-queued-command-request",
            new QueuedCommandResult { Handled = false });

        Assert.False(result.Success);

        await session.DisposeAsync();
    }

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

    private static bool IsCommand(SlashCommandInfo command, string name, SlashCommandKind kind)
    {
        return string.Equals(command.Name, name, StringComparison.OrdinalIgnoreCase) && command.Kind == kind;
    }

    private static bool IsKnownBuiltin(SlashCommandInfo command)
    {
        return command.Kind == SlashCommandKind.Builtin &&
            KnownBuiltinCommands.Contains(command.Name, StringComparer.OrdinalIgnoreCase);
    }

    private static string FormatCommands(IEnumerable<SlashCommandInfo> commands)
    {
        return string.Join(", ", commands.Select(c => $"{c.Name}:{c.Kind.Value}"));
    }
}
