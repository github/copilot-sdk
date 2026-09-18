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
async def test_session_connectors_exposes_complete_runtime_api():
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
    assert (
        await session.rpc.connectors.connect(request)
    ).kind is ConnectorConnectResultKind.CONSENT_REQUIRED
    assert (
        await session.rpc.connectors.reconnect(request)
    ).kind is ConnectorConnectResultKind.PENDING
    assert (
        await session.rpc.connectors.continue_connection(
            ConnectorContinueRequest(
                continuation_id="continuation-1",
                max_attempts=3,
                poll_interval_ms=100,
                deadline_ms=1_000,
            )
        )
    ).kind is ConnectorConnectResultKind.PENDING
    assert (await session.rpc.connectors.disconnect(request)).disconnected
    assert (
        await session.rpc.connectors.reconcile(
            ConnectorReconcileRequest(account_id="account-1", refresh_catalog=True)
        )
    ).api_version == 1

    assert [call.args[0] for call in transport.request.await_args_list] == [
        "session.connectors.getCapabilities",
        "session.connectors.getStatus",
        "session.connectors.list",
        "session.connectors.refresh",
        "session.connectors.connect",
        "session.connectors.reconnect",
        "session.connectors.continueConnection",
        "session.connectors.disconnect",
        "session.connectors.reconcile",
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
