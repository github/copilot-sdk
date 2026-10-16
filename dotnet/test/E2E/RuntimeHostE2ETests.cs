/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System.Collections.Concurrent;
using System.ComponentModel;
using System.Text.Json;
using System.Text.Json.Nodes;
using GitHub.Copilot.Test.Harness;
using Microsoft.Extensions.AI;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public sealed class RuntimeHostE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "runtime_host", output, replayOnly: true)
{
    private const string ToolPrompt = "Use the magic_number tool with seed 'hello' and tell me the result";
    private const string ComposedPrompt = "Call magic_number with seed 'hello' and client_echo with text 'ping', then report both results";
    private const string Marker = "APPLICATION_OWNED_AHP_PROMPT";

    private sealed class RuntimeHostFactAttribute : FactAttribute
    {
        public RuntimeHostFactAttribute()
        {
            if (Environment.GetEnvironmentVariable("COPILOT_RUNTIME_HOST_E2E") != "1")
                Skip = "Requires an integrated runtime; set COPILOT_RUNTIME_HOST_E2E=1";
        }
    }

    private sealed class RuntimeHostTheoryAttribute : TheoryAttribute
    {
        public RuntimeHostTheoryAttribute()
        {
            if (Environment.GetEnvironmentVariable("COPILOT_RUNTIME_HOST_E2E") != "1")
                Skip = "Requires an integrated runtime; set COPILOT_RUNTIME_HOST_E2E=1";
        }
    }

    private sealed class Application(E2ETestContext context, CopilotClient client)
    {
        public CopilotSession? Session;
        public int Creates;
        public int Resumes;
        public string? CreateSessionId;
        public string? ResumeSessionId;
        public int ToolCalls;
        public readonly ConcurrentQueue<string> HookSessions = new();
        public readonly ConcurrentQueue<CopilotSession> Releases = new();
        public readonly TaskCompletionSource Released = new(TaskCreationOptions.RunContinuationsAsynchronously);
        public readonly TaskCompletionSource Exited = new(TaskCreationOptions.RunContinuationsAsynchronously);
        public int ExitCount;

        public void Configure(SessionConfigBase config)
        {
            config.OnPermissionRequest = PermissionHandler.ApproveAll;
            config.SystemMessage = new SystemMessageConfig { Mode = SystemMessageMode.Append, Content = Marker };
            config.Tools = [AIFunctionFactory.Create(MagicNumber, "magic_number")];
            config.Hooks = new SessionHooks
            {
                OnPreToolUse = (_, invocation) =>
                {
                    HookSessions.Enqueue(invocation.SessionId);
                    return Task.FromResult<PreToolUseHookOutput?>(null);
                }
            };
        }

        [Description("Returns a magic number")]
        private string MagicNumber([Description("A seed value")] string seed)
        {
            Assert.Equal("hello", seed);
            Interlocked.Increment(ref ToolCalls);
            return "MAGIC_hello_42";
        }

        public AhpHostOptions Options() => new()
        {
            LocalServer = new(),
            CreateSession = async request =>
            {
                Interlocked.Increment(ref Creates);
                Assert.False(request.CancellationToken.IsCancellationRequested);
                Assert.Equal(context.WorkDir, request.Config.WorkingDirectory);
                CreateSessionId = request.Config.SessionId;
                Assert.False(string.IsNullOrEmpty(CreateSessionId));
                Configure(request.Config);
                Session = await context.CreateSessionAsync(client, request.Config);
                Assert.Equal(CreateSessionId, Session.SessionId);
                return Session;
            },
            ResumeSession = async request =>
            {
                Interlocked.Increment(ref Resumes);
                Assert.False(request.CancellationToken.IsCancellationRequested);
                Assert.False(request.Config.ContinuePendingWork);
                Assert.Equal(context.WorkDir, request.Config.WorkingDirectory);
                ResumeSessionId = request.SessionId;
                Configure(request.Config);
                Session = await context.ResumeSessionAsync(client, request.SessionId, request.Config);
                Assert.Equal(ResumeSessionId, Session.SessionId);
                return Session;
            },
            OnSessionReleased = session =>
            {
                Releases.Enqueue(session);
                Released.TrySetResult();
                return Task.CompletedTask;
            },
            OnExit = _ =>
            {
                Interlocked.Increment(ref ExitCount);
                Exited.TrySetResult();
                return Task.CompletedTask;
            }
        };
    }

    private static async Task<string> ConnectAsync(AhpTestClient ahp, AhpHost host, string? clientId = null)
    {
        Assert.NotNull(host.Url);
        var command = new JsonObject
        {
            ["op"] = "connect",
            ["url"] = host.Url,
            ["githubToken"] = "fake-token-for-e2e-tests"
        };
        if (host.Token is not null) command["token"] = host.Token;
        if (clientId is not null) command["clientId"] = clientId;
        return (await ahp.RequestAsync(command)).GetProperty("clientId").GetString()!;
    }

    private async Task AssertApplicationAsync(Application app, string sessionId)
    {
        Assert.Equal(1, app.ToolCalls);
        Assert.Contains(sessionId, app.HookSessions);
        var exchanges = await Ctx.GetExchangesAsync();
        Assert.Contains(exchanges.SelectMany(exchange => exchange.Request.Messages),
            message => message.Content?.GetRawText().Contains(Marker) == true);
        Assert.Contains(exchanges.SelectMany(exchange => exchange.Request.Tools ?? []),
            tool => tool.Function.Name == "magic_number");
    }

    [RuntimeHostTheory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task Creates_Or_Publishes_Exact_Application_Session(bool publish)
    {
        await Ctx.ConfigureForTestAsync("multi_client", "both_clients_see_tool_request_and_completion_events", replayOnly: true);
        var owner = Ctx.CreateClient();
        var app = new Application(Ctx, owner);
        await using var ahp = await AhpTestClient.StartAsync();
        if (publish)
        {
            var config = new SessionConfig { WorkingDirectory = Ctx.WorkDir };
            app.Configure(config);
            app.Session = await Ctx.CreateSessionAsync(owner, config);
        }
        await using var host = await owner.StartAhpHostAsync(app.Options());
        Assert.Null(host.Pid);
        var clientId = await ConnectAsync(ahp, host);
        string sessionId;
        if (publish)
        {
            sessionId = app.Session!.SessionId;
            var published = await host.PublishSessionAsync(sessionId);
            Assert.Equal(sessionId, published.SessionId);
            Assert.Equal($"ahp-session:/{sessionId}", published.SessionUri);
            await ahp.RequestAsync(new() { ["op"] = "attach", ["clientId"] = clientId, ["sessionId"] = sessionId });
        }
        else
        {
            var created = await ahp.RequestAsync(new() { ["op"] = "create", ["clientId"] = clientId, ["workDir"] = Ctx.WorkDir });
            sessionId = created.GetProperty("sessionId").GetString()!;
            Assert.Equal(1, app.Creates);
            Assert.Equal(app.CreateSessionId, app.Session!.SessionId);
            Assert.NotEqual(sessionId, app.Session.SessionId);
        }
        var response = await ahp.RequestAsync(new()
        {
            ["op"] = "turn",
            ["clientId"] = clientId,
            ["sessionId"] = sessionId,
            ["prompt"] = ToolPrompt
        });
        Assert.Contains("MAGIC_hello_42", response.GetProperty("text").GetString());
        await AssertApplicationAsync(app, app.Session!.SessionId);
        await Task.WhenAll(host.DisposeAsync().AsTask(), host.DisposeAsync().AsTask());
        await ahp.RequestAsync(new() { ["op"] = "stopped", ["clientId"] = clientId, ["url"] = host.Url });
        if (publish)
        {
            Assert.Equal(0, app.Creates);
            Assert.Equal(0, app.Resumes);
            Assert.Empty(app.Releases);
        }
        else
        {
            await app.Released.Task.WaitAsync(TimeSpan.FromSeconds(10));
            Assert.Same(app.Session, Assert.Single(app.Releases));
        }
        await app.Exited.Task.WaitAsync(TimeSpan.FromSeconds(10));
        Assert.Equal(1, app.ExitCount);
        Assert.NotEmpty(await app.Session!.GetEventsAsync());
        Assert.Equal("pong: still alive", (await owner.PingAsync("still alive")).Message);
    }

    [RuntimeHostFact]
    public async Task Resumes_After_Runtime_Restart_And_Composes_Tools()
    {
        await Ctx.ConfigureForTestAsync("runtime_host", "app_resume_callback_composes_tools_after_history", replayOnly: true);
        var firstOwner = Ctx.CreateClient();
        var first = new Application(Ctx, firstOwner);
        await using var ahp = await AhpTestClient.StartAsync();
        string clientId, sessionId;
        await using (var host = await firstOwner.StartAhpHostAsync(first.Options()))
        {
            clientId = await ConnectAsync(ahp, host);
            var created = await ahp.RequestAsync(new()
            {
                ["op"] = "create",
                ["clientId"] = clientId,
                ["workDir"] = Ctx.WorkDir,
                ["clientTools"] = true
            });
            sessionId = created.GetProperty("sessionId").GetString()!;
            Assert.Equal(1, first.Creates);
            Assert.Equal(first.CreateSessionId, first.Session!.SessionId);
            Assert.NotEqual(sessionId, first.Session.SessionId);
            var answer = await ahp.RequestAsync(new()
            {
                ["op"] = "turn",
                ["clientId"] = clientId,
                ["sessionId"] = sessionId,
                ["prompt"] = "What is 2+2?"
            });
            Assert.Contains("4", answer.GetProperty("text").GetString());
            await host.DisposeAsync();
            await ahp.RequestAsync(new() { ["op"] = "stopped", ["clientId"] = clientId, ["url"] = host.Url });
            await first.Released.Task.WaitAsync(TimeSpan.FromSeconds(10));
            Assert.Same(first.Session, Assert.Single(first.Releases));
        }
        await ahp.RequestAsync(new() { ["op"] = "close", ["clientId"] = clientId });
        await firstOwner.StopAsync();

        var resumedOwner = Ctx.CreateClient();
        var resumed = new Application(Ctx, resumedOwner);
        await using var replacement = await resumedOwner.StartAhpHostAsync(resumed.Options());
        Assert.Null(replacement.Pid);
        await ConnectAsync(ahp, replacement, clientId);
        var attached = await ahp.RequestAsync(new()
        {
            ["op"] = "attach",
            ["clientId"] = clientId,
            ["sessionId"] = sessionId,
            ["clientTools"] = true
        });
        Assert.Equal(["What is 2+2?"], attached.GetProperty("history").EnumerateArray()
            .Select(turn => turn.GetProperty("message").GetProperty("text").GetString()));
        Assert.Equal(0, resumed.Creates);
        Assert.Equal(1, resumed.Resumes);
        Assert.Equal(first.Session!.SessionId, resumed.ResumeSessionId);
        Assert.Equal(first.Session.SessionId, resumed.Session!.SessionId);
        Assert.NotSame(first.Session, resumed.Session);
        var response = await ahp.RequestAsync(new()
        {
            ["op"] = "turn",
            ["clientId"] = clientId,
            ["sessionId"] = sessionId,
            ["prompt"] = ComposedPrompt,
            ["clientTools"] = true
        });
        Assert.Contains("MAGIC_hello_42", response.GetProperty("text").GetString());
        Assert.Contains("CLIENT_ECHO_ping", response.GetProperty("text").GetString());
        Assert.Equal(1, response.GetProperty("clientToolCalls").GetInt32());
        await AssertApplicationAsync(resumed, first.Session.SessionId);
        await replacement.DisposeAsync();
        await ahp.RequestAsync(new() { ["op"] = "stopped", ["clientId"] = clientId, ["url"] = replacement.Url });
        await resumed.Released.Task.WaitAsync(TimeSpan.FromSeconds(10));
        Assert.Same(resumed.Session, Assert.Single(resumed.Releases));
        Assert.NotEmpty(await resumed.Session.GetEventsAsync());
    }
}
#endif
