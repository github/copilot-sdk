/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Reflection;
using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

/// <summary>
/// Custom fixture for multi-client commands/elicitation tests.
/// Uses TCP mode so a second (and third) client can connect to the same CLI process.
/// </summary>
public class MultiClientCommandsElicitationFixture : IAsyncLifetime
{
    public E2ETestContext Ctx { get; private set; } = null!;
    public CopilotClient Client1 { get; private set; } = null!;

    public async Task InitializeAsync()
    {
        Ctx = await E2ETestContext.CreateAsync();
        Client1 = Ctx.CreateClient(useStdio: false);
    }

    public async Task DisposeAsync()
    {
        if (Client1 is not null)
        {
            await Client1.ForceStopAsync();
        }

        await Ctx.DisposeAsync();
    }
}

public class MultiClientCommandsElicitationTests
    : IClassFixture<MultiClientCommandsElicitationFixture>, IAsyncLifetime
{
    private readonly MultiClientCommandsElicitationFixture _fixture;
    private readonly string _testName;
    private CopilotClient? _client2;
    private CopilotClient? _client3;

    private E2ETestContext Ctx => _fixture.Ctx;
    private CopilotClient Client1 => _fixture.Client1;

    public MultiClientCommandsElicitationTests(
        MultiClientCommandsElicitationFixture fixture,
        ITestOutputHelper output)
    {
        _fixture = fixture;
        _testName = GetTestName(output);
    }

    private static string GetTestName(ITestOutputHelper output)
    {
        var type = output.GetType();
        var testField = type.GetField("test", BindingFlags.Instance | BindingFlags.NonPublic);
        var test = (ITest?)testField?.GetValue(output);
        return test?.TestCase.TestMethod.Method.Name
            ?? throw new InvalidOperationException("Couldn't find test name");
    }

    public async Task InitializeAsync()
    {
        await Ctx.ConfigureForTestAsync("multi_client", _testName);

        // Trigger connection so we can read the port
        var initSession = await Client1.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });
        await initSession.DisposeAsync();

        var port = Client1.ActualPort
            ?? throw new InvalidOperationException("Client1 is not using TCP mode; ActualPort is null");

        _client2 = new CopilotClient(new CopilotClientOptions
        {
            CliUrl = $"localhost:{port}",
        });
    }

    public async Task DisposeAsync()
    {
        if (_client3 is not null)
        {
            await _client3.ForceStopAsync();
            _client3 = null;
        }

        if (_client2 is not null)
        {
            await _client2.ForceStopAsync();
            _client2 = null;
        }
    }

    private CopilotClient Client2 => _client2
        ?? throw new InvalidOperationException("Client2 not initialized");

    [Fact]
    public async Task Client_Receives_Commands_Changed_When_Another_Client_Joins_With_Commands()
    {
        var session1 = await Client1.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        // Wait for the commands.changed event deterministically
        var commandsChangedTcs = new TaskCompletionSource<CommandsChangedEvent>(
            TaskCreationOptions.RunContinuationsAsynchronously);

        using var sub = session1.On(evt =>
        {
            if (evt is CommandsChangedEvent changed)
            {
                commandsChangedTcs.TrySetResult(changed);
            }
        });

        // Client2 joins with commands
        var session2 = await Client2.ResumeSessionAsync(session1.SessionId, new ResumeSessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            Commands =
            [
                new CommandDefinition
                {
                    Name = "deploy",
                    Description = "Deploy the app",
                    Handler = _ => Task.CompletedTask,
                },
            ],
            DisableResume = true,
        });

        var commandsChanged = await commandsChangedTcs.Task.WaitAsync(TimeSpan.FromSeconds(15));

        Assert.NotNull(commandsChanged.Data.Commands);
        Assert.Contains(commandsChanged.Data.Commands, c =>
            c.Name == "deploy" && c.Description == "Deploy the app");

        await session2.DisposeAsync();
    }

    [Fact]
    public async Task Capabilities_Changed_Fires_When_Second_Client_Joins_With_Elicitation_Handler()
    {
        // Client1 creates session without elicitation
        var session1 = await Client1.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });
        Assert.True(session1.Capabilities.Ui?.Elicitation != true,
            "Session without elicitation handler should not have elicitation capability");

        // Listen for capabilities.changed event
        var capChangedTcs = new TaskCompletionSource<CapabilitiesChangedEvent>(
            TaskCreationOptions.RunContinuationsAsynchronously);

        using var sub = session1.On(evt =>
        {
            if (evt is CapabilitiesChangedEvent capEvt)
            {
                capChangedTcs.TrySetResult(capEvt);
            }
        });

        // Client2 joins WITH elicitation handler — triggers capabilities.changed
        var session2 = await Client2.ResumeSessionAsync(session1.SessionId, new ResumeSessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            OnElicitationRequest = _ => Task.FromResult(new ElicitationResult
            {
                Action = Rpc.SessionUiElicitationResultAction.Accept,
                Content = new Dictionary<string, object>(),
            }),
            DisableResume = true,
        });

        var capEvent = await capChangedTcs.Task.WaitAsync(TimeSpan.FromSeconds(15));

        Assert.NotNull(capEvent.Data.Ui);
        Assert.True(capEvent.Data.Ui!.Elicitation);

        // Client1's capabilities should have been auto-updated
        Assert.True(session1.Capabilities.Ui?.Elicitation == true);

        await session2.DisposeAsync();
    }

    [Fact]
    public async Task Capabilities_Changed_Fires_When_Elicitation_Provider_Disconnects()
    {
        // Client1 creates session without elicitation
        var session1 = await Client1.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });
        Assert.True(session1.Capabilities.Ui?.Elicitation != true,
            "Session without elicitation handler should not have elicitation capability");

        // Wait for elicitation to become available
        var capEnabledTcs = new TaskCompletionSource<bool>(
            TaskCreationOptions.RunContinuationsAsynchronously);

        using var subEnabled = session1.On(evt =>
        {
            if (evt is CapabilitiesChangedEvent { Data.Ui.Elicitation: true })
            {
                capEnabledTcs.TrySetResult(true);
            }
        });

        // Use a dedicated client (client3) so we can stop it without affecting client2
        var port = Client1.ActualPort
            ?? throw new InvalidOperationException("Client1 ActualPort is null");
        _client3 = new CopilotClient(new CopilotClientOptions
        {
            CliUrl = $"localhost:{port}",
        });

        // Client3 joins WITH elicitation handler
        await _client3.ResumeSessionAsync(session1.SessionId, new ResumeSessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            OnElicitationRequest = _ => Task.FromResult(new ElicitationResult
            {
                Action = Rpc.SessionUiElicitationResultAction.Accept,
                Content = new Dictionary<string, object>(),
            }),
            DisableResume = true,
        });

        await capEnabledTcs.Task.WaitAsync(TimeSpan.FromSeconds(15));
        Assert.True(session1.Capabilities.Ui?.Elicitation == true);

        // Now listen for the capability being removed
        var capDisabledTcs = new TaskCompletionSource<bool>(
            TaskCreationOptions.RunContinuationsAsynchronously);

        using var subDisabled = session1.On(evt =>
        {
            if (evt is CapabilitiesChangedEvent { Data.Ui.Elicitation: false })
            {
                capDisabledTcs.TrySetResult(true);
            }
        });

        // Force-stop client3 — destroys the socket, triggering server-side cleanup
        await _client3.ForceStopAsync();
        _client3 = null;

        await capDisabledTcs.Task.WaitAsync(TimeSpan.FromSeconds(15));
        Assert.True(session1.Capabilities.Ui?.Elicitation != true,
            "After elicitation provider disconnects, capability should be removed");
    }
}

