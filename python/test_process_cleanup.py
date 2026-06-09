"""
Process Cleanup Tests

Tests for ensuring child processes (like MCP servers) are properly terminated
when the client stops, especially on Windows.

Related to issue #1132: https://github.com/github/copilot-sdk/issues/1132
"""

import subprocess
import sys
from unittest.mock import MagicMock, patch

import pytest

from copilot.client import _kill_process_tree, _kill_process_tree_force


class TestKillProcessTree:
    """Test the _kill_process_tree helper function."""

    def test_kill_process_tree_with_none_process(self):
        """Should handle None process gracefully."""
        _kill_process_tree(None)  # Should not raise

    def test_kill_process_tree_with_no_pid(self):
        """Should handle process with no PID gracefully."""
        mock_process = MagicMock(spec=subprocess.Popen)
        mock_process.pid = None
        _kill_process_tree(mock_process)  # Should not raise

    @pytest.mark.skipif(sys.platform != "win32", reason="Windows-specific test")
    @patch("copilot.client.HAS_PSUTIL", True)
    def test_kill_process_tree_on_windows_with_psutil(self):
        """On Windows with psutil, should kill children recursively."""
        # We can't easily mock the import inside the function,
        # so this test verifies the function doesn't crash when psutil is available
        mock_process = MagicMock(spec=subprocess.Popen)
        mock_process.pid = 99999999  # Use a PID that doesn't exist

        # Should not raise even with non-existent PID
        _kill_process_tree(mock_process)

        # Verify terminate was called as fallback
        mock_process.terminate.assert_called_once()

    @pytest.mark.skipif(sys.platform != "win32", reason="Windows-specific test")
    @patch("copilot.client.HAS_PSUTIL", False)
    def test_kill_process_tree_on_windows_without_psutil(self):
        """On Windows without psutil, should fall back to simple terminate."""
        mock_process = MagicMock(spec=subprocess.Popen)
        mock_process.pid = 1234

        _kill_process_tree(mock_process)

        mock_process.terminate.assert_called_once()

    @pytest.mark.skipif(sys.platform == "win32", reason="Non-Windows test")
    @patch("copilot.client.HAS_PSUTIL", True)
    def test_kill_process_tree_on_unix(self):
        """On Unix-like systems, should use simple terminate (children die with parent)."""
        mock_process = MagicMock(spec=subprocess.Popen)
        mock_process.pid = 1234

        _kill_process_tree(mock_process)

        mock_process.terminate.assert_called_once()


class TestKillProcessTreeForce:
    """Test the _kill_process_tree_force helper function."""

    def test_kill_process_tree_force_with_none_process(self):
        """Should handle None process gracefully."""
        _kill_process_tree_force(None)  # Should not raise

    def test_kill_process_tree_force_with_no_pid(self):
        """Should handle process with no PID gracefully."""
        mock_process = MagicMock(spec=subprocess.Popen)
        mock_process.pid = None
        _kill_process_tree_force(mock_process)  # Should not raise

    @pytest.mark.skipif(sys.platform != "win32", reason="Windows-specific test")
    @patch("copilot.client.HAS_PSUTIL", True)
    def test_kill_process_tree_force_on_windows_with_psutil(self):
        """On Windows with psutil, should kill children immediately with no wait."""
        mock_process = MagicMock(spec=subprocess.Popen)
        mock_process.pid = 99999999  # Use a PID that doesn't exist

        # Should not raise even with non-existent PID
        _kill_process_tree_force(mock_process)

        # Verify kill was called as fallback (not terminate)
        mock_process.kill.assert_called_once()
        mock_process.terminate.assert_not_called()

    @pytest.mark.skipif(sys.platform != "win32", reason="Windows-specific test")
    @patch("copilot.client.HAS_PSUTIL", False)
    def test_kill_process_tree_force_on_windows_without_psutil(self):
        """On Windows without psutil, should fall back to simple kill."""
        mock_process = MagicMock(spec=subprocess.Popen)
        mock_process.pid = 1234

        _kill_process_tree_force(mock_process)

        mock_process.kill.assert_called_once()
        mock_process.terminate.assert_not_called()

    @pytest.mark.skipif(sys.platform == "win32", reason="Non-Windows test")
    @patch("copilot.client.HAS_PSUTIL", True)
    def test_kill_process_tree_force_on_unix(self):
        """On Unix-like systems, should use simple kill (children die with parent)."""
        mock_process = MagicMock(spec=subprocess.Popen)
        mock_process.pid = 1234

        _kill_process_tree_force(mock_process)

        mock_process.kill.assert_called_once()
        mock_process.terminate.assert_not_called()

    @patch("copilot.client.HAS_PSUTIL", False)
    def test_kill_process_tree_force_without_psutil(self):
        """Without psutil, should use kill() immediately."""
        mock_process = MagicMock(spec=subprocess.Popen)
        mock_process.pid = 1234

        _kill_process_tree_force(mock_process)

        mock_process.kill.assert_called_once()
        mock_process.terminate.assert_not_called()


# Note: Integration tests for actual MCP server cleanup would go in e2e/
# This file focuses on unit testing the helper function
