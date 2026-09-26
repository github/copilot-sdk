from unittest.mock import AsyncMock, Mock

import pytest

from copilot.generated.rpc import (
    ConnectorAccountRequest,
    ConnectorAvailability,
    ConnectorConnectRequest,
    ConnectorConnectResultKind,
    ConnectorContinueRequest,
    ConnectorReconcileRequest,
)
from copilot.session import CopilotSession


@pytest.mark.asyncio
async def test_session_connectors_exposes_host_lifecycle_api():
    transport = Mock()
    transport.request = AsyncMock(side_effect=connector_response)
    session = CopilotSession("session-1", transport)

    capabilities = await session.rpc.connectors.get_capabilities()
    assert capabilities.availability is ConnectorAvailability.ENABLED
    assert (await session.rpc.connectors.get_status()).api_version == 1
    assert (
        await session.rpc.connectors.list(ConnectorAccountRequest(account_id="account-1"))
    ).revision == 1
    assert (
        await session.rpc.connectors.refresh(ConnectorAccountRequest(account_id="account-1"))
    ).revision == 1

    request = ConnectorConnectRequest(account_id="account-1", connector_name="calendar")
    consent = await session.rpc.connectors.connect(request)
    assert consent.kind is ConnectorConnectResultKind.CONSENT_REQUIRED
    assert consent.consent_url == "https://example.com/consent"
    assert consent.continuation_id == "continuation-1"

    reconnect = await session.rpc.connectors.reconnect(request)
    assert reconnect.kind is ConnectorConnectResultKind.PENDING
    assert reconnect.continuation_id == "continuation-1"

    continued = await session.rpc.connectors.continue_connection(
        ConnectorContinueRequest(
            continuation_id="continuation-1",
            max_attempts=3,
            poll_interval_ms=100,
            deadline_ms=1_000,
        )
    )
    assert continued.kind is ConnectorConnectResultKind.PENDING
    assert continued.continuation_id == "continuation-1"

    assert (await session.rpc.connectors.disconnect(request)).disconnected
    assert (
        await session.rpc.connectors.reconcile(
            ConnectorReconcileRequest(account_id="account-1", refresh_catalog=True)
        )
    ).api_version == 1

    assert [(call.args[0], call.args[1]) for call in transport.request.await_args_list] == [
        ("session.connectors.getCapabilities", {"sessionId": "session-1"}),
        ("session.connectors.getStatus", {"sessionId": "session-1"}),
        (
            "session.connectors.list",
            {"accountId": "account-1", "sessionId": "session-1"},
        ),
        (
            "session.connectors.refresh",
            {"accountId": "account-1", "sessionId": "session-1"},
        ),
        (
            "session.connectors.connect",
            {
                "accountId": "account-1",
                "connectorName": "calendar",
                "sessionId": "session-1",
            },
        ),
        (
            "session.connectors.reconnect",
            {
                "accountId": "account-1",
                "connectorName": "calendar",
                "sessionId": "session-1",
            },
        ),
        (
            "session.connectors.continueConnection",
            {
                "continuationId": "continuation-1",
                "deadlineMs": 1_000,
                "maxAttempts": 3,
                "pollIntervalMs": 100,
                "sessionId": "session-1",
            },
        ),
        (
            "session.connectors.disconnect",
            {
                "accountId": "account-1",
                "connectorName": "calendar",
                "sessionId": "session-1",
            },
        ),
        (
            "session.connectors.reconcile",
            {
                "accountId": "account-1",
                "refreshCatalog": True,
                "sessionId": "session-1",
            },
        ),
    ]


def connector_response(method, _params, **_kwargs):
    if method == "session.connectors.getCapabilities":
        return {
            "apiVersion": 1,
            "availability": "enabled",
            "consentContinuation": True,
            "opaqueAccountSelection": True,
            "maxPollAttempts": 30,
            "maxPollIntervalMs": 2_000,
            "maxDeadlineMs": 60_000,
        }
    if method in ("session.connectors.list", "session.connectors.refresh"):
        return {"revision": 1, "refreshedAtMs": 1, "connectors": []}
    if method == "session.connectors.connect":
        return {
            "kind": "consent_required",
            "consentUrl": "https://example.com/consent",
            "continuationId": "continuation-1",
        }
    if method in ("session.connectors.reconnect", "session.connectors.continueConnection"):
        return {"kind": "pending", "continuationId": "continuation-1"}
    if method == "session.connectors.disconnect":
        return {"disconnected": True, "status": connector_status()}
    return connector_status()


def connector_status():
    return {
        "apiVersion": 1,
        "availability": "enabled",
        "runtimeServers": [],
        "pendingConnections": 0,
    }
