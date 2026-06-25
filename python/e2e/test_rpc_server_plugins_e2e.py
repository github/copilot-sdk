"""
E2E coverage for server-scoped plugin and marketplace RPC methods.

Mirrors ``dotnet/test/E2E/RpcServerPluginsE2ETests.cs`` (snapshot
category ``rpc_server_plugins``).
"""

from __future__ import annotations

import contextlib
import shutil
import uuid
from pathlib import Path

import pytest

from copilot import CopilotClient, RuntimeConnection
from copilot.rpc import (
    PluginsDisableRequest,
    PluginsEnableRequest,
    PluginsInstallRequest,
    PluginsMarketplacesAddRequest,
    PluginsMarketplacesBrowseRequest,
    PluginsMarketplacesRefreshRequest,
    PluginsMarketplacesRemoveRequest,
    PluginsUninstallRequest,
    PluginsUpdateRequest,
)

from .testharness import DEFAULT_GITHUB_TOKEN, E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")

MARKETPLACE_NAME = "csharp-e2e-marketplace"
PLUGIN_NAME = "csharp-e2e-plugin"
DIRECT_PLUGIN_NAME = "csharp-e2e-direct"


def _write_skill_file(plugin_dir: Path) -> None:
    skill = """---
name: csharp-e2e-skill
description: A demo skill contributed by the E2E test plugin.
---
# Demo Skill

This skill exists so the plugin reports at least one installed skill.
"""
    (plugin_dir / "SKILL.md").write_text(skill, encoding="utf-8", newline="\n")


def _create_local_marketplace_fixture(ctx: E2ETestContext) -> Path:
    directory = Path(ctx.work_dir) / f"copilot-e2e-mp-{uuid.uuid4().hex}"
    directory.mkdir(parents=True)
    manifest = f"""{{
  "name": "{MARKETPLACE_NAME}",
  "owner": {{ "name": "Copilot SDK E2E" }},
  "metadata": {{ "description": "Local marketplace fixture for SDK E2E tests." }},
  "plugins": [
    {{
      "name": "{PLUGIN_NAME}",
      "source": "./{PLUGIN_NAME}",
      "description": "E2E demo plugin advertised by the local marketplace.",
      "version": "1.0.0"
    }}
  ]
}}
"""
    (directory / "marketplace.json").write_text(manifest, encoding="utf-8", newline="\n")
    plugin_dir = directory / PLUGIN_NAME
    plugin_dir.mkdir()
    _write_skill_file(plugin_dir)
    return directory


def _create_direct_plugin_fixture(ctx: E2ETestContext) -> Path:
    directory = Path(ctx.work_dir) / f"copilot-e2e-plugin-{uuid.uuid4().hex}"
    directory.mkdir(parents=True)
    manifest = f"""{{
  "name": "{DIRECT_PLUGIN_NAME}",
  "description": "E2E demo plugin installed directly from a local path.",
  "version": "1.0.0"
}}
"""
    (directory / "plugin.json").write_text(manifest, encoding="utf-8", newline="\n")
    _write_skill_file(directory)
    return directory


async def _create_isolated_client(ctx: E2ETestContext) -> tuple[CopilotClient, Path]:
    home = Path(ctx.work_dir) / f"copilot-e2e-home-{uuid.uuid4().hex}"
    home.mkdir(parents=True)
    env = ctx.get_env()
    for key in ("COPILOT_HOME", "GH_CONFIG_DIR", "XDG_CONFIG_HOME", "XDG_STATE_HOME"):
        env[key] = str(home)
    client = CopilotClient(
        connection=RuntimeConnection.for_stdio(path=ctx.cli_path),
        working_directory=ctx.work_dir,
        env=env,
        github_token=DEFAULT_GITHUB_TOKEN,
    )
    await client.start()
    return client, home


async def _dispose_isolated(client: CopilotClient, home: Path, fixture_dir: Path | None) -> None:
    with contextlib.suppress(ExceptionGroup):
        await client.stop()
    with contextlib.suppress(OSError):
        shutil.rmtree(home, ignore_errors=True)
    if fixture_dir is not None:
        with contextlib.suppress(OSError):
            shutil.rmtree(fixture_dir, ignore_errors=True)


class TestRpcServerPlugins:
    async def test_should_install_list_and_uninstall_plugin_from_local_marketplace(
        self, ctx: E2ETestContext
    ):
        marketplace_dir = _create_local_marketplace_fixture(ctx)
        client, home = await _create_isolated_client(ctx)
        try:
            await client.rpc.plugins.marketplaces.add(
                PluginsMarketplacesAddRequest(source=str(marketplace_dir))
            )

            spec = f"{PLUGIN_NAME}@{MARKETPLACE_NAME}"
            install = await client.rpc.plugins.install(PluginsInstallRequest(source=spec))

            assert install.plugin.name == PLUGIN_NAME
            assert install.plugin.marketplace == MARKETPLACE_NAME
            assert install.plugin.enabled is True
            assert install.skills_installed >= 1
            assert install.deprecation_warning is None

            after_install = await client.rpc.plugins.list()
            listed = [
                p
                for p in after_install.plugins
                if p.name == PLUGIN_NAME and p.marketplace == MARKETPLACE_NAME
            ]
            assert len(listed) == 1
            assert listed[0].enabled is True

            await client.rpc.plugins.uninstall(PluginsUninstallRequest(name=spec))

            after_uninstall = await client.rpc.plugins.list()
            assert not any(
                p.name == PLUGIN_NAME and p.marketplace == MARKETPLACE_NAME
                for p in after_uninstall.plugins
            )
        finally:
            await _dispose_isolated(client, home, marketplace_dir)

    async def test_should_enable_and_disable_marketplace_plugin(self, ctx: E2ETestContext):
        marketplace_dir = _create_local_marketplace_fixture(ctx)
        client, home = await _create_isolated_client(ctx)
        try:
            spec = f"{PLUGIN_NAME}@{MARKETPLACE_NAME}"
            await client.rpc.plugins.marketplaces.add(
                PluginsMarketplacesAddRequest(source=str(marketplace_dir))
            )
            await client.rpc.plugins.install(PluginsInstallRequest(source=spec))

            await client.rpc.plugins.disable(PluginsDisableRequest(names=[spec]))
            assert _single_marketplace_plugin(await client.rpc.plugins.list()).enabled is False

            await client.rpc.plugins.enable(PluginsEnableRequest(names=[spec]))
            assert _single_marketplace_plugin(await client.rpc.plugins.list()).enabled is True
        finally:
            await _dispose_isolated(client, home, marketplace_dir)

    async def test_should_update_single_marketplace_plugin(self, ctx: E2ETestContext):
        marketplace_dir = _create_local_marketplace_fixture(ctx)
        client, home = await _create_isolated_client(ctx)
        try:
            spec = f"{PLUGIN_NAME}@{MARKETPLACE_NAME}"
            await client.rpc.plugins.marketplaces.add(
                PluginsMarketplacesAddRequest(source=str(marketplace_dir))
            )
            await client.rpc.plugins.install(PluginsInstallRequest(source=spec))

            update = await client.rpc.plugins.update(PluginsUpdateRequest(name=spec))

            assert update.skills_installed >= 1
            assert update.previous_version == "1.0.0"
            assert update.new_version == "1.0.0"
        finally:
            await _dispose_isolated(client, home, marketplace_dir)

    async def test_should_update_all_installed_plugins(self, ctx: E2ETestContext):
        marketplace_dir = _create_local_marketplace_fixture(ctx)
        client, home = await _create_isolated_client(ctx)
        try:
            spec = f"{PLUGIN_NAME}@{MARKETPLACE_NAME}"
            await client.rpc.plugins.marketplaces.add(
                PluginsMarketplacesAddRequest(source=str(marketplace_dir))
            )
            await client.rpc.plugins.install(PluginsInstallRequest(source=spec))

            result = await client.rpc.plugins.update_all()

            entries = [
                r
                for r in result.results
                if r.name == PLUGIN_NAME and r.marketplace == MARKETPLACE_NAME
            ]
            assert len(entries) == 1
            entry = entries[0]
            assert entry.success is True, entry.error
            assert entry.skills_installed is not None and entry.skills_installed >= 1
        finally:
            await _dispose_isolated(client, home, marketplace_dir)

    async def test_should_install_direct_local_plugin_with_deprecation_warning(
        self, ctx: E2ETestContext
    ):
        plugin_dir = _create_direct_plugin_fixture(ctx)
        client, home = await _create_isolated_client(ctx)
        try:
            install = await client.rpc.plugins.install(
                PluginsInstallRequest(source=str(plugin_dir))
            )

            assert install.plugin.name == DIRECT_PLUGIN_NAME
            assert install.plugin.marketplace == ""
            assert install.deprecation_warning is not None
            assert "deprecated" in install.deprecation_warning.lower()
            assert install.skills_installed >= 1

            after_install = await client.rpc.plugins.list()
            assert len([p for p in after_install.plugins if p.name == DIRECT_PLUGIN_NAME]) == 1

            await client.rpc.plugins.uninstall(PluginsUninstallRequest(name=DIRECT_PLUGIN_NAME))

            after_uninstall = await client.rpc.plugins.list()
            assert not any(p.name == DIRECT_PLUGIN_NAME for p in after_uninstall.plugins)
        finally:
            await _dispose_isolated(client, home, plugin_dir)

    async def test_should_list_browse_refresh_and_remove_local_marketplace(
        self, ctx: E2ETestContext
    ):
        marketplace_dir = _create_local_marketplace_fixture(ctx)
        client, home = await _create_isolated_client(ctx)
        try:
            add = await client.rpc.plugins.marketplaces.add(
                PluginsMarketplacesAddRequest(source=str(marketplace_dir))
            )
            assert add.name == MARKETPLACE_NAME

            marketplaces = await client.rpc.plugins.marketplaces.list()
            mine = [m for m in marketplaces.marketplaces if m.name == MARKETPLACE_NAME]
            assert len(mine) == 1
            assert mine[0].is_default is not True
            assert any(m.is_default is True for m in marketplaces.marketplaces)

            browse = await client.rpc.plugins.marketplaces.browse(
                PluginsMarketplacesBrowseRequest(name=MARKETPLACE_NAME)
            )
            advertised = [p for p in browse.plugins if p.name == PLUGIN_NAME]
            assert len(advertised) == 1
            assert (advertised[0].description or "").strip()

            refresh = await client.rpc.plugins.marketplaces.refresh(
                PluginsMarketplacesRefreshRequest(name=MARKETPLACE_NAME)
            )
            refreshed = [r for r in refresh.results if r.name == MARKETPLACE_NAME]
            assert len(refreshed) == 1
            assert refreshed[0].success is True, refreshed[0].error

            remove = await client.rpc.plugins.marketplaces.remove(
                PluginsMarketplacesRemoveRequest(name=MARKETPLACE_NAME)
            )
            assert remove.removed is True

            after_remove = await client.rpc.plugins.marketplaces.list()
            assert not any(m.name == MARKETPLACE_NAME for m in after_remove.marketplaces)
        finally:
            await _dispose_isolated(client, home, marketplace_dir)

    async def test_should_reload_mcp_config_cache(self, ctx: E2ETestContext):
        client, home = await _create_isolated_client(ctx)
        try:
            await client.rpc.mcp.config.reload()
        finally:
            await _dispose_isolated(client, home, None)


def _single_marketplace_plugin(plugin_list):
    plugins = [
        p
        for p in plugin_list.plugins
        if p.name == PLUGIN_NAME and p.marketplace == MARKETPLACE_NAME
    ]
    assert len(plugins) == 1
    return plugins[0]
