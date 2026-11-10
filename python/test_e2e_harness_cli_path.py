"""Unit tests for the E2E harness's same-checkout CLI preparation."""

from __future__ import annotations

from typing import get_type_hints
from unittest.mock import AsyncMock

import pytest

from copilot import CopilotClient, RuntimeConnection
from copilot.client import CloudSessionOptions
from e2e.testharness import context


def test_fixture_client_preserves_session_option_annotations_for_ahp():
    assert get_type_hints(context._TestClient.create_session) == get_type_hints(
        CopilotClient.create_session
    )


async def test_session_cwd_defaults_do_not_override_cloud_or_explicit_directories(monkeypatch):
    create_session = AsyncMock()
    monkeypatch.setattr(CopilotClient, "create_session", create_session)
    client = context._TestClient(
        "/fixture", connection=RuntimeConnection.for_stdio(path="unused-runtime")
    )
    await client.create_session()
    create_session.assert_awaited_once_with(working_directory="/fixture")
    create_session.reset_mock()
    await client.create_session(working_directory="/explicit")
    create_session.assert_awaited_once_with(working_directory="/explicit")
    create_session.reset_mock()
    cloud = CloudSessionOptions()
    await client.create_session(cloud=cloud)
    create_session.assert_awaited_once_with(cloud=cloud)
    create_session.reset_mock()
    subprocess_client = context._TestClient(
        None, connection=RuntimeConnection.for_stdio(path="unused-runtime")
    )
    await subprocess_client.create_session()
    create_session.assert_awaited_once_with()


class TestGetCliPathForTests:
    def test_env_var_takes_precedence(self, tmp_path, monkeypatch):
        cli = tmp_path / "custom-cli.js"
        cli.write_text("// custom entrypoint\n")
        monkeypatch.setenv("COPILOT_CLI_PATH", str(cli))
        assert context.get_cli_path_for_tests() == str(cli.resolve())

    def test_prepares_the_checkout_runtime(self, tmp_path, monkeypatch):
        monkeypatch.delenv("COPILOT_CLI_PATH", raising=False)
        cli = tmp_path / "copilot"
        cli.write_text("runtime\n")

        class Result:
            returncode = 0
            stdout = f"{cli}\n"
            stderr = ""

        monkeypatch.setattr(context.subprocess, "run", lambda *args, **kwargs: Result())
        assert context.get_cli_path_for_tests() == str(cli.resolve())

    def test_preparation_failure_includes_command_error(self, tmp_path, monkeypatch):
        class Result:
            returncode = 1
            stdout = ""
            stderr = "same-checkout artifact missing"

        monkeypatch.setattr(context.subprocess, "run", lambda *args, **kwargs: Result())
        with pytest.raises(RuntimeError) as excinfo:
            context._prepare_checkout_cli(tmp_path)
        assert "same-checkout artifact missing" in str(excinfo.value)

    def test_rejects_missing_selected_cli_without_falling_back(self, tmp_path, monkeypatch):
        monkeypatch.setenv("COPILOT_CLI_PATH", str(tmp_path / "missing-cli"))
        monkeypatch.setattr(
            context,
            "_prepare_checkout_cli",
            lambda *_args: pytest.fail("fallback preparation must not run"),
        )

        with pytest.raises(RuntimeError, match="COPILOT_CLI_PATH"):
            context.get_cli_path_for_tests()


def test_inprocess_environment_reuses_prepared_runtime_without_mutating_host(tmp_path, monkeypatch):
    cli = tmp_path / "copilot-runtime"
    cli.write_text("runtime\n")

    test_context = context.E2ETestContext()
    test_context.cli_path = str(cli)
    test_context.home_dir = str(tmp_path / "home")
    test_context.work_dir = str(tmp_path)
    monkeypatch.delenv("COPILOT_CLI_PATH", raising=False)
    monkeypatch.setenv("COPILOT_HMAC_KEY", "ambient-hmac")
    original_cwd = context.os.getcwd()

    environment = test_context.get_env()
    assert environment["COPILOT_CLI_PATH"] == str(cli)
    assert environment["COPILOT_HOME"] == test_context.home_dir
    assert environment["COPILOT_HMAC_KEY"] == ""
    assert "COPILOT_CLI_PATH" not in context.os.environ
    assert context.os.environ["COPILOT_HMAC_KEY"] == "ambient-hmac"
    assert context.os.getcwd() == original_cwd


@pytest.mark.parametrize("skip_download", ["1", "true", "yes"])
def test_inprocess_collection_preserves_offline_runtime_override(
    tmp_path, monkeypatch, skip_download
):
    cli = tmp_path / "copilot-runtime"
    cli.write_text("runtime\n")
    runtime = tmp_path / "runtime.node"
    runtime.write_text("native runtime\n")
    monkeypatch.setenv("COPILOT_CLI_PATH", str(cli))
    monkeypatch.setenv("COPILOT_SKIP_CLI_DOWNLOAD", skip_download)
    monkeypatch.setenv("COPILOT_HMAC_KEY", "secret")
    monkeypatch.setenv("CAPI_HMAC_KEY", "secret")

    assert context.get_cli_path_for_tests() == str(cli.resolve())
    client = CopilotClient(connection=RuntimeConnection.for_inprocess())
    assert client._inprocess_runtime_path == str(runtime)
    assert client._cli_path_source == "environment"
    assert context.os.environ["COPILOT_HMAC_KEY"] == "secret"
    assert context.os.environ["CAPI_HMAC_KEY"] == "secret"


def test_inprocess_collection_rejects_incomplete_offline_runtime(tmp_path, monkeypatch):
    cli = tmp_path / "copilot-runtime"
    cli.write_text("runtime\n")
    monkeypatch.setenv("COPILOT_CLI_PATH", str(cli))
    monkeypatch.setenv("COPILOT_SKIP_CLI_DOWNLOAD", "1")

    with pytest.raises(RuntimeError, match="In-process runtime library not found next to"):
        CopilotClient(connection=RuntimeConnection.for_inprocess())


def test_inprocess_runtime_selection_uses_process_environment(tmp_path, monkeypatch):
    cli = tmp_path / "copilot-runtime"
    cli.write_text("runtime\n")
    runtime = tmp_path / "runtime.node"
    runtime.write_text("native runtime\n")
    monkeypatch.setenv("COPILOT_CLI_PATH", str(cli))

    client = CopilotClient(connection=RuntimeConnection.for_inprocess())
    assert client._inprocess_runtime_path == str(runtime)
    assert context.os.environ["COPILOT_CLI_PATH"] == str(cli)
