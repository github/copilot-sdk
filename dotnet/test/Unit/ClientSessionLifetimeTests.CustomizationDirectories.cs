// Copyright (c) Microsoft Corporation. All rights reserved.

#if NET8_0_OR_GREATER
using GitHub.Copilot.Rpc;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    [Fact]
    public async Task CustomizationDirectories_Preserve_Positional_Discovery_Calls()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = _ => new Dictionary<string, object?>
        {
            ["agents"] = Array.Empty<object>(),
            ["hooks"] = Array.Empty<object>(),
            ["sources"] = Array.Empty<object>(),
            ["paths"] = Array.Empty<object>()
        };
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        IList<string> projects = ["project-root"];
        await client.StartAsync();
        await client.Rpc.Agents.DiscoverAsync(projects, true, CancellationToken.None);
        await client.Rpc.Agents.GetDiscoveryPathsAsync(projects, true, CancellationToken.None);
        await client.Rpc.Hooks.DiscoverAsync(projects, true, CancellationToken.None);
        await client.Rpc.Instructions.DiscoverAsync(projects, true, CancellationToken.None);
        await client.Rpc.Instructions.GetDiscoveryPathsAsync(projects, true, CancellationToken.None);
        await client.Rpc.Skills.GetDiscoveryPathsAsync(projects, ["ignored"], true, CancellationToken.None);
        foreach (var method in new[]
        {
            "agents.discover", "agents.getDiscoveryPaths", "hooks.discover",
            "instructions.discover", "instructions.getDiscoveryPaths", "skills.getDiscoveryPaths"
        })
        {
            var payload = Assert.Single(server.Requests, request => request.Method == method).Params;
            Assert.Equal("project-root", Assert.Single(payload.GetProperty("projectPaths").EnumerateArray()).GetString());
            foreach (var key in new[] { "agentDirectories", "hookDirectories", "instructionDirectories", "skillDirectories" })
            {
                Assert.False(payload.TryGetProperty(key, out _));
            }
        }
        await client.Rpc.Agents.DiscoverAsync(new AgentsDiscoverRequest
        {
            AgentDirectories = ["explicit-agent-root"]
        });
        var explicitPayload = server.Requests.Last(request => request.Method == "agents.discover").Params;
        Assert.Equal("explicit-agent-root", Assert.Single(explicitPayload.GetProperty("agentDirectories").EnumerateArray()).GetString());
        Assert.False(explicitPayload.TryGetProperty("projectPaths", out _));
    }

    [Theory]
    [InlineData(0)]
    [InlineData(1)]
    [InlineData(2)]
    public async Task CustomizationDirectories_Are_Preserved_On_Create_And_Resume(int kind)
    {
        IList<string>? roots = kind switch
        {
            0 => null,
            1 => [],
            _ => ["relative-root", "other-root"]
        };
        var directories = new Dictionary<string, IList<string>?>
        {
            ["skillDirectories"] = roots?.Select(root => root + "-skills").ToList(),
            ["agentDirectories"] = roots?.Select(root => root + "-agents").ToList(),
            ["instructionDirectories"] = roots?.Select(root => root + "-instructions").ToList(),
            ["hookDirectories"] = roots?.Select(root => root + "-hooks").ToList()
        };
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var created = await client.CreateSessionAsync(new SessionConfig
        {
            WorkingDirectory = "request-cwd",
            EnableConfigDiscovery = false,
            EnableFileHooks = false,
            SkillDirectories = directories["skillDirectories"],
            AgentDirectories = directories["agentDirectories"],
            InstructionDirectories = directories["instructionDirectories"],
            HookDirectories = directories["hookDirectories"]
        });
        await using var resumed = await client.ResumeSessionAsync("resumed", new ResumeSessionConfig
        {
            WorkingDirectory = "resume-cwd",
            EnableConfigDiscovery = false,
            EnableFileHooks = false,
            SkillDirectories = directories["skillDirectories"],
            AgentDirectories = directories["agentDirectories"],
            InstructionDirectories = directories["instructionDirectories"],
            HookDirectories = directories["hookDirectories"]
        });
        foreach (var method in new[] { "session.create", "session.resume" })
        {
            var payload = Assert.Single(server.Requests, request => request.Method == method).Params;
            foreach (var entry in directories)
            {
                if (entry.Value is null)
                {
                    Assert.False(payload.TryGetProperty(entry.Key, out _));
                }
                else
                {
                    Assert.Equal(entry.Value, payload.GetProperty(entry.Key).EnumerateArray().Select(item => item.GetString()));
                }
            }
            Assert.False(payload.GetProperty("enableConfigDiscovery").GetBoolean());
            Assert.False(payload.GetProperty("enableFileHooks").GetBoolean());
            Assert.False(payload.TryGetProperty("additionalDirectories", out _));
            Assert.False(payload.TryGetProperty("customAgents", out _));
            Assert.False(payload.TryGetProperty("hooks", out _));
        }
    }

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public void CustomizationDirectories_Clone_Owns_Its_Lists(bool resume)
    {
        SessionConfigBase original;
        SessionConfigBase clone;
        if (resume)
        {
            var config = new ResumeSessionConfig
            {
                AgentDirectories = ["agents"],
                HookDirectories = ["hooks"]
            };
            original = config;
            clone = config.Clone();
        }
        else
        {
            var config = new SessionConfig
            {
                AgentDirectories = ["agents"],
                HookDirectories = ["hooks"]
            };
            original = config;
            clone = config.Clone();
        }
        clone.AgentDirectories!.Add("clone-agents");
        clone.HookDirectories!.Add("clone-hooks");
        Assert.Equal("agents", Assert.Single(original.AgentDirectories!));
        Assert.Equal("hooks", Assert.Single(original.HookDirectories!));
        Assert.Collection(clone.AgentDirectories, item => Assert.Equal("agents", item), item => Assert.Equal("clone-agents", item));
        Assert.Collection(clone.HookDirectories, item => Assert.Equal("hooks", item), item => Assert.Equal("clone-hooks", item));
    }
}
#endif
