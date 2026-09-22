"""Unit tests for the E2E harness's pinned CLI preparation."""

from __future__ import annotations

import pytest

from copilot import CopilotClient, RuntimeConnection
from e2e import conftest
from e2e.testharness import context


class TestGetCliPathForTests:
    def test_env_var_takes_precedence(self, tmp_path, monkeypatch):
        cli = tmp_path / "custom-cli.js"
        cli.write_text("// custom entrypoint\n")
        monkeypatch.setenv("COPILOT_CLI_PATH", str(cli))
        assert context.get_cli_path_for_tests() == str(cli.resolve())

    def test_prepares_the_pinned_runtime(self, tmp_path, monkeypatch):
        monkeypatch.delenv("COPILOT_CLI_PATH", raising=False)
        cli = tmp_path / "copilot"
        cli.write_text("runtime\n")

        class Result:
            returncode = 0
            stdout = f"{cli}\n"
            stderr = ""

        monkeypatch.setattr(context.subprocess, "run", lambda *args, **kwargs: Result())
        assert context._prepare_pinned_cli(tmp_path) == str(cli.resolve())

    def test_preparation_failure_includes_command_error(self, tmp_path, monkeypatch):
        class Result:
            returncode = 1
            stdout = ""
            stderr = "download failed"

        monkeypatch.setattr(context.subprocess, "run", lambda *args, **kwargs: Result())
        with pytest.raises(RuntimeError) as excinfo:
            context._prepare_pinned_cli(tmp_path)
        assert "download failed" in str(excinfo.value)

    def test_runtime_checkout_requires_selected_cli_without_preparing_published(self, monkeypatch):
        monkeypatch.delenv("COPILOT_CLI_PATH", raising=False)
        monkeypatch.setenv("COPILOT_RUNTIME_SOURCE", "checkout")
        monkeypatch.setattr(
            context,
            "_prepare_pinned_cli",
            lambda *_args: pytest.fail("published preparation must not run"),
        )

        with pytest.raises(RuntimeError, match="COPILOT_CLI_PATH"):
            context.get_cli_path_for_tests()


def test_inprocess_environment_reuses_prepared_runtime(tmp_path, monkeypatch):
    cli = tmp_path / "copilot-runtime"
    cli.write_text("runtime\n")

    test_context = context.E2ETestContext()
    test_context.cli_path = str(cli)
    test_context.work_dir = str(tmp_path)
    monkeypatch.setattr(test_context, "get_env", lambda: {"HTTPS_PROXY": "https://proxy"})
    monkeypatch.delenv("COPILOT_CLI_PATH", raising=False)

    try:
        test_context._apply_inprocess_environment()
        assert context.os.environ["COPILOT_CLI_PATH"] == str(cli)
    finally:
        test_context._restore_inprocess_environment()


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

    conftest._neutralize_inprocess_environment()

    assert context.get_cli_path_for_tests() == str(cli.resolve())
    client = CopilotClient(connection=RuntimeConnection.for_inprocess())
    assert client._inprocess_runtime_path == str(runtime)
    assert client._cli_path_source == "environment"
    assert "COPILOT_HMAC_KEY" not in context.os.environ
    assert "CAPI_HMAC_KEY" not in context.os.environ


def test_inprocess_collection_rejects_incomplete_offline_runtime(tmp_path, monkeypatch):
    cli = tmp_path / "copilot-runtime"
    cli.write_text("runtime\n")
    monkeypatch.setenv("COPILOT_CLI_PATH", str(cli))
    monkeypatch.setenv("COPILOT_SKIP_CLI_DOWNLOAD", "1")

    conftest._neutralize_inprocess_environment()

    with pytest.raises(RuntimeError, match="In-process runtime library not found next to"):
        CopilotClient(connection=RuntimeConnection.for_inprocess())


def test_inprocess_collection_keeps_public_runtime_selection_behavior(tmp_path, monkeypatch):
    cli = tmp_path / "copilot-runtime"
    cli.write_text("runtime\n")
    monkeypatch.setenv("COPILOT_CLI_PATH", str(cli))
    monkeypatch.delenv("COPILOT_SKIP_CLI_DOWNLOAD", raising=False)

    conftest._neutralize_inprocess_environment()

    assert "COPILOT_CLI_PATH" not in context.os.environ
