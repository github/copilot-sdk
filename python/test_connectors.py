# Copyright (c) Microsoft Corporation. All rights reserved.

from unittest.mock import AsyncMock, Mock

import pytest

from copilot.generated.rpc import (
    AuthInfoType,
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
    assert capabilities.session_account_selection is None
    assert capabilities.targeted_reconcile is None
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


@pytest.mark.asyncio
async def test_unknown_connector_continuation_outcome_is_not_connected():
    transport = Mock()
    transport.request = AsyncMock(
        return_value={
            "kind": "future_outcome",
            "continuationId": "continuation-1",
            "status": connector_status(),
        }
    )
    session = CopilotSession("session-1", transport)

    with pytest.raises(ValueError, match="future_outcome"):
        await session.rpc.connectors.continue_connection(
            ConnectorContinueRequest(
                continuation_id="continuation-1",
                max_attempts=1,
                poll_interval_ms=0,
                deadline_ms=1_000,
            )
        )


@pytest.mark.asyncio
async def test_session_connector_account_and_targeted_reconcile_use_exact_wire_contract():
    account = {
        "accountId": "session-account-1",
        "authInfo": {"type": "token", "host": "github.com", "login": "alice"},
    }
    transport = Mock()
    transport.request = AsyncMock(
        side_effect=[
            {
                **connector_response("session.connectors.getCapabilities", {}),
                "sessionAccountSelection": True,
                "targetedReconcile": True,
            },
            account,
            None,
            connector_status(),
        ]
    )
    session = CopilotSession("session-1", transport)

    capabilities = await session.rpc.connectors.get_capabilities()
    assert capabilities.availability is ConnectorAvailability.ENABLED
    assert capabilities.session_account_selection is True
    assert capabilities.targeted_reconcile is True
    selected = await session.rpc.connectors.get_account()
    assert selected is not None
    assert selected.account_id == "session-account-1"
    assert selected.auth_info.type is AuthInfoType.TOKEN
    assert selected.to_dict() == account
    assert await session.rpc.connectors.get_account() is None
    await session.rpc.connectors.reconcile(
        ConnectorReconcileRequest(
            account_id=selected.account_id,
            refresh_catalog=True,
            force_connector_name="calendar",
        )
    )

    assert [(call.args[0], call.args[1]) for call in transport.request.await_args_list] == [
        ("session.connectors.getCapabilities", {"sessionId": "session-1"}),
        ("session.connectors.getAccount", {"sessionId": "session-1"}),
        ("session.connectors.getAccount", {"sessionId": "session-1"}),
        (
            "session.connectors.reconcile",
            {
                "sessionId": "session-1",
                "accountId": "session-account-1",
                "refreshCatalog": True,
                "forceConnectorName": "calendar",
            },
        ),
    ]


@pytest.mark.asyncio
@pytest.mark.parametrize("supported", [None, False])
async def test_hosts_can_avoid_unsupported_session_connector_calls(supported):
    capabilities = connector_response("session.connectors.getCapabilities", {})
    if supported is not None:
        capabilities.update(sessionAccountSelection=supported, targetedReconcile=supported)
    transport = Mock()
    transport.request = AsyncMock(return_value=capabilities)
    session = CopilotSession("session-1", transport)

    result = await session.rpc.connectors.get_capabilities()
    if result.availability is ConnectorAvailability.ENABLED:
        if result.session_account_selection is True:
            await session.rpc.connectors.get_account()
        if result.targeted_reconcile is True:
            await session.rpc.connectors.reconcile(
                ConnectorReconcileRequest(account_id="account-1", force_connector_name="calendar")
            )

    transport.request.assert_awaited_once_with(
        "session.connectors.getCapabilities", {"sessionId": "session-1"}
    )


@pytest.mark.asyncio
async def test_connector_catalog_presentation_metadata_is_optional():
    entry = {
        "name": "calendar",
        "displayName": "Calendar",
        "status": "connected",
        "runtimeServerIds": ["connector-calendar"],
    }
    decorated = {
        **entry,
        "logo": "https://example.com/calendar.svg",
        "tier": "standard",
        "releaseTag": "preview",
    }
    transport = Mock()
    transport.request = AsyncMock(
        side_effect=[
            {"connectors": [decorated], "revision": 1, "refreshedAtMs": 1},
            {"connectors": [entry], "revision": 2, "refreshedAtMs": 2},
        ]
    )
    session = CopilotSession("session-1", transport)
    catalog = await session.rpc.connectors.list(ConnectorAccountRequest(account_id="account-1"))
    assert catalog.connectors[0].to_dict() == decorated
    legacy = await session.rpc.connectors.refresh(ConnectorAccountRequest(account_id="account-1"))
    assert legacy.connectors[0].logo is None
    assert legacy.connectors[0].tier is None
    assert legacy.connectors[0].release_tag is None
    assert legacy.connectors[0].to_dict() == entry


def test_connector_reconcile_request_keeps_published_positional_order():
    request = ConnectorReconcileRequest("account-1", True)
    assert request.refresh_catalog is True
    assert request.force_connector_name is None
    assert request.to_dict() == {"accountId": "account-1", "refreshCatalog": True}
    targeted = ConnectorReconcileRequest("account-1", True, force_connector_name="mail")
    assert targeted.force_connector_name == "mail"
    with pytest.raises(TypeError):
        ConnectorReconcileRequest("account-1", True, "mail")


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
