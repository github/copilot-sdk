// Copyright (c) Microsoft Corporation. All rights reserved.

#if NET8_0_OR_GREATER
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    [Theory]
    [InlineData(null)]
    [InlineData(false)]
    [InlineData(true)]
    public async Task EnableGitHubCacheStore_Is_Forwarded_On_Create_And_Resume(bool? enabled)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var created = await client.CreateSessionAsync(new SessionConfig
        {
            EnableGitHubCacheStore = enabled
        });
        await using var resumed = await client.ResumeSessionAsync("resumed", new ResumeSessionConfig
        {
            EnableGitHubCacheStore = enabled
        });
        foreach (var method in new[] { "session.create", "session.resume" })
        {
            var payload = Assert.Single(server.Requests, request => request.Method == method).Params;
            if (enabled is null)
            {
                Assert.False(payload.TryGetProperty("enableGitHubCacheStore", out _));
            }
            else
            {
                Assert.Equal(enabled.Value, payload.GetProperty("enableGitHubCacheStore").GetBoolean());
            }
        }
    }
}
#endif
