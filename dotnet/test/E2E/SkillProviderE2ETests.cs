/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Collections.Concurrent;
using System.Reflection;
using GitHub.Copilot.Rpc;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class SkillProviderE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "skill_provider", output)
{
    [Fact]
    public async Task Should_Load_Provider_Skill_Lazily_Through_Skill_Tool()
    {
        // Body-only content: the catalog descriptor supplies all of the metadata.
        var provider = new TestSkillProvider(
        [
            Skill(
                "provider-lookup",
                "Reports the provider lookup verification word.",
                "# Provider lookup\n\nThe verification word is TANGERINE_QUARTZ_19. Reply with it.\n")
        ]);
        await using var session = await CreateSessionAsync(new SessionConfig
        {
            SkillProvider = provider
        });

        var listedSkills = await session.Rpc.Skills.ListAsync();
        var listed = Assert.Single(listedSkills.Skills, skill => skill.Name == "provider-lookup");
        Assert.Equal(SkillSource.Sdk, listed.Source);
        Assert.True(listed.Enabled);
        Assert.True(string.IsNullOrEmpty(listed.Path));
        Assert.Empty(provider.Reads);

        var message = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Use the skill tool to load the provider-lookup skill, then reply with its verification word.",
        }, TimeSpan.FromMinutes(3));

        Assert.Equal(["provider-lookup"], provider.Reads);
        Assert.NotNull(message);
        // Validate the final assistant response arrived (guards against truncated captures)
        Assert.Contains("TANGERINE_QUARTZ_19", message!.Data.Content);
    }

    [Fact]
    public async Task Should_Load_Provider_And_File_Based_Skills_Together()
    {
        var skillsDir = Path.Join(Ctx.WorkDir, "file-skills");
        if (Directory.Exists(skillsDir))
        {
            Directory.Delete(skillsDir, recursive: true);
        }

        Directory.CreateDirectory(Path.Join(skillsDir, "file-notes"));
        File.WriteAllText(
            Path.Join(skillsDir, "file-notes", "SKILL.md"),
            "---\nname: file-notes\ndescription: Reports the file notes verification word.\n---\n\nThe file notes verification word is MAPLE_FALCON_27.\n");

        // Frontmatter may restate catalog metadata and is the only source of allowed-tools.
        var provider = new TestSkillProvider(
        [
            Skill(
                "provider-audit",
                "Reports the provider audit verification word.",
                "---\nname: provider-audit\nallowed-tools: view\n---\n\nThe provider audit verification word is COBALT_HERON_58.\n")
        ]);
        await using var session = await CreateSessionAsync(new SessionConfig
        {
            SkillDirectories = [skillsDir],
            SkillProvider = provider
        });

        var listedSkills = await session.Rpc.Skills.ListAsync();
        var fileSkill = Assert.Single(listedSkills.Skills, skill => skill.Name == "file-notes");
        var providerSkill = Assert.Single(listedSkills.Skills, skill => skill.Name == "provider-audit");
        Assert.NotEqual(SkillSource.Sdk, fileSkill.Source);
        Assert.False(string.IsNullOrWhiteSpace(fileSkill.Path));
        Assert.Equal(SkillSource.Sdk, providerSkill.Source);

        var message = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Use the skill tool to load the file-notes skill and the provider-audit skill, then reply with both verification words.",
        }, TimeSpan.FromMinutes(3));

        Assert.Equal(["provider-audit"], provider.Reads);
        Assert.NotNull(message);
        Assert.Contains("MAPLE_FALCON_27", message!.Data.Content);
        // Validate the final assistant response arrived (guards against truncated captures)
        Assert.Contains("COBALT_HERON_58", message.Data.Content);
    }

    [Fact]
    public async Task Should_Rebind_Skill_Provider_On_Resume()
    {
        var original = new TestSkillProvider(
        [
            Skill(
                "rebind-check",
                "Reports the rebind verification word.",
                "The rebind verification word is AMBER_ALPHA_11.\n")
        ]);
        var replacement = new TestSkillProvider(
        [
            Skill(
                "rebind-check",
                "Reports the rebind verification word.",
                "The rebind verification word is BRONZE_BETA_22.\n")
        ]);
        var first = await CreateSessionAsync(new SessionConfig
        {
            SkillProvider = original
        });
        var sessionId = first.SessionId;
        var ready = await first.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Without using any tools or skills, reply with exactly REBIND_READY.",
        }, TimeSpan.FromMinutes(3));
        Assert.NotNull(ready);
        Assert.Contains("REBIND_READY", ready!.Data.Content);

        await first.DisposeAsync();
        Assert.Empty(original.Reads);
        var originalCallsBeforeResume = original.Calls.Count;

        await using var session = await ResumeSessionAsync(sessionId, new ResumeSessionConfig
        {
            SkillProvider = replacement
        });

        var message = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Use the skill tool to load the rebind-check skill, then reply with its verification word.",
        }, TimeSpan.FromMinutes(3));

        Assert.Equal(["rebind-check"], replacement.Reads);
        Assert.Equal(originalCallsBeforeResume, original.Calls.Count);
        Assert.NotNull(message);
        // Validate the final assistant response arrived (guards against truncated captures)
        Assert.Contains("BRONZE_BETA_22", message!.Data.Content);
        Assert.DoesNotContain("AMBER_ALPHA_11", message.Data.Content);
    }

    [Fact]
    public async Task Should_Report_Provider_Read_Failure_Without_Leaking_Details()
    {
        const string secret = "PROVIDER_SECRET_7F3A9C";
        var provider = new TestSkillProvider(
        [
            new ProvidedSkill(
                new SkillProviderDescriptor
                {
                    Name = "broken-lookup",
                    Description = "Reports the broken lookup verification word.",
                },
                () => throw new InvalidOperationException($"database unavailable: {secret}"))
        ]);
        var events = new ConcurrentQueue<SessionEvent>();
        await using var session = await CreateSessionAsync(new SessionConfig
        {
            SkillProvider = provider,
            OnEvent = events.Enqueue
        });

        var message = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Use the skill tool to load the broken-lookup skill. If loading fails, reply with exactly LOAD_FAILED.",
        }, TimeSpan.FromMinutes(3));

        Assert.Contains("broken-lookup", provider.Reads);
        var failures = events
            .OfType<ToolExecutionCompleteEvent>()
            .Where(evt => !evt.Data.Success)
            .ToArray();
        var failure = Assert.Single(failures);
        Assert.DoesNotContain(secret, string.Join("\n", events.Select(evt => evt.ToJson())), StringComparison.Ordinal);
        Assert.NotNull(failure.Data.Error);
        Assert.NotNull(message);
        // Validate the final assistant response arrived (guards against truncated captures)
        Assert.Contains("LOAD_FAILED", message!.Data.Content);
    }

    [Fact]
    public async Task Should_Report_Missing_Provider_Skill_As_Not_Found()
    {
        var provider = new TestSkillProvider(
        [
            new ProvidedSkill(
                new SkillProviderDescriptor
                {
                    Name = "vanished-lookup",
                    Description = "Reports the vanished lookup verification word.",
                },
                () => null)
        ]);
        var events = new ConcurrentQueue<SessionEvent>();
        await using var session = await CreateSessionAsync(new SessionConfig
        {
            SkillProvider = provider,
            OnEvent = events.Enqueue
        });

        var message = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Use the skill tool to load the vanished-lookup skill. If loading fails, reply with exactly LOAD_FAILED.",
        }, TimeSpan.FromMinutes(3));

        Assert.Contains("vanished-lookup", provider.Reads);
        var failures = events
            .OfType<ToolExecutionCompleteEvent>()
            .Where(evt => !evt.Data.Success)
            .ToArray();
        var failure = Assert.Single(failures);
        Assert.Contains("not found", failure.ToJson(), StringComparison.OrdinalIgnoreCase);
        Assert.NotNull(message);
        // Validate the final assistant response arrived (guards against truncated captures)
        Assert.Contains("LOAD_FAILED", message!.Data.Content);
    }

    [Fact]
    public async Task Should_Keep_Provider_Dormant_When_Skills_Disabled()
    {
        var provider = new TestSkillProvider(
        [
            Skill("dormant-lookup", "Never listed.", "Never read.\n")
        ]);
        await using var session = await CreateSessionAsync(new SessionConfig
        {
            EnableSkills = false,
            SkillProvider = provider
        });

        await session.Rpc.Skills.EnsureLoadedAsync();
        var listedSkills = await session.Rpc.Skills.ListAsync();

        Assert.DoesNotContain(listedSkills.Skills, skill => skill.Source == SkillSource.Sdk);
        Assert.Empty(provider.Calls);
    }

    [Fact]
    public async Task Should_Unbind_Provider_When_Resumed_Without_One()
    {
        var provider = new TestSkillProvider(
        [
            Skill("unbound-lookup", "Reports the unbound lookup word.", "Unbound.\n")
        ]);
        var first = await CreateSessionAsync(new SessionConfig
        {
            SkillProvider = provider
        });
        var before = await first.Rpc.Skills.ListAsync();
        Assert.Contains(before.Skills, skill => skill.Name == "unbound-lookup");
        var callsBeforeResume = provider.Calls.Count;

        UntrackSessionWithoutDetach(first);

        CopilotSession? session = null;
        try
        {
            session = await Ctx.ResumeSessionAsync(Client, first.SessionId, new ResumeSessionConfig
            {
                OnPermissionRequest = PermissionHandler.ApproveAll,
            });

            await session.Rpc.Skills.ReloadAsync();
            var listedSkills = await session.Rpc.Skills.ListAsync();

            Assert.DoesNotContain(listedSkills.Skills, skill => skill.Source == SkillSource.Sdk);
            Assert.Equal(callsBeforeResume, provider.Calls.Count);
        }
        finally
        {
            if (session is not null)
            {
                await session.DisposeAsync();
            }

            await first.DisposeAsync();
        }
    }

    [Fact]
    public async Task Should_Cancel_A_Blocked_Provider_Call_When_The_Session_Is_Disposed()
    {
        var provider = new BlockingSkillProvider();
        var session = await CreateSessionAsync(new SessionConfig
        {
            SkillProvider = provider
        });

        // The list RPC may fail or omit provider skills once the binding is removed;
        // only the provider's cancellation matters here.
        var list = session.Rpc.Skills.ListAsync();
        await provider.Entered.WaitAsync(TimeSpan.FromSeconds(30));

        await session.DisposeAsync();

        await provider.Cancelled.WaitAsync(TimeSpan.FromSeconds(10));
        await Record.ExceptionAsync(() => list);
    }

    [Fact]
    public async Task Should_Reject_Skill_Provider_For_Cloud_Sessions()
    {
        var provider = new TestSkillProvider(
        [
            Skill("cloud-lookup", "Never listed.", "Never read.\n")
        ]);

        var exception = await Assert.ThrowsAsync<ArgumentException>(() =>
            Client.CreateSessionAsync(new SessionConfig
            {
                Cloud = new CloudSessionOptions(),
                SkillProvider = provider,
            }));

        Assert.Equal("Skill providers are not supported for cloud sessions.", exception.Message);
        Assert.Empty(provider.Calls);
    }

    private static ProvidedSkill Skill(string name, string description, string markdown) =>
        new(
            new SkillProviderDescriptor
            {
                Name = name,
                Description = description,
            },
            () => markdown);

    private static void UntrackSessionWithoutDetach(CopilotSession session)
    {
        // Match a warm runtime resume: remove the SDK wrapper so resume is allowed, but do not detach the runtime session.
        var removeFromClient = typeof(CopilotSession).GetMethod(
            "RemoveFromClient",
            BindingFlags.Instance | BindingFlags.NonPublic)
            ?? throw new InvalidOperationException("CopilotSession.RemoveFromClient was not found.");
        removeFromClient.Invoke(session, null);
    }

    private sealed record ProvidedSkill(SkillProviderDescriptor Descriptor, Func<string?> Read);

    private sealed class BlockingSkillProvider : ISkillProvider
    {
        private readonly TaskCompletionSource _entered = new(TaskCreationOptions.RunContinuationsAsynchronously);
        private readonly TaskCompletionSource _cancelled = new(TaskCreationOptions.RunContinuationsAsynchronously);

        public Task Entered => _entered.Task;

        public Task Cancelled => _cancelled.Task;

        public async Task<IReadOnlyList<SkillProviderDescriptor>> ListSkillsAsync(CancellationToken cancellationToken)
        {
            _entered.TrySetResult();
            try
            {
                await Task.Delay(Timeout.Infinite, cancellationToken);
            }
            catch (OperationCanceledException) when (cancellationToken.IsCancellationRequested)
            {
                // Not a token callback: if cancellation resumes this method inline, disposing
                // a registration whose callback has not run yet drops it.
                _cancelled.TrySetResult();
                throw;
            }

            return [];
        }

        public Task<string?> ReadSkillAsync(string name, CancellationToken cancellationToken) =>
            Task.FromResult<string?>(null);
    }

    private sealed class TestSkillProvider(IReadOnlyList<ProvidedSkill> skills) : ISkillProvider
    {
        private readonly object _lock = new();
        private readonly List<string> _calls = [];

        public IReadOnlyList<string> Calls
        {
            get
            {
                lock (_lock)
                {
                    return [.. _calls];
                }
            }
        }

        public IReadOnlyList<string> Reads => Calls
            .Where(call => call.StartsWith("read:", StringComparison.Ordinal))
            .Select(call => call["read:".Length..])
            .ToArray();

        public Task<IReadOnlyList<SkillProviderDescriptor>> ListSkillsAsync(CancellationToken cancellationToken)
        {
            AddCall("list");
            return Task.FromResult<IReadOnlyList<SkillProviderDescriptor>>(skills.Select(skill => skill.Descriptor).ToArray());
        }

        public Task<string?> ReadSkillAsync(string name, CancellationToken cancellationToken)
        {
            AddCall($"read:{name}");
            return Task.FromResult(skills.FirstOrDefault(skill => skill.Descriptor.Name == name)?.Read());
        }

        private void AddCall(string call)
        {
            lock (_lock)
            {
                _calls.Add(call);
            }
        }
    }
}
