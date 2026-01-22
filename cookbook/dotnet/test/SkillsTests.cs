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

    [Fact]
    public async Task Should_Load_And_Apply_Skill_From_SkillDirectories()
    {
        var skillsDir = CreateSkillDir();
        var session = await Client.CreateSessionAsync(new SessionConfig
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
        var session = await Client.CreateSessionAsync(new SessionConfig
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

    [Fact(Skip = "See the big comment around the equivalent test in the Node SDK. Skipped because the feature doesn't work correctly yet.")]
    public async Task Should_Apply_Skill_On_Session_Resume_With_SkillDirectories()
    {
        var skillsDir = CreateSkillDir();

        // Create a session without skills first
        var session1 = await Client.CreateSessionAsync();
        var sessionId = session1.SessionId;

        // First message without skill - marker should not appear
        var message1 = await session1.SendAndWaitAsync(new MessageOptions { Prompt = "Say hi." });
        Assert.NotNull(message1);
        Assert.DoesNotContain(SkillMarker, message1!.Data.Content);

        // Resume with skillDirectories - skill should now be active
        var session2 = await Client.ResumeSessionAsync(sessionId, new ResumeSessionConfig
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
