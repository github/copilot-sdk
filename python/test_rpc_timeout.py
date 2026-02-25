"""Tests for timeout parameter on generated RPC methods."""
from unittest.mock import AsyncMock

import pytest

from copilot.generated.rpc import (
    FleetApi,
    Mode,
    ModeApi,
    ModelsApi,
    PlanApi,
    SessionFleetStartParams,
    SessionModeSetParams,
)


class TestRpcTimeout:
    """Tests for timeout forwarding across all four codegen branches:
    - session-scoped with params
    - session-scoped without params
    - server-scoped with params  (not tested — no server+params method exists yet)
    - server-scoped without params
    """

    # ── session-scoped, with params ──────────────────────────────────

    @pytest.mark.asyncio
    async def test_default_timeout_not_forwarded(self):
        client = AsyncMock()
        client.request = AsyncMock(return_value={"started": True})
        api = FleetApi(client, "sess-1")

        await api.start(SessionFleetStartParams(prompt="go"))

        client.request.assert_called_once()
        _, kwargs = client.request.call_args
        assert "timeout" not in kwargs

    @pytest.mark.asyncio
    async def test_custom_timeout_forwarded(self):
        client = AsyncMock()
        client.request = AsyncMock(return_value={"started": True})
        api = FleetApi(client, "sess-1")

        await api.start(SessionFleetStartParams(prompt="go"), timeout=600.0)

        _, kwargs = client.request.call_args
        assert kwargs["timeout"] == 600.0

    @pytest.mark.asyncio
    async def test_timeout_on_session_params_method(self):
        client = AsyncMock()
        client.request = AsyncMock(return_value={"mode": "plan"})
        api = ModeApi(client, "sess-1")

        await api.set(SessionModeSetParams(mode=Mode.PLAN), timeout=120.0)

        _, kwargs = client.request.call_args
        assert kwargs["timeout"] == 120.0

    # ── session-scoped, no params ────────────────────────────────────

    @pytest.mark.asyncio
    async def test_timeout_on_session_no_params_method(self):
        client = AsyncMock()
        client.request = AsyncMock(return_value={"exists": True})
        api = PlanApi(client, "sess-1")

        await api.read(timeout=90.0)

        _, kwargs = client.request.call_args
        assert kwargs["timeout"] == 90.0

    @pytest.mark.asyncio
    async def test_default_timeout_on_session_no_params_method(self):
        client = AsyncMock()
        client.request = AsyncMock(return_value={"exists": True})
        api = PlanApi(client, "sess-1")

        await api.read()

        _, kwargs = client.request.call_args
        assert "timeout" not in kwargs

    # ── server-scoped, no params ─────────────────────────────────────

    @pytest.mark.asyncio
    async def test_timeout_on_server_no_params_method(self):
        client = AsyncMock()
        client.request = AsyncMock(return_value={"models": []})
        api = ModelsApi(client)

        await api.list(timeout=45.0)

        _, kwargs = client.request.call_args
        assert kwargs["timeout"] == 45.0

    @pytest.mark.asyncio
    async def test_default_timeout_on_server_no_params_method(self):
        client = AsyncMock()
        client.request = AsyncMock(return_value={"models": []})
        api = ModelsApi(client)

        await api.list()

        _, kwargs = client.request.call_args
        assert "timeout" not in kwargs
