"""E2E Connection Token Tests

Tests for the optional TCP ``connect`` token handshake. Mirrors the Node SDK's
``connection_token.test.ts``.
"""

import os
import shutil
import tempfile

import pytest
import pytest_asyncio

from copilot import CopilotClient
from copilot.client import ExternalServerConfig, SubprocessConfig
from copilot.session import PermissionHandler

from .testharness.proxy import CapiProxy

pytestmark = pytest.mark.asyncio(loop_scope="module")


class ConnectionTokenContext:
    """Spawns a TCP CLI server with an explicit connection token."""

    def __init__(self, token: str | None):
        self.token = token
        self.cli_path: str = ""
        self.home_dir: str = ""
        self.work_dir: str = ""
        self.proxy_url: str = ""
        self._proxy: CapiProxy | None = None
        self._client: CopilotClient | None = None

    async def setup(self):
        from .testharness.context import get_cli_path_for_tests

        self.cli_path = get_cli_path_for_tests()
        self.home_dir = tempfile.mkdtemp(prefix="copilot-token-config-")
        self.work_dir = tempfile.mkdtemp(prefix="copilot-token-work-")

        self._proxy = CapiProxy()
        self.proxy_url = await self._proxy.start()

        github_token = (
            "fake-token-for-e2e-tests" if os.environ.get("GITHUB_ACTIONS") == "true" else None
        )

        self._client = CopilotClient(
            SubprocessConfig(
                cli_path=self.cli_path,
                cwd=self.work_dir,
                env=self.get_env(),
                use_stdio=False,
                tcp_connection_token=self.token,
                github_token=github_token,
            )
        )

        # Trigger the spawn + connect handshake so the server is listening.
        await self._client.start()

    async def teardown(self):
        if self._client:
            try:
                await self._client.stop()
            except Exception:
                pass
            self._client = None
        if self._proxy:
            await self._proxy.stop(skip_writing_cache=True)
            self._proxy = None
        if self.home_dir and os.path.exists(self.home_dir):
            shutil.rmtree(self.home_dir, ignore_errors=True)
        if self.work_dir and os.path.exists(self.work_dir):
            shutil.rmtree(self.work_dir, ignore_errors=True)

    def get_env(self) -> dict:
        env = os.environ.copy()
        env.update(
            {
                "COPILOT_API_URL": self.proxy_url,
                "COPILOT_HOME": self.home_dir,
                "XDG_CONFIG_HOME": self.home_dir,
                "XDG_STATE_HOME": self.home_dir,
            }
        )
        return env

    @property
    def client(self) -> CopilotClient:
        if not self._client:
            raise RuntimeError("Context not set up")
        return self._client


@pytest_asyncio.fixture(scope="module", loop_scope="module")
async def explicit_token_ctx():
    ctx = ConnectionTokenContext(token="right-token")
    await ctx.setup()
    yield ctx
    await ctx.teardown()


@pytest_asyncio.fixture(scope="module", loop_scope="module")
async def auto_token_ctx():
    ctx = ConnectionTokenContext(token=None)
    await ctx.setup()
    yield ctx
    await ctx.teardown()


class TestConnectionToken:
    async def test_explicit_token_round_trips(self, explicit_token_ctx: ConnectionTokenContext):
        """Client started with an explicit token can ping successfully."""
        # Sanity-check that the token was forwarded to the spawned CLI and the
        # `connect` handshake succeeded; a real ping must round-trip.
        response = await explicit_token_ctx.client.ping("hi")
        assert response.message == "pong: hi"

        # Bonus: a fresh session round-trip also exercises the live connection.
        session = await explicit_token_ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        await session.disconnect()

    async def test_auto_generated_token_round_trips(self, auto_token_ctx: ConnectionTokenContext):
        """When the SDK spawns its own CLI in TCP mode without an explicit token,
        the auto-generated UUID is forwarded and the `connect` handshake succeeds."""
        response = await auto_token_ctx.client.ping("hi")
        assert response.message == "pong: hi"

    async def test_wrong_token_is_rejected(self, explicit_token_ctx: ConnectionTokenContext):
        """A sibling client connecting with the wrong token is rejected."""
        port = explicit_token_ctx.client.actual_port
        assert port is not None

        wrong = CopilotClient(
            ExternalServerConfig(url=f"localhost:{port}", tcp_connection_token="wrong")
        )
        try:
            with pytest.raises(Exception, match="AUTHENTICATION_FAILED"):
                await wrong.start()
        finally:
            try:
                await wrong.force_stop()
            except Exception:
                pass

    async def test_missing_token_is_rejected(self, explicit_token_ctx: ConnectionTokenContext):
        """A sibling client with no token is rejected when the server requires one."""
        port = explicit_token_ctx.client.actual_port
        assert port is not None

        no_token = CopilotClient(ExternalServerConfig(url=f"localhost:{port}"))
        try:
            with pytest.raises(Exception, match="AUTHENTICATION_FAILED"):
                await no_token.start()
        finally:
            try:
                await no_token.force_stop()
            except Exception:
                pass
