"""
Tests for skills configuration functionality
"""

import os
import shutil

import pytest

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")

SKILL_MARKER = "PINEAPPLE_COCONUT_42"


@pytest.fixture(autouse=True)
def clean_skills_dir(ctx: E2ETestContext):
    """Ensure we start fresh each time"""
    skills_dir = os.path.join(ctx.work_dir, ".test_skills")
    if os.path.exists(skills_dir):
        shutil.rmtree(skills_dir)
    yield


def create_skill_dir(work_dir: str) -> str:
    """Create a skills directory in the working directory"""
    skills_dir = os.path.join(work_dir, ".test_skills")
    os.makedirs(skills_dir, exist_ok=True)

    # Create a skill subdirectory with SKILL.md
    skill_subdir = os.path.join(skills_dir, "test-skill")
    os.makedirs(skill_subdir, exist_ok=True)

    # Create a skill that instructs the model to include a specific marker in responses
    skill_content = f"""---
name: test-skill
description: A test skill that adds a marker to responses
---

# Test Skill Instructions

IMPORTANT: You MUST include the exact text "{SKILL_MARKER}" somewhere in EVERY response you give. \
This is a mandatory requirement. Include it naturally in your response.
""".replace("\r", "")
    with open(os.path.join(skill_subdir, "SKILL.md"), "w", newline="\n") as f:
        f.write(skill_content)

    return skills_dir


class TestSkillBehavior:
    async def test_should_load_and_apply_skill_from_skilldirectories(self, ctx: E2ETestContext):
        """Test that skills are loaded and applied from skillDirectories"""
        skills_dir = create_skill_dir(ctx.work_dir)
        session = await ctx.client.create_session({"skill_directories": [skills_dir]})

        assert session.session_id is not None

        # The skill instructs the model to include a marker - verify it appears
        message = await session.send_and_wait({"prompt": "Say hello briefly using the test skill."})
        assert message is not None
        assert SKILL_MARKER in message.data.content

        await session.destroy()

    async def test_should_not_apply_skill_when_disabled_via_disabledskills(
        self, ctx: E2ETestContext
    ):
        """Test that disabledSkills prevents skill from being applied"""
        skills_dir = create_skill_dir(ctx.work_dir)
        session = await ctx.client.create_session(
            {"skill_directories": [skills_dir], "disabled_skills": ["test-skill"]}
        )

        assert session.session_id is not None

        # The skill is disabled, so the marker should NOT appear
        message = await session.send_and_wait({"prompt": "Say hello briefly using the test skill."})
        assert message is not None
        assert SKILL_MARKER not in message.data.content

        await session.destroy()

    @pytest.mark.skip(
        reason="See the big comment around the equivalent test in the Node SDK. "
        "Skipped because the feature doesn't work correctly yet."
    )
    async def test_should_apply_skill_on_session_resume_with_skilldirectories(
        self, ctx: E2ETestContext
    ):
        """Test that skills are applied when added on session resume"""
        skills_dir = create_skill_dir(ctx.work_dir)

        # Create a session without skills first
        session1 = await ctx.client.create_session()
        session_id = session1.session_id

        # First message without skill - marker should not appear
        message1 = await session1.send_and_wait({"prompt": "Say hi."})
        assert message1 is not None
        assert SKILL_MARKER not in message1.data.content

        # Resume with skillDirectories - skill should now be active
        session2 = await ctx.client.resume_session(session_id, {"skill_directories": [skills_dir]})

        assert session2.session_id == session_id

        # Now the skill should be applied
        message2 = await session2.send_and_wait({"prompt": "Say hello again using the test skill."})
        assert message2 is not None
        assert SKILL_MARKER in message2.data.content

        await session2.destroy()
