/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Collections.Concurrent;
using System.ComponentModel;
using System.Reflection;
using System.Text.RegularExpressions;
using GitHub.Copilot.SDK.Test.Harness;
using Microsoft.Extensions.AI;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

/// <summary>
/// Custom fixture for multi-client tests that uses TCP mode so a second client can connect.
/// </summary>
public class MultiClientTestFixture : IAsyncLifetime
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

public class MultiClientTests : IClassFixture<MultiClientTestFixture>, IAsyncLifetime
{
    private readonly MultiClientTestFixture _fixture;
    private readonly string _testName;
    private CopilotClient? _client2;

    private E2ETestContext Ctx => _fixture.Ctx;
    private CopilotClient Client1 => _fixture.Client1;

    public MultiClientTests(MultiClientTestFixture fixture, ITestOutputHelper output)
    {
        _fixture = fixture;
        _testName = GetTestName(output);
    }

    private static string GetTestName(ITestOutputHelper output)
    {
        var type = output.GetType();
        var testField = type.GetField("test", BindingFlags.Instance | BindingFlags.NonPublic);
        var test = (ITest?)testField?.GetValue(output);
        return test?.TestCase.TestMethod.Method.Name ?? throw new InvalidOperationException("Couldn't find test name");
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
        if (_client2 is not null)
        {
            await _client2.ForceStopAsync();
            _client2 = null;
        }
    }

    private CopilotClient Client2 => _client2 ?? throw new InvalidOperationException("Client2 not initialized");

    [Fact]
    public async Task Both_Clients_See_Tool_Request_And_Completion_Events()
    {
        var tool = AIFunctionFactory.Create(MagicNumber, "magic_number");

        var session1 = await Client1.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            Tools = [tool],
        });

        var session2 = await Client2.ResumeSessionAsync(session1.SessionId, new ResumeSessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        // Set up event waiters BEFORE sending the prompt to avoid race conditions
        var client1Requested = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
        var client2Requested = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
        var client1Completed = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
        var client2Completed = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);

        using var sub1 = session1.On(evt =>
        {
            if (evt is ExternalToolRequestedEvent) client1Requested.TrySetResult(true);
            if (evt is ExternalToolCompletedEvent) client1Completed.TrySetResult(true);
        });
        using var sub2 = session2.On(evt =>
        {
            if (evt is ExternalToolRequestedEvent) client2Requested.TrySetResult(true);
            if (evt is ExternalToolCompletedEvent) client2Completed.TrySetResult(true);
        });

        var response = await session1.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Use the magic_number tool with seed 'hello' and tell me the result",
        });

        Assert.NotNull(response);
        Assert.Contains("MAGIC_hello_42", response!.Data.Content ?? string.Empty);

        // Wait for all broadcast events to arrive on both clients
        await Task.WhenAll(
            client1Requested.Task, client2Requested.Task,
            client1Completed.Task, client2Completed.Task).WaitAsync(TimeSpan.FromSeconds(10));

        await session2.DisposeAsync();

        [Description("Returns a magic number")]
        static string MagicNumber([Description("A seed value")] string seed) => $"MAGIC_{seed}_42";
    }

    [Fact]
    public async Task One_Client_Approves_Permission_And_Both_See_The_Result()
    {
        var client1PermissionRequests = new List<PermissionRequest>();

        var session1 = await Client1.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = (request, _) =>
            {
                client1PermissionRequests.Add(request);
                return Task.FromResult(new PermissionRequestResult
                {
                    Kind = PermissionRequestResultKind.Approved,
                });
            },
        });

        // Client 2 resumes — its handler never completes, so only client 1's approval takes effect
        var session2 = await Client2.ResumeSessionAsync(session1.SessionId, new ResumeSessionConfig
        {
            OnPermissionRequest = (_, _) => new TaskCompletionSource<PermissionRequestResult>().Task,
        });

        var client1Events = new ConcurrentBag<SessionEvent>();
        var client2Events = new ConcurrentBag<SessionEvent>();

        using var sub1 = session1.On(evt => client1Events.Add(evt));
        using var sub2 = session2.On(evt => client2Events.Add(evt));

        var response = await session1.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Create a file called hello.txt containing the text 'hello world'",
        });

        Assert.NotNull(response);
        Assert.NotEmpty(client1PermissionRequests);

        Assert.Contains(client1Events, e => e is PermissionRequestedEvent);
        Assert.Contains(client2Events, e => e is PermissionRequestedEvent);
        Assert.Contains(client1Events, e => e is PermissionCompletedEvent);
        Assert.Contains(client2Events, e => e is PermissionCompletedEvent);

        foreach (var evt in client1Events.OfType<PermissionCompletedEvent>()
            .Concat(client2Events.OfType<PermissionCompletedEvent>()))
        {
            Assert.Equal(PermissionCompletedDataResultKind.Approved, evt.Data.Result.Kind);
        }

        await session2.DisposeAsync();
    }

    [Fact]
    public async Task One_Client_Rejects_Permission_And_Both_See_The_Result()
    {
        var session1 = await Client1.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = (_, _) => Task.FromResult(new PermissionRequestResult
            {
                Kind = PermissionRequestResultKind.DeniedInteractivelyByUser,
            }),
        });

        // Client 2 resumes — its handler never completes
        var session2 = await Client2.ResumeSessionAsync(session1.SessionId, new ResumeSessionConfig
        {
            OnPermissionRequest = (_, _) => new TaskCompletionSource<PermissionRequestResult>().Task,
        });

        var client1Events = new ConcurrentBag<SessionEvent>();
        var client2Events = new ConcurrentBag<SessionEvent>();

        using var sub1 = session1.On(evt => client1Events.Add(evt));
        using var sub2 = session2.On(evt => client2Events.Add(evt));

        // Write a file so the agent has something to edit
        await File.WriteAllTextAsync(Path.Combine(Ctx.WorkDir, "protected.txt"), "protected content");

        await session1.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Edit protected.txt and replace 'protected' with 'hacked'.",
        });

        // Verify the file was NOT modified
        var content = await File.ReadAllTextAsync(Path.Combine(Ctx.WorkDir, "protected.txt"));
        Assert.Equal("protected content", content);

        Assert.Contains(client1Events, e => e is PermissionRequestedEvent);
        Assert.Contains(client2Events, e => e is PermissionRequestedEvent);

        foreach (var evt in client1Events.OfType<PermissionCompletedEvent>()
            .Concat(client2Events.OfType<PermissionCompletedEvent>()))
        {
            Assert.Equal(PermissionCompletedDataResultKind.DeniedInteractivelyByUser, evt.Data.Result.Kind);
        }

        await session2.DisposeAsync();
    }

    [Fact]
    public async Task Two_Clients_Register_Different_Tools_And_Agent_Uses_Both()
    {
        var toolA = AIFunctionFactory.Create(CityLookup, "city_lookup");
        var toolB = AIFunctionFactory.Create(CurrencyLookup, "currency_lookup");

        var session1 = await Client1.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            Tools = [toolA],
        });

        var session2 = await Client2.ResumeSessionAsync(session1.SessionId, new ResumeSessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            Tools = [toolB],
        });

        // Send prompts sequentially to avoid nondeterministic tool_call ordering
        var response1 = await session1.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Use the city_lookup tool with countryCode 'US' and tell me the result.",
        });
        Assert.NotNull(response1);
        Assert.Contains("CITY_FOR_US", response1!.Data.Content ?? string.Empty);

        var response2 = await session1.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Now use the currency_lookup tool with countryCode 'US' and tell me the result.",
        });
        Assert.NotNull(response2);
        Assert.Contains("CURRENCY_FOR_US", response2!.Data.Content ?? string.Empty);

        await session2.DisposeAsync();

        [Description("Returns a city name for a given country code")]
        static string CityLookup([Description("A two-letter country code")] string countryCode) => $"CITY_FOR_{countryCode}";

        [Description("Returns a currency for a given country code")]
        static string CurrencyLookup([Description("A two-letter country code")] string countryCode) => $"CURRENCY_FOR_{countryCode}";
    }

    [Fact]
    public async Task Disconnecting_Client_Removes_Its_Tools()
    {
        var toolA = AIFunctionFactory.Create(StableTool, "stable_tool");
        var toolB = AIFunctionFactory.Create(EphemeralTool, "ephemeral_tool");

        var session1 = await Client1.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            Tools = [toolA],
        });

        await Client2.ResumeSessionAsync(session1.SessionId, new ResumeSessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            Tools = [toolB],
        });

        // Verify both tools work before disconnect (sequential to avoid nondeterministic tool_call ordering)
        var stableResponse = await session1.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Use the stable_tool with input 'test1' and tell me the result.",
        });
        Assert.NotNull(stableResponse);
        Assert.Contains("STABLE_test1", stableResponse!.Data.Content ?? string.Empty);

        var ephemeralResponse = await session1.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Use the ephemeral_tool with input 'test2' and tell me the result.",
        });
        Assert.NotNull(ephemeralResponse);
        Assert.Contains("EPHEMERAL_test2", ephemeralResponse!.Data.Content ?? string.Empty);

        // Disconnect client 2
        await Client2.ForceStopAsync();
        await Task.Delay(500); // Let the server process the disconnection

        // Recreate client2 for cleanup
        var port = Client1.ActualPort!.Value;
        _client2 = new CopilotClient(new CopilotClientOptions
        {
            CliUrl = $"localhost:{port}",
        });

        // Now only stable_tool should be available
        var afterResponse = await session1.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Use the stable_tool with input 'still_here'. Also try using ephemeral_tool if it is available.",
        });
        Assert.NotNull(afterResponse);
        Assert.Contains("STABLE_still_here", afterResponse!.Data.Content ?? string.Empty);
        Assert.DoesNotContain("EPHEMERAL_", afterResponse!.Data.Content ?? string.Empty);

        [Description("A tool that persists across disconnects")]
        static string StableTool([Description("Input value")] string input) => $"STABLE_{input}";

        [Description("A tool that will disappear when its client disconnects")]
        static string EphemeralTool([Description("Input value")] string input) => $"EPHEMERAL_{input}";
    }
}
