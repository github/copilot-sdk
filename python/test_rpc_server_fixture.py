"""Regression controls for the persisted-session E2E fixture's completion fence."""

from types import SimpleNamespace
from unittest.mock import AsyncMock

import pytest

from copilot.generated.session_events import AssistantMessageData
from copilot.rpc import LocalSessionMetadataValue, SessionContext
from e2e import test_rpc_server_e2e as scenario


@pytest.mark.asyncio
@pytest.mark.parametrize("disconnect_fails", [False, True])
async def test_persisted_session_fixture_observes_completion_before_save_and_cleanup(
    tmp_path, monkeypatch, disconnect_fails
):
    state = {}
    calls = []

    async def send(*args, **kwargs):
        calls.append("send-without-completion")

    async def send_and_wait(prompt, timeout):
        assert prompt == "Record a turn for sessions.list discriminator coverage"
        assert timeout == 60.0
        calls.append("completed-turn")
        return SimpleNamespace(
            data=AssistantMessageData(
                content=scenario.SYNTHETIC_TEXT, message_id="fixture-assistant"
            )
        )

    async def save(request):
        assert calls == ["completed-turn"], "persistence must follow observed turn completion"
        assert request.session_id == state["session_id"]
        calls.append("save")
        return object()

    async def listed(request):
        assert calls == ["completed-turn", "save"]
        return SimpleNamespace(
            sessions=[
                LocalSessionMetadataValue(
                    session_id=state["session_id"],
                    is_remote=False,
                    start_time="2026-01-01T00:00:00Z",
                    modified_time="2026-01-01T00:00:00Z",
                    context=SessionContext(cwd=state["working_directory"]),
                )
            ]
        )

    async def disconnect():
        calls.append("disconnect")
        if disconnect_fails:
            raise RuntimeError("controlled detach failure")

    session = SimpleNamespace(send=send, send_and_wait=send_and_wait, disconnect=disconnect)

    async def create_session(**kwargs):
        state.update(kwargs)
        return session

    client = SimpleNamespace(
        start=AsyncMock(),
        stop=AsyncMock(),
        create_session=create_session,
        rpc=SimpleNamespace(
            sessions=SimpleNamespace(
                save=save,
                list=listed,
                find_by_prefix=AsyncMock(return_value=SimpleNamespace(session_id=None)),
                find_by_task_id=AsyncMock(return_value=SimpleNamespace(session_id=None)),
                get_last_for_context=AsyncMock(return_value=SimpleNamespace(session_id=None)),
                get_sizes=AsyncMock(return_value=SimpleNamespace(sizes={})),
                check_in_use=AsyncMock(return_value=SimpleNamespace(in_use=[])),
            )
        ),
    )

    def make_client(ctx, token, **kwargs):
        if "request_handler" in kwargs:
            assert isinstance(kwargs["request_handler"], scenario._PersistedSessionRequestHandler)
        return client

    monkeypatch.setattr(scenario, "_configure_user", AsyncMock())
    monkeypatch.setattr(scenario, "_make_authed_client", make_client)
    run = scenario.TestRpcServer().test_should_list_find_and_inspect_persisted_session_state
    ctx = SimpleNamespace(work_dir=str(tmp_path))
    if disconnect_fails:
        with pytest.raises(RuntimeError, match="controlled detach failure"):
            await run(ctx)
    else:
        await run(ctx)
    assert calls == ["completed-turn", "save", "disconnect"]
    client.stop.assert_awaited_once()
