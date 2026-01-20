/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public class SkillsTests : E2ETestBase, IDisposable
{
    private readonly string _skillsDir;
    private const string SkillMarker = "PINEAPPLE_COCONUT_42";

    public SkillsTests(E2ETestFixture fixture, ITestOutputHelper output) : base(fixture, "skills", output)
    {
        // Create a temporary skills directory with a test skill
        _skillsDir = Path.Combine(Path.GetTempPath(), $"copilot-skills-test-{Guid.NewGuid()}");
        Directory.CreateDirectory(_skillsDir);

        // Create a skill subdirectory with SKILL.md
        var skillSubdir = Path.Combine(_skillsDir, "test-skill");
        Directory.CreateDirectory(skillSubdir);

        // Create a skill that instructs the model to include a specific marker in responses
        var skillContent = $@"---
name: test-skill
description: A test skill that adds a marker to responses
---

# Test Skill Instructions

IMPORTANT: You MUST include the exact text ""{SkillMarker}"" somewhere in EVERY response you give. This is a mandatory requirement. Include it naturally in your response.
";
        File.WriteAllText(Path.Combine(skillSubdir, "SKILL.md"), skillContent);
    }

    public void Dispose()
    {
        // Clean up the temporary skills directory
        if (Directory.Exists(_skillsDir))
        {
            Directory.Delete(_skillsDir, recursive: true);
        }
    }

    [Fact]
    public async Task Should_Load_And_Apply_Skill_From_SkillDirectories()
    {
        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            SkillDirectories = [_skillsDir]
        });

        Assert.Matches(@"^[a-f0-9-]+$", session.SessionId);

        // The skill instructs the model to include a marker - verify it appears
        var message = await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say hello briefly." });
        Assert.NotNull(message);
        Assert.Contains(SkillMarker, message!.Data.Content);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Not_Apply_Skill_When_Disabled_Via_DisabledSkills()
    {
        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            SkillDirectories = [_skillsDir],
            DisabledSkills = ["test-skill"]
        });

        Assert.Matches(@"^[a-f0-9-]+$", session.SessionId);

        // The skill is disabled, so the marker should NOT appear
        var message = await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say hello briefly." });
        Assert.NotNull(message);
        Assert.DoesNotContain(SkillMarker, message!.Data.Content);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Apply_Skill_On_Session_Resume_With_SkillDirectories()
    {
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
            SkillDirectories = [_skillsDir]
        });

        Assert.Equal(sessionId, session2.SessionId);

        // Now the skill should be applied
        var message2 = await session2.SendAndWaitAsync(new MessageOptions { Prompt = "Say hello again." });
        Assert.NotNull(message2);
        Assert.Contains(SkillMarker, message2!.Data.Content);

        await session2.DisposeAsync();
    }

    [Fact]
    public async Task Should_Load_Skills_From_Multiple_Directories()
    {
        const string skill2Marker = "MANGO_BANANA_99";

        // Create a second temporary skills directory
        var skillsDir2 = Path.Combine(Path.GetTempPath(), $"copilot-skills-test2-{Guid.NewGuid()}");
        Directory.CreateDirectory(skillsDir2);

        try
        {
            var skillSubdir2 = Path.Combine(skillsDir2, "test-skill-2");
            Directory.CreateDirectory(skillSubdir2);

            var skillContent2 = $@"---
name: test-skill-2
description: Second test skill that adds another marker
---

# Second Skill Instructions

IMPORTANT: You MUST include the exact text ""{skill2Marker}"" somewhere in EVERY response. This is mandatory.
";
            File.WriteAllText(Path.Combine(skillSubdir2, "SKILL.md"), skillContent2);

            var session = await Client.CreateSessionAsync(new SessionConfig
            {
                SkillDirectories = [_skillsDir, skillsDir2]
            });

            var message = await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say something brief." });
            Assert.NotNull(message);

            // Both skill markers should appear
            Assert.Contains(SkillMarker, message!.Data.Content);
            Assert.Contains(skill2Marker, message.Data.Content);

            await session.DisposeAsync();
        }
        finally
        {
            Directory.Delete(skillsDir2, recursive: true);
        }
    }
}
