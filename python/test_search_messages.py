from unittest.mock import AsyncMock, Mock

import pytest

from copilot.session import CopilotSession


def event(event_type: str, data: dict) -> dict:
    return {
        "type": event_type,
        "id": "00000000-0000-4000-8000-000000000001",
        "parentId": None,
        "timestamp": "2026-08-23T00:00:00Z",
        "data": data,
    }


@pytest.fixture
def session() -> CopilotSession:
    client = Mock()
    client.request = AsyncMock(
        return_value={
            "events": [
                event("user.message", {"content": "Configure Authentication"}),
                event(
                    "session.error",
                    {"errorType": "notification", "message": "authentication failed"},
                ),
                event(
                    "assistant.message",
                    {
                        "content": "Authentication is configured",
                        "messageId": "message-1",
                    },
                ),
                event(
                    "assistant.message",
                    {"content": "Deployment complete", "messageId": "message-2"},
                ),
            ]
        }
    )
    return CopilotSession("session-1", client)


@pytest.mark.asyncio
async def test_searches_only_message_content_case_insensitively(session: CopilotSession):
    results = await session.search_messages("authentication")

    assert [event.data.content for event in results] == [
        "Configure Authentication",
        "Authentication is configured",
    ]
    session._client.request.assert_awaited_once_with(
        "session.getMessages", {"sessionId": "session-1"}
    )


@pytest.mark.asyncio
async def test_filters_by_message_type_and_case(session: CopilotSession):
    results = await session.search_messages(
        "Authentication", event_type="assistant.message", case_sensitive=True
    )

    assert [event.data.content for event in results] == ["Authentication is configured"]


@pytest.mark.asyncio
async def test_supports_compiled_regular_expressions(session: CopilotSession):
    import re

    results = await session.search_messages(re.compile(r"auth\w+", re.IGNORECASE))

    assert len(results) == 2
