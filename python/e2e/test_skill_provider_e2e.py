"""
E2E tests for SDK skill providers.
"""

import asyncio
import json
import os
from collections.abc import Callable
from dataclasses import dataclass

import pytest

from copilot import CloudSessionOptions, PermissionHandler, SkillProvider, SkillProviderDescriptor
from copilot.session_events import (
    SessionEvent,
    SessionEventType,
    SkillSource,
    ToolExecutionCompleteData,
    session_event_to_dict,
)

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


@dataclass(frozen=True)
class ProvidedSkill:
    descriptor: SkillProviderDescriptor
    read: Callable[[], str | None]


class InMemorySkillProvider(SkillProvider):
    """An in-memory provider that records every callback the runtime makes."""

    def __init__(self, skills: list[ProvidedSkill]):
        self.calls: list[str] = []
        self._skills = skills

    @property
    def reads(self) -> list[str]:
        return [call.removeprefix("read:") for call in self.calls if call.startswith("read:")]

    def list_skills(self) -> list[SkillProviderDescriptor]:
        self.calls.append("list")
        return [skill.descriptor for skill in self._skills]

    def read_skill(self, name: str) -> str | None:
        self.calls.append(f"read:{name}")
        for skill in self._skills:
            if skill.descriptor.name == name:
                return skill.read()
        return None


def skill(name: str, description: str, markdown: str) -> ProvidedSkill:
    return ProvidedSkill(
        descriptor=SkillProviderDescriptor(description=description, name=name),
        read=lambda: markdown,
    )


def failed_tool_executions(events: list[SessionEvent]) -> list[SessionEvent]:
    return [
        event
        for event in events
        if event.type == SessionEventType.TOOL_EXECUTION_COMPLETE
        and isinstance(event.data, ToolExecutionCompleteData)
        and not event.data.success
    ]


class TestSkillProviders:
    async def test_should_load_provider_skill_lazily_through_skill_tool(self, ctx: E2ETestContext):
        # Body-only content: the catalog descriptor supplies all of the metadata.
        provider = InMemorySkillProvider(
            [
                skill(
                    "provider-lookup",
                    "Reports the provider lookup verification word.",
                    "# Provider lookup\n\n"
                    "The verification word is TANGERINE_QUARTZ_19. Reply with it.\n",
                )
            ]
        )
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            skill_provider=provider,
        )

        try:
            skills = await session.rpc.skills.list()
            listed = next((s for s in skills.skills if s.name == "provider-lookup"), None)
            assert listed is not None
            assert listed.source == SkillSource.SDK
            assert listed.enabled is True
            assert (listed.path or "") == ""
            assert provider.reads == []

            message = await session.send_and_wait(
                "Use the skill tool to load the provider-lookup skill, "
                "then reply with its verification word."
            )

            assert provider.reads == ["provider-lookup"]
            assert message is not None
            # Validate the final assistant response arrived (guards against truncated captures)
            assert "TANGERINE_QUARTZ_19" in message.data.content
        finally:
            await session.disconnect()

    async def test_should_load_provider_and_file_based_skills_together(self, ctx: E2ETestContext):
        skills_dir = os.path.join(ctx.work_dir, "file-skills")
        file_skill_dir = os.path.join(skills_dir, "file-notes")
        os.makedirs(file_skill_dir, exist_ok=True)
        with open(os.path.join(file_skill_dir, "SKILL.md"), "w", newline="\n") as f:
            f.write(
                "---\n"
                "name: file-notes\n"
                "description: Reports the file notes verification word.\n"
                "---\n"
                "\n"
                "The file notes verification word is MAPLE_FALCON_27.\n"
            )

        # Frontmatter may restate catalog metadata and is the only source of allowed-tools.
        provider = InMemorySkillProvider(
            [
                skill(
                    "provider-audit",
                    "Reports the provider audit verification word.",
                    "---\n"
                    "name: provider-audit\n"
                    "allowed-tools: view\n"
                    "---\n"
                    "\n"
                    "The provider audit verification word is COBALT_HERON_58.\n",
                )
            ]
        )
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            skill_directories=[skills_dir],
            skill_provider=provider,
        )

        try:
            skills = await session.rpc.skills.list()
            file_skill = next((s for s in skills.skills if s.name == "file-notes"), None)
            provider_skill = next((s for s in skills.skills if s.name == "provider-audit"), None)
            assert file_skill is not None
            assert file_skill.source != SkillSource.SDK
            assert file_skill.path
            assert provider_skill is not None
            assert provider_skill.source == SkillSource.SDK

            message = await session.send_and_wait(
                "Use the skill tool to load the file-notes skill and the "
                "provider-audit skill, then reply with both verification words."
            )

            assert provider.reads == ["provider-audit"]
            assert message is not None
            assert "MAPLE_FALCON_27" in message.data.content
            # Validate the final assistant response arrived (guards against truncated captures)
            assert "COBALT_HERON_58" in message.data.content
        finally:
            await session.disconnect()

    async def test_should_rebind_skill_provider_on_resume(self, ctx: E2ETestContext):
        original = InMemorySkillProvider(
            [
                skill(
                    "rebind-check",
                    "Reports the rebind verification word.",
                    "The rebind verification word is AMBER_ALPHA_11.\n",
                )
            ]
        )
        replacement = InMemorySkillProvider(
            [
                skill(
                    "rebind-check",
                    "Reports the rebind verification word.",
                    "The rebind verification word is BRONZE_BETA_22.\n",
                )
            ]
        )
        first = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            skill_provider=original,
        )
        session_id = first.session_id
        try:
            # A completed turn persists the session so that it can be resumed after disconnecting.
            await first.send_and_wait(
                "Without using any tools or skills, reply with exactly REBIND_READY."
            )
        finally:
            await first.disconnect()
        assert original.reads == []
        original_calls_before_resume = len(original.calls)

        session = await ctx.client.resume_session(
            session_id,
            on_permission_request=PermissionHandler.approve_all,
            skill_provider=replacement,
        )

        try:
            message = await session.send_and_wait(
                "Use the skill tool to load the rebind-check skill, "
                "then reply with its verification word."
            )

            assert replacement.reads == ["rebind-check"]
            assert len(original.calls) == original_calls_before_resume
            assert message is not None
            # Validate the final assistant response arrived (guards against truncated captures)
            assert "BRONZE_BETA_22" in message.data.content
            assert "AMBER_ALPHA_11" not in message.data.content
        finally:
            await session.disconnect()

    async def test_should_report_provider_read_failure_without_leaking_details(
        self, ctx: E2ETestContext
    ):
        secret = "PROVIDER_SECRET_7F3A9C"

        def fail_with_secret() -> str | None:
            raise RuntimeError(f"database unavailable: {secret}")

        provider = InMemorySkillProvider(
            [
                ProvidedSkill(
                    descriptor=SkillProviderDescriptor(
                        description="Reports the broken lookup verification word.",
                        name="broken-lookup",
                    ),
                    read=fail_with_secret,
                )
            ]
        )
        events: list[SessionEvent] = []
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            skill_provider=provider,
            on_event=events.append,
        )

        try:
            message = await session.send_and_wait(
                "Use the skill tool to load the broken-lookup skill. "
                "If loading fails, reply with exactly LOAD_FAILED."
            )

            assert "broken-lookup" in provider.reads
            failures = failed_tool_executions(events)
            assert len(failures) == 1
            serialized_events = json.dumps([session_event_to_dict(event) for event in events])
            assert secret not in serialized_events
            assert message is not None
            # Validate the final assistant response arrived (guards against truncated captures)
            assert "LOAD_FAILED" in message.data.content
        finally:
            await session.disconnect()

    async def test_should_report_missing_provider_skill_as_not_found(self, ctx: E2ETestContext):
        provider = InMemorySkillProvider(
            [
                ProvidedSkill(
                    descriptor=SkillProviderDescriptor(
                        description="Reports the vanished lookup verification word.",
                        name="vanished-lookup",
                    ),
                    read=lambda: None,
                )
            ]
        )
        events: list[SessionEvent] = []
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            skill_provider=provider,
            on_event=events.append,
        )

        try:
            message = await session.send_and_wait(
                "Use the skill tool to load the vanished-lookup skill. "
                "If loading fails, reply with exactly LOAD_FAILED."
            )

            assert "vanished-lookup" in provider.reads
            failures = failed_tool_executions(events)
            assert len(failures) == 1
            data = failures[0].data
            assert isinstance(data, ToolExecutionCompleteData)
            assert data.error is not None
            assert "not found" in data.error.message.lower()
            assert message is not None
            # Validate the final assistant response arrived (guards against truncated captures)
            assert "LOAD_FAILED" in message.data.content
        finally:
            await session.disconnect()

    async def test_should_keep_provider_dormant_when_skills_disabled(self, ctx: E2ETestContext):
        provider = InMemorySkillProvider(
            [skill("dormant-lookup", "Never listed.", "Never read.\n")]
        )
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            enable_skills=False,
            skill_provider=provider,
        )

        try:
            await session.rpc.skills.ensure_loaded()
            skills = await session.rpc.skills.list()

            assert [s for s in skills.skills if s.source == SkillSource.SDK] == []
            assert provider.calls == []
        finally:
            await session.disconnect()

    async def test_should_unbind_provider_when_resumed_without_one(self, ctx: E2ETestContext):
        provider = InMemorySkillProvider(
            [skill("unbound-lookup", "Reports the unbound lookup word.", "Unbound.\n")]
        )
        first = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            skill_provider=provider,
        )
        before = await first.rpc.skills.list()
        assert any(s.name == "unbound-lookup" for s in before.skills)
        calls_before_resume = len(provider.calls)

        # Resume while the provider is still bound so the unbind is observable.
        session = await ctx.client.resume_session(
            first.session_id,
            on_permission_request=PermissionHandler.approve_all,
        )

        try:
            await session.rpc.skills.reload()
            skills = await session.rpc.skills.list()

            assert [s for s in skills.skills if s.source == SkillSource.SDK] == []
            assert len(provider.calls) == calls_before_resume
        finally:
            await session.disconnect()

    async def test_should_cancel_a_blocked_provider_call_when_the_session_disconnects(
        self, ctx: E2ETestContext
    ):
        entered = asyncio.Event()
        cancelled = asyncio.Event()

        class BlockingSkillProvider:
            async def list_skills(self) -> list[SkillProviderDescriptor]:
                entered.set()
                try:
                    await asyncio.Event().wait()
                except asyncio.CancelledError:
                    cancelled.set()
                    raise
                return []

            def read_skill(self, name: str) -> str | None:
                return None

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            skill_provider=BlockingSkillProvider(),
        )

        # The list RPC fails once the binding is removed; only the provider's
        # cancellation matters here.
        listing = asyncio.create_task(session.rpc.skills.list())
        try:
            await asyncio.wait_for(entered.wait(), timeout=30)
            await session.disconnect()
            await asyncio.wait_for(cancelled.wait(), timeout=10)
        finally:
            listing.cancel()
            await asyncio.gather(listing, return_exceptions=True)

    async def test_should_reject_skill_provider_for_cloud_sessions(self, ctx: E2ETestContext):
        provider = InMemorySkillProvider([skill("cloud-lookup", "Never listed.", "Never read.\n")])

        with pytest.raises(
            ValueError, match="Skill providers are not supported for cloud sessions."
        ):
            await ctx.client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                cloud=CloudSessionOptions(),
                skill_provider=provider,
            )

        assert provider.calls == []
