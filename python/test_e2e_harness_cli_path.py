"""Unit tests for the E2E harness's pinned CLI preparation."""

from __future__ import annotations

import pytest

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
