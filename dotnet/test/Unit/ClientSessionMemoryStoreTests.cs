/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text.Json;
using System.Threading.Tasks;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    [Fact]
    public async Task MemoryStore_Create_And_Resume_Flag_Store_And_Serve_Callback_Before_Response()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = _ => null;
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var storedSessionIds = new List<string>();
        var memory = new MemoryConfiguration
        {
            Enabled = true,
            StoreMemory = (stored, _) =>
            {
                lock (storedSessionIds)
                {
                    storedSessionIds.Add(stored.SessionId);
                }
                return Task.CompletedTask;
            }
        };
        var storeResults = new List<Task<JsonElement>>();
        server.BeforeResponseAsync = async (request, _) =>
        {
            if (request.Method is "session.create" or "session.resume")
            {
                storeResults.Add(await server.SendRequestWithoutWaitingAsync(
                    "memoryStore.store",
                    MemoryStoreRequest(request.Params.GetProperty("sessionId").GetString()!)));
            }
        };

        await using var created = await client.CreateSessionAsync(new SessionConfig { SessionId = "memory-create", Memory = memory });
        await using var resumed = await client.ResumeSessionAsync("memory-resume", new ResumeSessionConfig { Memory = memory });

        foreach (var method in new[] { "session.create", "session.resume" })
        {
            var wireParams = Assert.Single(server.Requests, request => request.Method == method).Params;
            Assert.Equal("""{"enabled":true}""", wireParams.GetProperty("memory").GetRawText());
            Assert.True(wireParams.GetProperty("hasMemoryStore").GetBoolean());
        }

        Assert.Equal(2, storeResults.Count);
        foreach (var result in storeResults)
        {
            Assert.Equal(JsonValueKind.Null, (await result.WaitAsync(TimeSpan.FromSeconds(5))).ValueKind);
        }
        Assert.Equal(["memory-create", "memory-resume"], storedSessionIds.Order());
        Assert.DoesNotContain(server.Requests, request => request.Method == "session.memory.configureStore");

        server.BeforeResponseAsync = null;
        server.ClearRequests();
        await using var withoutStore = await client.CreateSessionAsync(new SessionConfig { Memory = new MemoryConfiguration { Enabled = true } });
        Assert.False(Assert.Single(server.Requests, request => request.Method == "session.create")
            .Params.TryGetProperty("hasMemoryStore", out _));
    }

    [Fact]
    public async Task MemoryStore_Requires_Enabled_Memory_Before_Sending_Requests()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = _ => null;
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var memory = new MemoryConfiguration { Enabled = false, StoreMemory = (_, _) => Task.CompletedTask };

        var createError = await Assert.ThrowsAsync<ArgumentException>(() =>
            client.CreateSessionAsync(new SessionConfig { Memory = memory }));
        var resumeError = await Assert.ThrowsAsync<ArgumentException>(() =>
            client.ResumeSessionAsync("memory-resume", new ResumeSessionConfig { Memory = memory }));

        Assert.StartsWith("Memory.StoreMemory requires Memory.Enabled to be true.", createError.Message);
        Assert.StartsWith("Memory.StoreMemory requires Memory.Enabled to be true.", resumeError.Message);
        Assert.DoesNotContain(server.Requests, request => request.Method is "session.create" or "session.resume");

        var cloudError = await Assert.ThrowsAsync<ArgumentException>(() => client.CreateSessionAsync(new SessionConfig
        {
            Cloud = new CloudSessionOptions(),
            Memory = new MemoryConfiguration { Enabled = true, StoreMemory = (_, _) => Task.CompletedTask }
        }));
        Assert.Equal("Memory stores are not supported for cloud sessions.", cloudError.Message);
    }

    [Fact]
    public async Task MemoryStore_Store_Request_Invokes_Callback()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = _ => null;
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        StoredMemory? stored = null;
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            Memory = new MemoryConfiguration
            {
                Enabled = true,
                StoreMemory = (memory, _) =>
                {
                    stored = memory;
                    return Task.CompletedTask;
                }
            }
        });

        var result = await server.SendRequestAsync("memoryStore.store", MemoryStoreRequest(session.SessionId));

        Assert.Equal(JsonValueKind.Null, result.ValueKind);
        Assert.NotNull(stored);
        Assert.Equal(session.SessionId, stored.SessionId);
        Assert.Equal("package manager", stored.Subject);
        Assert.Equal("Use pnpm.", stored.Fact);
        Assert.Equal("package.json", stored.Citations);
        Assert.Equal("The workspace uses pnpm.", stored.Reason);
    }

    [Fact]
    public async Task MemoryStore_Store_Request_Failures_Are_Generic()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = _ => null;
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var failing = await client.CreateSessionAsync(new SessionConfig
        {
            Memory = new MemoryConfiguration
            {
                Enabled = true,
                StoreMemory = (_, _) => throw new InvalidOperationException("secret backend detail")
            }
        });
        await using var noStore = await client.CreateSessionAsync(new SessionConfig());

        var failure = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync("memoryStore.store", MemoryStoreRequest(failing.SessionId)));
        Assert.Equal("Memory store callback failed", failure.Message);
        Assert.DoesNotContain("secret", Assert.IsType<JsonElement>(failure.Data["error"]).GetRawText(), StringComparison.OrdinalIgnoreCase);

        var missing = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync("memoryStore.store", MemoryStoreRequest(noStore.SessionId)));
        Assert.Equal("No memory store is registered", missing.Message);

        var unknown = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync("memoryStore.store", MemoryStoreRequest("unknown-session")));
        Assert.Equal("Memory store owner is unavailable", unknown.Message);
    }

    private static Dictionary<string, object?> MemoryStoreRequest(string sessionId) => new()
    {
        ["sessionId"] = sessionId,
        ["subject"] = "package manager",
        ["fact"] = "Use pnpm.",
        ["citations"] = "package.json",
        ["reason"] = "The workspace uses pnpm."
    };
}
#endif
