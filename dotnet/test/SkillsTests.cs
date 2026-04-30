/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public class SkillsTests : E2ETestBase
{
    private const string SkillMarker = "PINEAPPLE_COCONUT_42";

    private readonly string _workDir;

    public SkillsTests(E2ETestFixture fixture, ITestOutputHelper output) : base(fixture, "skills", output)
    {
        _workDir = fixture.Ctx.WorkDir;

        var skillsDir = Path.Join(_workDir, ".test_skills");
        if (Directory.Exists(skillsDir))
        {
            Directory.Delete(skillsDir, recursive: true);
        }
    }

    private string CreateSkillDir()
    {
        var skillsDir = Path.Join(_workDir, ".test_skills");
        Directory.CreateDirectory(skillsDir);

        // Create a skill subdirectory with SKILL.md
        var skillSubdir = Path.Join(skillsDir, "test-skill");
        Directory.CreateDirectory(skillSubdir);

        // Create a skill that instructs the model to include a specific marker in responses
        var skillContent = $@"---
name: test-skill
description: A test skill that adds a marker to responses
---

# Test Skill Instructions

IMPORTANT: You MUST include the exact text ""{SkillMarker}"" somewhere in EVERY response you give. This is a mandatory requirement. Include it naturally in your response.
".ReplaceLineEndings("\n");
        File.WriteAllText(Path.Join(skillSubdir, "SKILL.md"), skillContent);

        return skillsDir;
    }

    private static void CreateSkill(string skillsDir, string name, string description, string body)
    {
        var skillSubdir = Path.Join(skillsDir, name);
        Directory.CreateDirectory(skillSubdir);

        var skillContent = $@"---
name: {name}
description: {description}
---

{body}
".ReplaceLineEndings("\n");
        File.WriteAllText(Path.Join(skillSubdir, "SKILL.md"), skillContent);
    }

    [Fact]
    public async Task Should_Load_And_Apply_Skill_From_SkillDirectories()
    {
        var skillsDir = CreateSkillDir();
        var session = await CreateSessionAsync(new SessionConfig
        {
            SkillDirectories = [skillsDir]
        });

        Assert.Matches(@"^[a-f0-9-]+$", session.SessionId);

        // The skill instructs the model to include a marker - verify it appears
        var message = await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say hello briefly using the test skill." });
        Assert.NotNull(message);
        Assert.Contains(SkillMarker, message!.Data.Content);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Not_Apply_Skill_When_Disabled_Via_DisabledSkills()
    {
        var skillsDir = CreateSkillDir();
        var session = await CreateSessionAsync(new SessionConfig
        {
            SkillDirectories = [skillsDir],
            DisabledSkills = ["test-skill"]
        });

        Assert.Matches(@"^[a-f0-9-]+$", session.SessionId);

        // The skill is disabled, so the marker should NOT appear
        var message = await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say hello briefly using the test skill." });
        Assert.NotNull(message);
        Assert.DoesNotContain(SkillMarker, message!.Data.Content);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Control_Ambient_Project_Skills_With_EnableConfigDiscovery()
    {
        var projectDir = Path.Join(_workDir, $"config-discovery-{Guid.NewGuid():N}");
        var projectSkillsDir = Path.Join(projectDir, ".github", "skills");
        var skillName = $"ambient-skill-{Guid.NewGuid():N}".Substring(0, 32);
        Directory.CreateDirectory(projectSkillsDir);
        CreateSkill(
            projectSkillsDir,
            skillName,
            "A project skill discovered from .github/skills",
            "Use the exact phrase AMBIENT_DISCOVERY_SKILL when this skill is active.");

        var disabledSession = await CreateSessionAsync(new SessionConfig
        {
            WorkingDirectory = projectDir,
            EnableConfigDiscovery = false,
        });
        var disabledSkills = await disabledSession.Rpc.Skills.ListAsync();
        Assert.DoesNotContain(disabledSkills.Skills, skill => string.Equals(skill.Name, skillName, StringComparison.Ordinal));
        await disabledSession.DisposeAsync();

        var enabledSession = await CreateSessionAsync(new SessionConfig
        {
            WorkingDirectory = projectDir,
            EnableConfigDiscovery = true,
        });
        var enabledSkills = await enabledSession.Rpc.Skills.ListAsync();
        var discoveredSkill = Assert.Single(enabledSkills.Skills, skill => string.Equals(skill.Name, skillName, StringComparison.Ordinal));
        Assert.True(discoveredSkill.Enabled);
        Assert.Equal("project", discoveredSkill.Source);
        Assert.EndsWith(Path.Join(skillName, "SKILL.md"), discoveredSkill.Path);
        await enabledSession.DisposeAsync();
    }

    [Fact]
    public async Task Should_Allow_Agent_With_Skills_To_Invoke_Skill()
    {
        var skillsDir = CreateSkillDir();
        var customAgents = new List<CustomAgentConfig>
        {
            new CustomAgentConfig
            {
                Name = "skill-agent",
                Description = "An agent with access to test-skill",
                Prompt = "You are a helpful test agent.",
                Skills = ["test-skill"]
            }
        };

        var session = await CreateSessionAsync(new SessionConfig
        {
            SkillDirectories = [skillsDir],
            CustomAgents = customAgents,
            Agent = "skill-agent"
        });

        Assert.Matches(@"^[a-f0-9-]+$", session.SessionId);

        // The agent has Skills = ["test-skill"], so the skill content is preloaded into its context
        var message = await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say hello briefly using the test skill." });
        Assert.NotNull(message);
        Assert.Contains(SkillMarker, message!.Data.Content);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Not_Provide_Skills_To_Agent_Without_Skills_Field()
    {
        var skillsDir = CreateSkillDir();
        var customAgents = new List<CustomAgentConfig>
        {
            new CustomAgentConfig
            {
                Name = "no-skill-agent",
                Description = "An agent without skills access",
                Prompt = "You are a helpful test agent."
            }
        };

        var session = await CreateSessionAsync(new SessionConfig
        {
            SkillDirectories = [skillsDir],
            CustomAgents = customAgents,
            Agent = "no-skill-agent"
        });

        Assert.Matches(@"^[a-f0-9-]+$", session.SessionId);

        // The agent has no Skills field, so no skill content is injected
        var message = await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say hello briefly using the test skill." });
        Assert.NotNull(message);
        Assert.DoesNotContain(SkillMarker, message!.Data.Content);

        await session.DisposeAsync();
    }

    [Fact(Skip = "See the big comment around the equivalent test in the Node SDK. Skipped because the feature doesn't work correctly yet.")]
    public async Task Should_Apply_Skill_On_Session_Resume_With_SkillDirectories()
    {
        var skillsDir = CreateSkillDir();

        // Create a session without skills first
        var session1 = await CreateSessionAsync();
        var sessionId = session1.SessionId;

        // First message without skill - marker should not appear
        var message1 = await session1.SendAndWaitAsync(new MessageOptions { Prompt = "Say hi." });
        Assert.NotNull(message1);
        Assert.DoesNotContain(SkillMarker, message1!.Data.Content);

        // Resume with skillDirectories - skill should now be active
        var session2 = await ResumeSessionAsync(sessionId, new ResumeSessionConfig
        {
            SkillDirectories = [skillsDir]
        });

        Assert.Equal(sessionId, session2.SessionId);

        // Now the skill should be applied
        var message2 = await session2.SendAndWaitAsync(new MessageOptions { Prompt = "Say hello again using the test skill." });
        Assert.NotNull(message2);
        Assert.Contains(SkillMarker, message2!.Data.Content);

        await session2.DisposeAsync();
    }
}
