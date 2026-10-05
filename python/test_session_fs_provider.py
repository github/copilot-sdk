# --------------------------------------------------------------------------------------------
#  Copyright (c) Microsoft Corporation. All rights reserved.
# --------------------------------------------------------------------------------------------

"""Unit tests for the session filesystem provider adapter."""

from types import SimpleNamespace
from unittest.mock import AsyncMock, Mock

import pytest

from copilot import SessionFsWriteFailure
from copilot.session_fs_provider import SessionFsProvider, create_session_fs_adapter


@pytest.mark.parametrize(
    ("failure", "changed"),
    [(RuntimeError("rejected"), None), (SessionFsWriteFailure("disk full"), True)],
)
async def test_failed_write_reports_only_known_changes(failure: Exception, changed: bool | None):
    provider = Mock(spec=SessionFsProvider)
    provider.write_file = AsyncMock(side_effect=failure)
    adapter = create_session_fs_adapter(provider)

    error = await adapter.write_file(SimpleNamespace(path="/file", content="data", mode=None))

    assert error is not None
    assert error.message == str(failure)
    assert error.write_changed is changed
    assert error.to_dict().get("writeChanged") is changed


async def test_write_marker_does_not_leak_into_other_methods():
    provider = Mock(spec=SessionFsProvider)
    provider.read_file = AsyncMock(side_effect=SessionFsWriteFailure("read failed"))
    adapter = create_session_fs_adapter(provider)

    result = await adapter.read_file(SimpleNamespace(path="/file"))

    assert result.error is not None
    assert result.error.write_changed is None
