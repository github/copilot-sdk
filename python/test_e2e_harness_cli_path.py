"""Unit tests for the E2E harness's same-checkout CLI preparation."""

from __future__ import annotations

from typing import get_type_hints
from unittest.mock import AsyncMock, Mock

import pytest

from copilot import CopilotClient, RuntimeConnection, StdioRuntimeConnection
from copilot.client import CloudSessionOptions
from e2e import test_inprocess_ffi_e2e
from e2e.testharness import context


def test_fixture_client_preserves_session_option_annotations_for_ahp():
    assert get_type_hints(context._TestClient.create_session) == get_type_hints(
        CopilotClient.create_session
    )


async def test_session_cwd_defaults_do_not_override_cloud_or_explicit_directories(monkeypatch):
    create_session = AsyncMock()
    monkeypatch.setattr(CopilotClient, "create_session", create_session)
    client = context._TestClient(
        "/fixture",
        cli_path="unused-runtime",
        connection=RuntimeConnection.for_stdio(path="unused-runtime"),
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
        None,
        cli_path="unused-runtime",
        connection=RuntimeConnection.for_stdio(path="unused-runtime"),
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


@pytest.fixture
def prepared_runtime(tmp_path, monkeypatch):
    cli = tmp_path / "copilot-runtime"
    cli.write_text("runtime\n")
    (tmp_path / "runtime.node").write_text("native runtime\n")
    monkeypatch.setattr(context, "_prepare_checkout_cli", Mock(return_value=str(cli)))
    monkeypatch.setattr(
        "copilot._cli_download.ensure_runtime_wrapper",
        Mock(side_effect=AssertionError("managed runtime provisioner must not run")),
    )
    monkeypatch.setenv("COPILOT_SDK_DEFAULT_CONNECTION", "inprocess")
    monkeypatch.setenv("COPILOT_SKIP_CLI_DOWNLOAD", "1")
    monkeypatch.delenv("COPILOT_CLI_PATH", raising=False)

    proxy = Mock(spec=context.CapiProxy)
    proxy.start.return_value = "http://proxy.test"
    proxy.get_proxy_env.return_value = {}
    monkeypatch.setattr(context, "CapiProxy", Mock(return_value=proxy))
    return cli


@pytest.mark.parametrize("use_override", [False, True])
async def test_inprocess_setup_reuses_prepared_runtime_without_mutating_host(
    prepared_runtime, monkeypatch, use_override
):
    if use_override:
        monkeypatch.setenv("COPILOT_CLI_PATH", str(prepared_runtime))
        monkeypatch.setattr(
            context,
            "_prepare_checkout_cli",
            Mock(side_effect=AssertionError("override must bypass checkout preparation")),
        )
    monkeypatch.setenv("COPILOT_HMAC_KEY", "ambient-hmac")
    original_env = context.os.environ.copy()
    original_cwd = context.os.getcwd()

    test_context = context.E2ETestContext()
    try:
        await test_context.setup()
        assert test_context.cli_path == str(prepared_runtime)
        assert test_context.client._inprocess_runtime_path == str(
            prepared_runtime.with_name("runtime.node")
        )
        assert test_context.client._inprocess_cli_entrypoint == str(prepared_runtime)
        environment = test_context.get_env()
        assert environment["COPILOT_CLI_PATH"] == str(prepared_runtime)
        assert environment["COPILOT_HOME"] == test_context.home_dir
        assert environment["COPILOT_HMAC_KEY"] == ""
    finally:
        await test_context.teardown()
    assert context.os.environ == original_env
    assert context.os.getcwd() == original_cwd


async def test_inprocess_setup_rejects_missing_prepared_library(prepared_runtime):
    prepared_runtime.with_name("runtime.node").unlink()
    test_context = context.E2ETestContext()
    try:
        with pytest.raises(RuntimeError, match="In-process runtime library not found next to"):
            await test_context.setup()
    finally:
        await test_context.teardown()


async def test_standalone_ffi_smoke_uses_the_fixture_runtime(prepared_runtime, monkeypatch):
    create_host = Mock(side_effect=RuntimeError("native startup stopped"))
    monkeypatch.setattr("copilot.client.FfiRuntimeHost.create", create_host)
    test_context = context.E2ETestContext()
    try:
        await test_context.setup()
        with pytest.raises(RuntimeError, match="native startup stopped"):
            await test_context.client._start_inprocess_ffi()
        smoke_test = test_inprocess_ffi_e2e.TestInProcessFfi()
        with pytest.raises(RuntimeError, match="native startup stopped"):
            await smoke_test.test_should_start_and_connect_over_in_process_ffi(test_context, "stop")
        assert create_host.call_count == 2
        for call in create_host.call_args_list:
            assert call.args == (str(prepared_runtime.with_name("runtime.node")),)
            assert call.kwargs["cli_entrypoint"] == str(prepared_runtime)
    finally:
        await test_context.teardown()


@pytest.mark.parametrize(("use_stdio", "cli_args"), [(True, None), (False, ["--log-level=debug"])])
async def test_setup_preserves_subprocess_runtime_selection(prepared_runtime, use_stdio, cli_args):
    test_context = context.E2ETestContext(use_stdio=use_stdio)
    try:
        await test_context.setup(cli_args=cli_args)
        connection = test_context.client._connection
        assert isinstance(connection, StdioRuntimeConnection)
        assert connection.path == str(prepared_runtime)
        assert connection.args == tuple(cli_args or [])
        assert test_context.client._options.working_directory == test_context.work_dir
        assert test_context.client._inprocess_runtime_path is None
    finally:
        await test_context.teardown()


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
