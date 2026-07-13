"""Tests for the in-process runtime library download integrity checks."""

from __future__ import annotations

import base64
import hashlib
from unittest.mock import patch

import pytest

from copilot import _cli_download


def _integrity(data: bytes, algo: str = "sha512") -> str:
    digest = hashlib.new(algo, data).digest()
    return f"{algo}-{base64.b64encode(digest).decode('ascii')}"


class TestVerifyIntegrity:
    def test_accepts_matching_checksum(self):
        data = b"native-library-bytes"
        _cli_download._verify_integrity(data, _integrity(data))

    def test_rejects_mismatched_checksum(self):
        with pytest.raises(RuntimeError, match="Integrity mismatch"):
            _cli_download._verify_integrity(b"tampered", _integrity(b"original"))

    def test_rejects_unsupported_algorithm(self):
        # Fail closed rather than silently skipping verification of native code.
        with pytest.raises(RuntimeError, match="Unsupported integrity algorithm"):
            _cli_download._verify_integrity(b"bytes", "md5-deadbeef")


class TestEnsureRuntimeLibraryFailsClosed:
    def test_raises_when_integrity_unavailable(self, tmp_path):
        """A missing npm integrity value must abort the download, not load unverified code."""
        cli_path = tmp_path / "copilot"
        cli_path.write_bytes(b"#!/bin/sh\n")

        with (
            patch("copilot._ffi_runtime_host.resolve_library_path", return_value=None),
            patch.object(_cli_download, "_should_skip_download", return_value=False),
            patch.object(_cli_download, "get_npm_platform", return_value="linux-x64"),
            patch.object(_cli_download, "get_runtime_lib_url", return_value="https://example/lib"),
            patch.object(_cli_download, "_fetch_url_bytes", return_value=b"tarball-bytes"),
            patch.object(_cli_download, "_fetch_runtime_integrity", return_value=None),
            patch.object(_cli_download, "_extract_runtime_node") as extract,
        ):
            with pytest.raises(RuntimeError, match="refusing to load unverified native code"):
                _cli_download.ensure_runtime_library(str(cli_path), version="1.2.3")

        # The library bytes must never be extracted/written when verification is impossible.
        extract.assert_not_called()
