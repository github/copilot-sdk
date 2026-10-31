# Copyright (c) Microsoft Corporation. All rights reserved.

import base64
from types import SimpleNamespace
from unittest.mock import AsyncMock, Mock

import pytest

from copilot import SessionFsByteRange, SessionFsReadFileRangeProvider
from copilot.session_fs_provider import SessionFsProvider, create_session_fs_adapter


@pytest.mark.parametrize("offset,length", [(2, 2), (4, None), (3, 0), (7, 20), (9, None), (100, 2)])
async def test_ranges_preserve_utf8_byte_positions_without_binary(offset, length):
    data = "a☃🚀z".encode()
    expected = data[offset : None if length is None else offset + length]
    provider = Mock(spec=SessionFsReadFileRangeProvider)
    provider.read_file_range = AsyncMock(return_value=SessionFsByteRange(expected, len(data)))
    result = await create_session_fs_adapter(provider).read_file_range(
        SimpleNamespace(path="/file", offset=offset, length=length)
    )
    assert result.error is None
    assert result.byte_length == len(data)
    assert base64.b64decode(result.content) == expected
    provider.read_file_range.assert_awaited_once_with("/file", offset, length)


@pytest.mark.parametrize("position", [-1, 0.5, True, 9_007_199_254_740_992])
async def test_invalid_ranges_never_invoke_provider(position):
    provider = Mock(spec=SessionFsReadFileRangeProvider)
    provider.read_file_range = AsyncMock()
    adapter = create_session_fs_adapter(provider)
    for offset, length in [(position, 1), (0, position)]:
        result = await adapter.read_file_range(
            SimpleNamespace(path="/file", offset=offset, length=length)
        )
        assert result.error is not None
        assert result.error.code.value == "UNKNOWN"
    provider.read_file_range.assert_not_awaited()


@pytest.mark.parametrize(
    "content,total", [(b"x", 5), (b"xxx", 5), (b"xx", -1), (b"xx", 9_007_199_254_740_992)]
)
async def test_invalid_provider_byte_length_is_an_error(content, total):
    provider = Mock(spec=SessionFsReadFileRangeProvider)
    provider.read_file_range = AsyncMock(return_value=SessionFsByteRange(content, total))
    result = await create_session_fs_adapter(provider).read_file_range(
        SimpleNamespace(path="/file", offset=1, length=2)
    )
    assert result.error is not None


async def test_legacy_provider_does_not_fallback_and_filesystem_errors_are_preserved():
    legacy = Mock(spec=SessionFsProvider)
    result = await create_session_fs_adapter(legacy).read_file_range(
        SimpleNamespace(path="/file", offset=0)
    )
    assert result.error is not None
    legacy.read_file.assert_not_called()
    provider = Mock(spec=SessionFsReadFileRangeProvider)
    provider.read_file_range = AsyncMock(side_effect=FileNotFoundError(2, "missing"))
    result = await create_session_fs_adapter(provider).read_file_range(
        SimpleNamespace(path="/file", offset=0)
    )
    assert result.error.code.value == "ENOENT"
