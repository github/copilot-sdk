/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System.Text.Json;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    private const string SkillMarkdown = """
        ---
        name: native-skill
        description: A text-only test skill
        user-invocable: false
        disable-model-invocation: false
        argument-hint: "[topic]"
        ---
        # Native skill
        Use résumé examples.
        """;

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task SkillProvider_Is_Bound_Before_Session_Open_And_Reads_Lazily(bool resume)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        Task<JsonElement>? listDuringOpen = null;
        server.BeforeResponseAsync = (request, _) =>
        {
            if (request.Method is "session.create" or "session.resume")
            {
                // Send the callback before the open response without blocking the response read loop.
                listDuringOpen = server.SendRequestAsync("skillProvider.list", new Dictionary<string, object?>
                {
                    ["sessionId"] = request.Params.GetProperty("sessionId").GetString()
                });
            }
            return Task.CompletedTask;
        };
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var provider = new TestSkillProvider
        {
            Skills =
            [
                new SkillProviderDescriptor
                {
                    Name = "native-skill",
                    Description = "A text-only test skill",
                    UserInvocable = false,
                    DisableModelInvocation = false,
                    ArgumentHint = "[topic]"
                },
                new SkillProviderDescriptor { Name = "minimal-skill", Description = "Only required metadata" }
            ]
        };

        await using var session = await OpenSessionWithSkillsAsync(client, resume, provider);

        var request = Assert.Single(server.Requests, request => request.Method == (resume ? "session.resume" : "session.create"));
        Assert.True(request.Params.GetProperty("hasSkillProvider").GetBoolean());
        Assert.False(request.Params.TryGetProperty("skillProvider", out _));
        Assert.False(request.Params.TryGetProperty("skillDirectories", out _));
        Assert.False(request.Params.TryGetProperty("tools", out _));
        Assert.False(string.IsNullOrEmpty(request.Params.GetProperty("sessionId").GetString()));
        var callback = listDuringOpen;
        Assert.NotNull(callback);
        var listed = await callback.WaitAsync(TimeSpan.FromSeconds(10));
        Assert.Single(listed.EnumerateObject());
        var skills = listed.GetProperty("skills");
        Assert.Equal(2, skills.GetArrayLength());
        Assert.Equal("native-skill", skills[0].GetProperty("name").GetString());
        Assert.Equal("A text-only test skill", skills[0].GetProperty("description").GetString());
        Assert.False(skills[0].GetProperty("userInvocable").GetBoolean());
        Assert.False(skills[0].GetProperty("disableModelInvocation").GetBoolean());
        Assert.Equal("[topic]", skills[0].GetProperty("argumentHint").GetString());
        Assert.Equal(5, skills[0].EnumerateObject().Count());
        Assert.Equal(2, skills[1].EnumerateObject().Count());
        Assert.Equal(1, provider.ListCalls);
        Assert.Equal(0, provider.ReadCalls);

        var read = await server.SendRequestAsync("skillProvider.read", SkillRequest(session.SessionId));

        Assert.Single(read.EnumerateObject());
        Assert.Equal(SkillMarkdown, read.GetProperty("markdown").GetString());
        Assert.Equal("native-skill", provider.LastReadName);
        Assert.Equal(1, provider.ReadCalls);
        Assert.True(provider.ListCancellationToken.CanBeCanceled);
        Assert.Equal(provider.ListCancellationToken, provider.ReadCancellationToken);
    }

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task SkillProvider_Disabled_Skills_Keep_The_Provider_Bound(bool resume)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var provider = new TestSkillProvider { Skills = [] };

        await using var session = await OpenSessionWithSkillsAsync(client, resume, provider, enableSkills: false);

        var request = Assert.Single(server.Requests, request => request.Method == (resume ? "session.resume" : "session.create"));
        Assert.True(request.Params.GetProperty("hasSkillProvider").GetBoolean());
        Assert.False(request.Params.GetProperty("enableSkills").GetBoolean());
        Assert.Equal(0, provider.ListCalls);
        Assert.Equal(0, provider.ReadCalls);

        // The runtime controls skill loading; the SDK must not discard a dormant binding.
        var listed = await server.SendRequestAsync("skillProvider.list", SkillRequest(session.SessionId));
        Assert.Empty(listed.GetProperty("skills").EnumerateArray());
        Assert.Equal(1, provider.ListCalls);
    }

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task SkillProvider_Absence_Omits_Flag_And_Rejects_Callbacks(bool resume)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await OpenSessionWithSkillsAsync(client, resume, provider: null);

        var request = Assert.Single(server.Requests, request => request.Method == (resume ? "session.resume" : "session.create"));
        Assert.False(request.Params.TryGetProperty("hasSkillProvider", out _));
        Assert.False(request.Params.TryGetProperty("skillProvider", out _));
        foreach (var method in new[] { "skillProvider.list", "skillProvider.read" })
        {
            var error = await Assert.ThrowsAsync<InvalidOperationException>(() =>
                server.SendRequestAsync(method, SkillRequest(session.SessionId)));
            Assert.Contains($"No skill provider registered for session {session.SessionId}", error.Message);
        }
    }

    [Fact]
    public async Task SkillProvider_Callbacks_Are_Session_Scoped_And_Rebound_On_Resume()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var firstProvider = new TestSkillProvider { Markdown = SkillMarkdown + "\nFirst session." };
        var secondProvider = new TestSkillProvider { Markdown = SkillMarkdown + "\nSecond session." };
        await using var first = await client.CreateSessionAsync(new SessionConfig { SkillProvider = firstProvider });
        await using var second = await client.CreateSessionAsync(new SessionConfig { SkillProvider = secondProvider });

        var firstRead = await server.SendRequestAsync("skillProvider.read", SkillRequest(first.SessionId));
        var secondRead = await server.SendRequestAsync("skillProvider.read", SkillRequest(second.SessionId));
        Assert.Equal(firstProvider.Markdown, firstRead.GetProperty("markdown").GetString());
        Assert.Equal(secondProvider.Markdown, secondRead.GetProperty("markdown").GetString());

        foreach (var method in new[] { "skillProvider.list", "skillProvider.read" })
        {
            var error = await Assert.ThrowsAsync<InvalidOperationException>(() =>
                server.SendRequestAsync(method, SkillRequest("unknown-session")));
            Assert.Contains("Unknown session unknown-session", error.Message);
        }
        Assert.Equal(0, firstProvider.ListCalls);
        Assert.Equal(0, secondProvider.ListCalls);
        Assert.Equal(1, firstProvider.ReadCalls);
        Assert.Equal(1, secondProvider.ReadCalls);

        await first.DisposeAsync();
        var disposedError = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync("skillProvider.read", SkillRequest(first.SessionId)));
        Assert.Contains($"Unknown session {first.SessionId}", disposedError.Message);

        var replacementProvider = new TestSkillProvider { Markdown = SkillMarkdown + "\nResumed session." };
        await using var resumed = await client.ResumeSessionAsync(first.SessionId, new ResumeSessionConfig
        {
            SkillProvider = replacementProvider
        });
        var resumedRead = await server.SendRequestAsync("skillProvider.read", SkillRequest(resumed.SessionId));
        Assert.Equal(replacementProvider.Markdown, resumedRead.GetProperty("markdown").GetString());
        Assert.Equal(1, firstProvider.ReadCalls);
        Assert.Equal(1, replacementProvider.ReadCalls);

        await resumed.DisposeAsync();
        await using var unbound = await client.ResumeSessionAsync(first.SessionId, new ResumeSessionConfig());
        var unboundError = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync("skillProvider.read", SkillRequest(unbound.SessionId)));
        Assert.Contains("No skill provider registered", unboundError.Message);
        Assert.Equal(1, replacementProvider.ReadCalls);
        Assert.Equal(1, secondProvider.ReadCalls);
    }

    [Fact]
    public async Task SkillProvider_Is_Unregistered_After_Failed_Creation()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        server.FailSessionCreate();
        var provider = new TestSkillProvider();

        await Assert.ThrowsAsync<IOException>(() => client.CreateSessionAsync(new SessionConfig
        {
            SessionId = "failed-skill-session",
            SkillProvider = provider
        }));

        foreach (var method in new[] { "skillProvider.list", "skillProvider.read" })
        {
            var error = await Assert.ThrowsAsync<InvalidOperationException>(() =>
                server.SendRequestAsync(method, SkillRequest("failed-skill-session")));
            Assert.Contains("Unknown session failed-skill-session", error.Message);
        }
        Assert.Equal(0, provider.ListCalls);
        Assert.Equal(0, provider.ReadCalls);
    }

    [Fact]
    public async Task SkillProvider_Is_Unregistered_After_Deletion()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var provider = new TestSkillProvider();
        await using var session = await client.CreateSessionAsync(new SessionConfig { SkillProvider = provider });

        await client.DeleteSessionAsync(session.SessionId);

        var error = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync("skillProvider.list", SkillRequest(session.SessionId)));
        Assert.Contains($"Unknown session {session.SessionId}", error.Message);
        Assert.Equal(0, provider.ListCalls);
    }

    [Theory]
    [InlineData("skillProvider.list")]
    [InlineData("skillProvider.read")]
    public async Task SkillProvider_Exceptions_Are_Returned_As_Rpc_Errors(string method)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var provider = new TestSkillProvider { Error = new InvalidOperationException("skill provider failed") };
        await using var session = await client.CreateSessionAsync(new SessionConfig { SkillProvider = provider });

        var error = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync(method, SkillRequest(session.SessionId)));

        Assert.Contains("skill provider failed", error.Message);
    }

    [Fact]
    public async Task SkillProvider_Receives_The_Rpc_Lifetime_Cancellation_Token()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var provider = new TestSkillProvider();
        await using var session = await client.CreateSessionAsync(new SessionConfig { SkillProvider = provider });
        await server.SendRequestAsync("skillProvider.list", SkillRequest(session.SessionId));
        await server.SendRequestAsync("skillProvider.read", SkillRequest(session.SessionId));
        Assert.True(provider.ListCancellationToken.CanBeCanceled);
        Assert.True(provider.ReadCancellationToken.CanBeCanceled);
        Assert.False(provider.ListCancellationToken.IsCancellationRequested);
        Assert.False(provider.ReadCancellationToken.IsCancellationRequested);

        await client.ForceStopAsync();

        Assert.True(provider.ListCancellationToken.IsCancellationRequested);
        Assert.True(provider.ReadCancellationToken.IsCancellationRequested);
    }

    [Fact]
    public async Task SkillProvider_Rejects_Server_Assigned_Cloud_Ids_Before_Connecting()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });

        var error = await Assert.ThrowsAsync<ArgumentException>(() => client.CreateSessionAsync(new SessionConfig
        {
            Cloud = new CloudSessionOptions(),
            SkillProvider = new TestSkillProvider()
        }));

        Assert.Contains("server-assigned cloud session IDs are not supported", error.Message);
        Assert.Empty(server.Requests);
    }

    private static Task<CopilotSession> OpenSessionWithSkillsAsync(
        CopilotClient client, bool resume, SkillProvider? provider, bool? enableSkills = null)
        => resume
            ? client.ResumeSessionAsync("resumed-skill-session", new ResumeSessionConfig
            {
                SkillProvider = provider,
                EnableSkills = enableSkills
            })
            : client.CreateSessionAsync(new SessionConfig
            {
                SkillProvider = provider,
                EnableSkills = enableSkills
            });

    private static Dictionary<string, object?> SkillRequest(string sessionId)
        => new() { ["sessionId"] = sessionId, ["name"] = "native-skill" };

    private sealed class TestSkillProvider : SkillProvider
    {
        public IReadOnlyList<SkillProviderDescriptor> Skills { get; init; } =
        [
            new SkillProviderDescriptor
            {
                Name = "native-skill",
                Description = "A text-only test skill",
                UserInvocable = false,
                DisableModelInvocation = false,
                ArgumentHint = "[topic]"
            }
        ];

        public string Markdown { get; init; } = SkillMarkdown;
        public Exception? Error { get; init; }
        public int ListCalls { get; private set; }
        public int ReadCalls { get; private set; }
        public string? LastReadName { get; private set; }
        public CancellationToken ListCancellationToken { get; private set; }
        public CancellationToken ReadCancellationToken { get; private set; }

        public override Task<IReadOnlyList<SkillProviderDescriptor>> ListAsync(CancellationToken cancellationToken = default)
        {
            ListCalls++;
            ListCancellationToken = cancellationToken;
            return Error is { } error
                ? Task.FromException<IReadOnlyList<SkillProviderDescriptor>>(error)
                : Task.FromResult(Skills);
        }

        public override Task<string> ReadAsync(string name, CancellationToken cancellationToken = default)
        {
            ReadCalls++;
            LastReadName = name;
            ReadCancellationToken = cancellationToken;
            return Error is { } error ? Task.FromException<string>(error) : Task.FromResult(Markdown);
        }
    }
}
#endif
