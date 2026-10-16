/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Logging;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    [Fact]
    public async Task SkillProvider_Create_And_Resume_Set_Flag_Only_When_Provided()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var provider = new TestSkillProvider
        {
            ReadHandler = (name, _) => Task.FromResult<string?>($"# {name}")
        };

        await using var created = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = "skill-create",
            SkillProvider = provider
        });
        await using var resumed = await client.ResumeSessionAsync("skill-resume", new ResumeSessionConfig
        {
            SkillProvider = provider
        });

        Assert.True(Assert.Single(server.Requests, request => request.Method == "session.create")
            .Params.GetProperty("hasSkillProvider").GetBoolean());
        Assert.True(Assert.Single(server.Requests, request => request.Method == "session.resume")
            .Params.GetProperty("hasSkillProvider").GetBoolean());

        var read = await server.SendRequestAsync("skillProvider.read", SkillReadRequest(resumed.SessionId, "resumed-skill"));
        Assert.Equal("# resumed-skill", read.GetProperty("markdown").GetString());

        await created.DisposeAsync();
        await resumed.DisposeAsync();
        server.ClearRequests();

        await using var createWithoutProvider = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = "skill-create-no-provider"
        });
        await using var resumeWithoutProvider = await client.ResumeSessionAsync("skill-resume-no-provider", new ResumeSessionConfig());

        Assert.False(Assert.Single(server.Requests, request => request.Method == "session.create")
            .Params.TryGetProperty("hasSkillProvider", out _));
        Assert.False(Assert.Single(server.Requests, request => request.Method == "session.resume")
            .Params.TryGetProperty("hasSkillProvider", out _));
    }

    [Fact]
    public async Task SkillProvider_Callbacks_Work_Before_Create_And_Resume_Response()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var provider = new TestSkillProvider();
        var callbackResults = new List<Task<JsonElement>>();
        server.BeforeResponseAsync = async (request, _) =>
        {
            if (request.Method is "session.create" or "session.resume"
                && request.Params.TryGetProperty("sessionId", out var sessionId))
            {
                callbackResults.Add(await server.SendRequestWithoutWaitingAsync(
                    "skillProvider.list",
                    SkillListRequest(sessionId.GetString()!)));
            }
        };

        await using var created = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = "pre-response-create",
            SkillProvider = provider
        });
        await using var resumed = await client.ResumeSessionAsync("pre-response-resume", new ResumeSessionConfig
        {
            SkillProvider = provider
        });

        Assert.Equal(2, callbackResults.Count);
        foreach (var callback in callbackResults)
        {
            var result = await callback.WaitAsync(TimeSpan.FromSeconds(5));
            Assert.Equal("dynamic", Assert.Single(result.GetProperty("skills").EnumerateArray()).GetProperty("name").GetString());
        }
    }

    [Fact]
    public async Task SkillProvider_List_Returns_CamelCase_Descriptors_And_Omits_Null_Optionals()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var provider = new TestSkillProvider
        {
            ListHandler = _ => Task.FromResult<IReadOnlyList<SkillProviderDescriptor>>(
            [
                new()
                {
                    Name = "basic",
                    Description = "Basic skill"
                },
                new()
                {
                    Name = "advanced",
                    Description = "Advanced skill",
                    UserInvocable = false,
                    DisableModelInvocation = true,
                    ArgumentHint = "<topic>"
                }
            ])
        };
        await using var session = await client.CreateSessionAsync(new SessionConfig { SkillProvider = provider });

        var result = await server.SendRequestAsync("skillProvider.list", SkillListRequest(session.SessionId));
        var skills = result.GetProperty("skills").EnumerateArray().ToArray();

        Assert.Equal("basic", skills[0].GetProperty("name").GetString());
        Assert.Equal("Basic skill", skills[0].GetProperty("description").GetString());
        Assert.False(skills[0].TryGetProperty("userInvocable", out _));
        Assert.False(skills[0].TryGetProperty("disableModelInvocation", out _));
        Assert.False(skills[0].TryGetProperty("argumentHint", out _));
        Assert.False(skills[0].TryGetProperty("UserInvocable", out _));
        Assert.False(skills[0].TryGetProperty("DisableModelInvocation", out _));
        Assert.False(skills[0].TryGetProperty("ArgumentHint", out _));
        Assert.Equal("advanced", skills[1].GetProperty("name").GetString());
        Assert.False(skills[1].GetProperty("userInvocable").GetBoolean());
        Assert.True(skills[1].GetProperty("disableModelInvocation").GetBoolean());
        Assert.Equal("<topic>", skills[1].GetProperty("argumentHint").GetString());
    }

    [Fact]
    public async Task SkillProvider_Null_List_Result_Is_Empty()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var provider = new TestSkillProvider
        {
            ListHandler = _ => Task.FromResult<IReadOnlyList<SkillProviderDescriptor>>(null!)
        };
        await using var session = await client.CreateSessionAsync(new SessionConfig { SkillProvider = provider });

        var result = await server.SendRequestAsync("skillProvider.list", SkillListRequest(session.SessionId));

        Assert.Empty(result.GetProperty("skills").EnumerateArray());
    }

    [Fact]
    public async Task SkillProvider_Read_Returns_Markdown_And_Null_For_Missing_Skill()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var provider = new TestSkillProvider
        {
            ReadHandler = (name, _) => Task.FromResult(name == "known" ? "# Known" : null)
        };
        await using var session = await client.CreateSessionAsync(new SessionConfig { SkillProvider = provider });

        var read = await server.SendRequestAsync("skillProvider.read", SkillReadRequest(session.SessionId, "known"));

        Assert.Equal("# Known", read.GetProperty("markdown").GetString());
        var missing = await server.SendRequestAsync("skillProvider.read", SkillReadRequest(session.SessionId, "missing"));
        Assert.True(missing.TryGetProperty("markdown", out var markdown), missing.ToString());
        Assert.Equal(JsonValueKind.Null, markdown.ValueKind);
    }

    [Fact]
    public async Task SkillProvider_Provider_Failures_Do_Not_Leak_Exception_Details()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        var logger = new SkillProviderLogger();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url), Logger = logger });
        var listFailure = new InvalidOperationException("secret list failure");
        var readFailure = new InvalidOperationException("secret read failure");
        var provider = new TestSkillProvider
        {
            ListHandler = _ => throw listFailure,
            ReadHandler = (_, _) => throw readFailure
        };
        await using var session = await client.CreateSessionAsync(new SessionConfig { SkillProvider = provider });

        var listError = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync("skillProvider.list", SkillListRequest(session.SessionId)));
        AssertRpcError(listError, "Skill provider listSkills failed");

        var readError = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync("skillProvider.read", SkillReadRequest(session.SessionId, "throws")));
        AssertRpcError(readError, "Skill provider readSkill failed");

        Assert.DoesNotContain("secret", listError.Message, StringComparison.OrdinalIgnoreCase);
        Assert.DoesNotContain("secret", readError.Message, StringComparison.OrdinalIgnoreCase);
        Assert.Contains(logger.Entries, entry =>
            entry.Message == $"Skill provider listSkills failed. SessionId={session.SessionId}" && entry.Exception == listFailure);
        Assert.Contains(logger.Entries, entry =>
            entry.Message == $"Skill provider readSkill failed. SessionId={session.SessionId}" && entry.Exception == readFailure);
    }

    [Fact]
    public async Task SkillProvider_Provider_Cancellation_Without_Request_Cancellation_Is_Provider_Failure()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var provider = new TestSkillProvider
        {
            ListHandler = _ => throw new TaskCanceledException("secret list timeout"),
            ReadHandler = (_, _) => throw new TaskCanceledException("secret read timeout")
        };
        await using var session = await client.CreateSessionAsync(new SessionConfig { SkillProvider = provider });

        var listError = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync("skillProvider.list", SkillListRequest(session.SessionId)).WaitAsync(TimeSpan.FromSeconds(10)));
        AssertRpcError(listError, "Skill provider listSkills failed");

        var readError = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync("skillProvider.read", SkillReadRequest(session.SessionId, "times-out")).WaitAsync(TimeSpan.FromSeconds(10)));
        AssertRpcError(readError, "Skill provider readSkill failed");
    }

    [Fact]
    public async Task SkillProvider_Rejects_Unknown_NoProvider_And_Disposed_Sessions()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await client.StartAsync();

        var unknown = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync("skillProvider.list", SkillListRequest("unknown-session")));
        AssertRpcError(unknown, "No skill provider for session: unknown-session");

        await using var noProvider = await client.CreateSessionAsync(new SessionConfig { SessionId = "no-provider" });
        var noProviderError = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync("skillProvider.list", SkillListRequest(noProvider.SessionId)));
        AssertRpcError(noProviderError, "No skill provider for session: no-provider");

        var providerSession = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = "disposed-provider",
            SkillProvider = new TestSkillProvider()
        });
        await providerSession.DisposeAsync();
        var disposed = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync("skillProvider.list", SkillListRequest("disposed-provider")));
        AssertRpcError(disposed, "No skill provider for session: disposed-provider");
    }

    [Fact]
    public async Task SkillProvider_Cloud_Create_Throws_Before_Connecting()
    {
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri("http://127.0.0.1:1")
        });
        var provider = new TestSkillProvider();

        var error = await Assert.ThrowsAsync<ArgumentException>(() => client.CreateSessionAsync(new SessionConfig
        {
            Cloud = new CloudSessionOptions
            {
                Repository = new CloudSessionRepository
                {
                    Owner = "github",
                    Name = "copilot-sdk",
                    Branch = "main"
                }
            },
            SkillProvider = provider
        }));

        Assert.Equal("Skill providers are not supported for cloud sessions.", error.Message);
        Assert.Equal(0, provider.ListCalls);
        Assert.Equal(0, provider.ReadCalls);
    }

    [Fact]
    public async Task SkillProvider_EmptyMode_Defaults_EnableSkills_To_False()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url),
            Mode = CopilotClientMode.Empty,
            BaseDirectory = Path.GetTempPath(),
        });

        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            AvailableTools = [],
            SkillProvider = new TestSkillProvider()
        });

        var request = Assert.Single(server.Requests, request => request.Method == "session.create");
        Assert.True(request.Params.GetProperty("hasSkillProvider").GetBoolean());
        Assert.False(request.Params.GetProperty("enableSkills").GetBoolean());
    }

    private static Dictionary<string, object?> SkillListRequest(string sessionId) => new()
    {
        ["sessionId"] = sessionId
    };

    private static Dictionary<string, object?> SkillReadRequest(string sessionId, string name) => new()
    {
        ["sessionId"] = sessionId,
        ["name"] = name
    };

    private static void AssertRpcError(InvalidOperationException exception, string expectedMessage)
    {
        Assert.Equal(expectedMessage, exception.Message);
        var error = Assert.IsType<JsonElement>(exception.Data["error"]);
        Assert.Equal(-32603, error.GetProperty("code").GetInt32());
        Assert.Equal(expectedMessage, error.GetProperty("message").GetString());
        Assert.False(error.TryGetProperty("data", out _), error.ToString());
    }

    private sealed class SkillProviderLogger : ILogger
    {
        private readonly ConcurrentQueue<(string Message, Exception? Exception)> _entries = new();

        public IReadOnlyCollection<(string Message, Exception? Exception)> Entries => _entries;

        public IDisposable? BeginScope<TState>(TState state) where TState : notnull => null;

        public bool IsEnabled(LogLevel logLevel) => true;

        public void Log<TState>(LogLevel logLevel, EventId eventId, TState state, Exception? exception, Func<TState, Exception?, string> formatter) =>
            _entries.Enqueue((formatter(state, exception), exception));
    }

    private sealed class TestSkillProvider : ISkillProvider
    {
        public int ListCalls;
        public int ReadCalls;

        public Func<CancellationToken, Task<IReadOnlyList<SkillProviderDescriptor>>> ListHandler { get; init; } =
            _ => Task.FromResult<IReadOnlyList<SkillProviderDescriptor>>(
            [
                new()
                {
                    Name = "dynamic",
                    Description = "Dynamic skill"
                }
            ]);

        public Func<string, CancellationToken, Task<string?>> ReadHandler { get; init; } =
            (name, _) => Task.FromResult<string?>($"# {name}");

        public Task<IReadOnlyList<SkillProviderDescriptor>> ListSkillsAsync(CancellationToken cancellationToken)
        {
            Interlocked.Increment(ref ListCalls);
            return ListHandler(cancellationToken);
        }

        public Task<string?> ReadSkillAsync(string name, CancellationToken cancellationToken)
        {
            Interlocked.Increment(ref ReadCalls);
            return ReadHandler(name, cancellationToken);
        }
    }

    private sealed partial class FakeCopilotServer
    {
        public async Task<Task<JsonElement>> SendRequestWithoutWaitingAsync(string method, Dictionary<string, object?> parameters)
        {
            var stream = _stream ?? throw new InvalidOperationException("Client is not connected.");
            var id = Interlocked.Increment(ref _nextRequestId);
            var completion = new TaskCompletionSource<JsonElement>(TaskCreationOptions.RunContinuationsAsynchronously);
            if (!_pendingRequests.TryAdd(id, completion))
            {
                throw new InvalidOperationException("Failed to track callback request.");
            }

            await WriteMessageAsync(stream, new Dictionary<string, object?>
            {
                ["jsonrpc"] = "2.0",
                ["id"] = id,
                ["method"] = method,
                ["params"] = parameters
            }, _cts.Token);

            return completion.Task.WaitAsync(_cts.Token);
        }
    }
}
#endif
