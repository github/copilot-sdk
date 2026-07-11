"""
E2E coverage for miscellaneous server-scoped RPC methods.

Mirrors ``dotnet/test/E2E/RpcServerMiscE2ETests.cs`` (snapshot category
``rpc_server_misc``).
"""

from __future__ import annotations

import contextlib
import shutil
import uuid
from pathlib import Path

import pytest

from copilot import CopilotClient, RuntimeConnection
from copilot.rpc import (
    AccountLoginRequest,
    AccountLogoutRequest,
    AgentRegistrySpawnRequest,
    SendAttachmentsToMessageParams,
    SessionsOpenResumeLast,
    SessionsOpenStatus,
    UserSettingsSetRequest,
)
from copilot.session import PermissionHandler

from .testharness import DEFAULT_GITHUB_TOKEN, E2ETestContext, wait_for_condition

pytestmark = pytest.mark.asyncio(loop_scope="module")


def _create_dedicated_client(ctx: E2ETestContext) -> CopilotClient:
    return CopilotClient(
        connection=RuntimeConnection.for_stdio(path=ctx.cli_path),
        working_directory=ctx.work_dir,
        env=ctx.get_env(),
        github_token=DEFAULT_GITHUB_TOKEN,
    )


async def _create_isolated_client(
    ctx: E2ETestContext, github_token: str | None = DEFAULT_GITHUB_TOKEN
) -> tuple[CopilotClient, Path]:
    home = Path(ctx.work_dir) / f"copilot-e2e-misc-home-{uuid.uuid4().hex}"
    home.mkdir(parents=True)
    env = ctx.get_env()
    for key in ("COPILOT_HOME", "GH_CONFIG_DIR", "XDG_CONFIG_HOME", "XDG_STATE_HOME"):
        env[key] = str(home)
    env["COPILOT_DEBUG_GITHUB_API_URL"] = ctx.proxy_url
    if github_token is None:
        env["GH_TOKEN"] = ""
        env["GITHUB_TOKEN"] = ""
    client = CopilotClient(
        connection=RuntimeConnection.for_stdio(path=ctx.cli_path),
        working_directory=ctx.work_dir,
        env=env,
        github_token=github_token,
        use_logged_in_user=False if github_token is None else None,
    )
    await client.start()
    return client, home


async def _stop_client(client: CopilotClient) -> None:
    with contextlib.suppress(ExceptionGroup, Exception):
        await client.stop()


async def _dispose_isolated(client: CopilotClient, home: Path) -> None:
    await _stop_client(client)
    with contextlib.suppress(OSError):
        shutil.rmtree(home, ignore_errors=True)


class TestRpcServerMisc:
    async def test_should_reload_user_settings(self, ctx: E2ETestContext):
        await ctx.client.start()

        await ctx.client.rpc.user.settings.reload()

    async def test_should_get_set_and_clear_user_settings(self, ctx: E2ETestContext):
        client, home = await _create_isolated_client(ctx)
        try:
            before = await client.rpc.user.settings.get()
            assert len(before.settings) > 0
            for key, setting in before.settings.items():
                assert key.strip()
                assert isinstance(setting.is_default, bool)

            setting_key, setting = next(
                (key, value)
                for key, value in before.settings.items()
                if isinstance(value.value, bool)
            )
            toggled_value = setting.value is not True

            set_result = await client.rpc.user.settings.set(
                UserSettingsSetRequest(settings={setting_key: toggled_value})
            )
            assert setting_key not in set_result.shadowed_keys

            await client.rpc.user.settings.reload()
            after_set = await client.rpc.user.settings.get()
            assert after_set.settings[setting_key].is_default is False
            assert after_set.settings[setting_key].value is toggled_value

            await client.rpc.user.settings.set(UserSettingsSetRequest(settings={setting_key: None}))
            await client.rpc.user.settings.reload()
            after_clear = await client.rpc.user.settings.get()
            assert after_clear.settings[setting_key].is_default is True
        finally:
            await _dispose_isolated(client, home)

    async def test_should_login_list_get_current_auth_and_logout_account(self, ctx: E2ETestContext):
        login = f"rpc-account-{uuid.uuid4().hex}"
        token = f"rpc-account-token-{uuid.uuid4().hex}"
        await ctx.set_copilot_user_by_token(
            token,
            {
                "login": login,
                "copilot_plan": "individual_pro",
                "endpoints": {
                    "api": ctx.proxy_url,
                    "telemetry": "https://localhost:1/telemetry",
                },
                "analytics_tracking_id": "rpc-account-tracking-id",
            },
        )

        client, home = await _create_isolated_client(ctx, github_token=None)
        try:
            initial = await client.rpc.account.get_current_auth()
            assert initial.auth_info is None

            login_result = await client.rpc.account.login(
                AccountLoginRequest(host="https://github.com", login=login, token=token)
            )
            assert isinstance(login_result.stored_in_vault, bool)

            current = await client.rpc.account.get_current_auth()
            assert current.auth_errors is None
            assert current.auth_info is not None
            assert current.auth_info.type == "user"
            assert current.auth_info.host == "https://github.com"
            assert current.auth_info.login == login

            users = await client.rpc.account.get_all_users()
            assert isinstance(users, list)
            account = next(
                (
                    user
                    for user in users
                    if user.auth_info.type == "user"
                    and getattr(user.auth_info, "login", None) == login
                ),
                None,
            )
            if account is not None:
                assert account.token == token

            logout = await client.rpc.account.logout(
                AccountLogoutRequest(auth_info=current.auth_info)
            )
            assert logout.has_more_users is False

            after_logout = await client.rpc.account.get_current_auth()
            assert after_logout.auth_info is None
        finally:
            await _dispose_isolated(client, home)

    async def test_should_report_agent_registry_spawn_gate_closed(self, ctx: E2ETestContext):
        client, home = await _create_isolated_client(ctx)
        try:
            with pytest.raises(Exception) as excinfo:
                await client.rpc.agent_registry.spawn(AgentRegistrySpawnRequest(cwd=ctx.work_dir))

            message = str(excinfo.value)
            assert "Unhandled method".lower() not in message.lower()
            assert "agentRegistry.spawn".lower() in message.lower()
            assert "not enabled" in message.lower() or "no delegate" in message.lower(), message
        finally:
            await _dispose_isolated(client, home)

    async def test_should_shut_down_owned_runtime(self, ctx: E2ETestContext):
        client = _create_dedicated_client(ctx)
        try:
            await client.start()
            await client.rpc.user.settings.reload()

            await client.rpc.runtime.shutdown()

            async def stopped_serving() -> bool:
                try:
                    await client.rpc.user.settings.reload(timeout=1.0)
                    return False
                except Exception:
                    return True

            await wait_for_condition(
                stopped_serving,
                timeout=15.0,
                poll_interval=0.1,
                timeout_message="Runtime kept serving RPCs after a graceful shutdown.",
            )
        finally:
            await _stop_client(client)

    async def test_should_report_not_found_when_opening_session_without_context(
        self, ctx: E2ETestContext
    ):
        client, home = await _create_isolated_client(ctx)
        try:
            result = await client.rpc.sessions.open(SessionsOpenResumeLast())

            assert result.status == SessionsOpenStatus.NOT_FOUND
            assert result.session_id is None
        finally:
            await _dispose_isolated(client, home)

    async def test_should_reject_send_attachments_from_non_extension_connection(
        self, ctx: E2ETestContext
    ):
        async with await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        ) as session:
            with pytest.raises(Exception) as excinfo:
                await session.rpc.extensions.send_attachments_to_message(
                    SendAttachmentsToMessageParams(attachments=[])
                )

            message = str(excinfo.value)
            assert "Unhandled method".lower() not in message.lower()
            assert "extension" in message.lower()
