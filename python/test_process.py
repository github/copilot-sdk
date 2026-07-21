"""Unit tests for CLI process tree termination helpers."""

import signal
import sys
from unittest.mock import Mock, patch

import pytest

from copilot._process import popen_process_group_kwargs, terminate_owned_cli_process


class TestPopenProcessGroupKwargs:
    def test_returns_start_new_session_on_posix(self):
        with patch.object(sys, "platform", "linux"):
            assert popen_process_group_kwargs() == {"start_new_session": True}

    def test_returns_empty_on_windows(self):
        with patch.object(sys, "platform", "win32"):
            assert popen_process_group_kwargs() == {}


class TestTerminateOwnedCliProcess:
    def test_noop_for_none_process(self):
        assert terminate_owned_cli_process(None) is True

    def test_returns_true_for_exited_process(self):
        process = Mock()
        process.poll.return_value = 0
        assert terminate_owned_cli_process(process) is True

    @patch("copilot._process.time.sleep", return_value=None)
    @patch("copilot._process.os.killpg")
    @patch("copilot._process.os.getpgid", return_value=42)
    def test_posix_graceful_uses_killpg_sigterm(self, _getpgid, killpg, _sleep):
        process = Mock()
        process.poll.side_effect = [None, 0]
        process.pid = 99

        with patch.object(sys, "platform", "linux"):
            assert terminate_owned_cli_process(process, graceful=True, timeout=1.0) is True

        killpg.assert_called_once_with(42, signal.SIGTERM)

    @patch("copilot._process.subprocess.run")
    def test_windows_uses_taskkill_tree(self, run):
        process = Mock()
        process.poll.side_effect = [None, 0]
        process.pid = 1234

        with patch.object(sys, "platform", "win32"):
            assert terminate_owned_cli_process(process, graceful=True, timeout=1.0) is True

        run.assert_called_once()
        assert run.call_args.args[0][:3] == ["taskkill", "/T", "/PID"]
