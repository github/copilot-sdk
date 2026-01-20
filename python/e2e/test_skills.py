"""
Tests for skills configuration functionality
"""

import os
import shutil
import tempfile
import pytest

from .testharness import E2ETestContext, get_final_assistant_message

pytestmark = pytest.mark.asyncio(loop_scope="module")

SKILL_MARKER = "PINEAPPLE_COCONUT_42"


@pytest.fixture(scope="module")
def skills_dir():
    """Create a temporary skills directory with a test skill"""
    skills_dir = tempfile.mkdtemp(prefix="copilot-skills-test-")

    # Create a skill subdirectory with SKILL.md
    skill_subdir = os.path.join(skills_dir, "test-skill")
    os.makedirs(skill_subdir, exist_ok=True)

    # Create a skill that instructs the model to include a specific marker in responses
    skill_content = f"""---
name: test-skill
description: A test skill that adds a marker to responses
---

# Test Skill Instructions

IMPORTANT: You MUST include the exact text "{SKILL_MARKER}" somewhere in EVERY response you give. This is a mandatory requirement. Include it naturally in your response.
"""
    with open(os.path.join(skill_subdir, "SKILL.md"), "w") as f:
        f.write(skill_content)

    yield skills_dir

    # Clean up
    shutil.rmtree(skills_dir, ignore_errors=True)


class TestSkillBehavior:
    async def test_load_and_apply_skill_from_skill_directories(
        self, ctx: E2ETestContext, skills_dir: str
    ):
        """Test that skills are loaded and applied from skillDirectories"""
        session = await ctx.client.create_session({"skill_directories": [skills_dir]})

        assert session.session_id is not None

        # The skill instructs the model to include a marker - verify it appears
        message = await session.send_and_wait({"prompt": "Say hello briefly."})
        assert message is not None
        assert SKILL_MARKER in message.data.content

        await session.destroy()

    async def test_not_apply_skill_when_disabled_via_disabled_skills(
        self, ctx: E2ETestContext, skills_dir: str
    ):
        """Test that disabledSkills prevents skill from being applied"""
        session = await ctx.client.create_session(
            {"skill_directories": [skills_dir], "disabled_skills": ["test-skill"]}
        )

        assert session.session_id is not None

        # The skill is disabled, so the marker should NOT appear
        message = await session.send_and_wait({"prompt": "Say hello briefly."})
        assert message is not None
        assert SKILL_MARKER not in message.data.content

        await session.destroy()

    async def test_apply_skill_on_session_resume_with_skill_directories(
        self, ctx: E2ETestContext, skills_dir: str
    ):
        """Test that skills are applied when added on session resume"""
        # Create a session without skills first
        session1 = await ctx.client.create_session()
        session_id = session1.session_id

        # First message without skill - marker should not appear
        message1 = await session1.send_and_wait({"prompt": "Say hi."})
        assert message1 is not None
        assert SKILL_MARKER not in message1.data.content

        # Resume with skillDirectories - skill should now be active
        session2 = await ctx.client.resume_session(
            session_id, {"skill_directories": [skills_dir]}
        )

        assert session2.session_id == session_id

        # Now the skill should be applied
        message2 = await session2.send_and_wait({"prompt": "Say hello again."})
        assert message2 is not None
        assert SKILL_MARKER in message2.data.content

        await session2.destroy()


class TestMultipleSkills:
    async def test_load_skills_from_multiple_directories(
        self, ctx: E2ETestContext, skills_dir: str
    ):
        """Test that skills from multiple directories are all loaded"""
        skill2_marker = "MANGO_BANANA_99"

        # Create a second temporary skills directory
        skills_dir2 = tempfile.mkdtemp(prefix="copilot-skills-test2-")

        try:
            skill_subdir2 = os.path.join(skills_dir2, "test-skill-2")
            os.makedirs(skill_subdir2, exist_ok=True)

            skill_content2 = f"""---
name: test-skill-2
description: Second test skill that adds another marker
---

# Second Skill Instructions

IMPORTANT: You MUST include the exact text "{skill2_marker}" somewhere in EVERY response. This is mandatory.
"""
            with open(os.path.join(skill_subdir2, "SKILL.md"), "w") as f:
                f.write(skill_content2)

            session = await ctx.client.create_session(
                {"skill_directories": [skills_dir, skills_dir2]}
            )

            message = await session.send_and_wait({"prompt": "Say something brief."})
            assert message is not None

            # Both skill markers should appear
            assert SKILL_MARKER in message.data.content
            assert skill2_marker in message.data.content

            await session.destroy()
        finally:
            shutil.rmtree(skills_dir2, ignore_errors=True)
