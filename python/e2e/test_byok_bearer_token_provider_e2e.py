# --------------------------------------------------------------------------------------------
#  Copyright (c) Microsoft Corporation. All rights reserved.
# --------------------------------------------------------------------------------------------

"""E2E coverage for the experimental BYOK bearer-token-provider surface.

Mirrors ``nodejs/test/e2e/byok_bearer_token_provider.e2e.test.ts``. A BYOK
provider config may carry a ``bearer_token_provider`` callback; the callback stays
entirely on the SDK/client side. The SDK strips it from the wire config, sets
the ``hasBearerTokenProvider`` flag, and the runtime calls back over the
session-scoped ``providerToken.getToken`` RPC before each outbound model
request, applying the returned token as the ``Authorization`` header.

Like the other ``copilot_request_*`` tests, this one installs a client-global
``CopilotRequestHandler`` instead of using the CAPI proxy: the handler
fabricates the bootstrap (catalog/policy) responses and intercepts the
runtime's outbound BYOK request in-process, capturing the ``Authorization``
header and returning a synthetic ``404``. It validates, against a real runtime:
 1. the callback's token reaches the model request as ``Authorization: Bearer <token>``;
 2. the runtime re-acquires a token per request (no runtime-side caching);
 3. per-provider dispatch routes each provider's turn to its own callback, and
    the resulting token reaches that provider's endpoint.
"""

from __future__ import annotations

import re

import httpx
import pytest
import pytest_asyncio

from copilot import CopilotRequestContext, CopilotRequestHandler
from copilot.session import BearerTokenProvider, PermissionHandler

from ._copilot_request_helpers import build_isolated_client, build_non_inference_response
from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")

# Fake BYOK provider base URLs. These hosts are never actually dialed: the
# client-global request interceptor fully answers any request aimed at a
# ``.invalid`` host, so they only need to be syntactically valid, non-resolving
# URLs. Distinct hosts let the per-provider test assert routing by host.
PRIMARY_HOST = "byok-endpoint.invalid"
PRIMARY_BASE_URL = f"https://{PRIMARY_HOST}/v1"
RED_HOST = "byok-red.invalid"
RED_BASE_URL = f"https://{RED_HOST}/v1"
BLUE_HOST = "byok-blue.invalid"
BLUE_BASE_URL = f"https://{BLUE_HOST}/v1"


class _CapturingRequestHandler(CopilotRequestHandler):
    """Client-global HTTP interceptor used in place of a real BYOK listener.

    The runtime invokes :meth:`send_request` for every model-layer HTTP request.
    Requests aimed at a fake BYOK host are captured — recording the
    ``Authorization`` header the runtime applied after calling the provider's
    ``bearer_token_provider`` callback over ``providerToken.getToken`` — and answered
    with a synthetic ``404`` (non-retryable, so each outbound model request
    yields exactly one capture). Every other request (CAPI bootstrap: model
    catalog, policy, …) is fabricated locally so no real network or CAPI proxy
    is involved.
    """

    def __init__(self) -> None:
        # (host, authorization) for each captured BYOK request, in arrival order.
        self.captures: list[tuple[str, str | None]] = []

    async def send_request(
        self, request: httpx.Request, ctx: CopilotRequestContext
    ) -> httpx.Response:
        url = httpx.URL(request.url)
        host = url.host
        if host.endswith(".invalid"):
            self.captures.append((host, request.headers.get("authorization")))
            return httpx.Response(
                404,
                headers={"content-type": "application/json"},
                json={"error": {"message": "fake byok endpoint"}},
                request=request,
            )
        return build_non_inference_response(str(request.url))

    def reset(self) -> None:
        self.captures.clear()

    def auth_headers(self) -> list[str]:
        """The ``Authorization`` headers captured across BYOK requests, in order."""
        return [auth for (_host, auth) in self.captures if auth is not None]

    def auth_header_for_host(self, host: str) -> str | None:
        """The ``Authorization`` header captured for requests aimed at ``host``."""
        for captured_host, auth in self.captures:
            if captured_host == host:
                return auth
        return None


@pytest_asyncio.fixture(loop_scope="module")
async def bearer_fixture(ctx: E2ETestContext):
    handler = _CapturingRequestHandler()
    client = build_isolated_client(ctx, handler)
    await client.start()
    try:
        yield client, handler
    finally:
        try:
            await client.stop()
        except Exception:
            # Best-effort teardown during fixture cleanup.
            pass


async def _run_turn(client, providers, models, selection_id: str, prompt: str) -> None:
    """Drive one BYOK turn; the synthetic 404 errors the turn, which is expected."""
    session = await client.create_session(
        on_permission_request=PermissionHandler.approve_all,
        model=selection_id,
        providers=providers,
        models=models,
    )
    try:
        # The interceptor always 404s, so the turn errors after the runtime has
        # already sent the (token-bearing) request — which is all we assert on.
        try:
            await session.send_and_wait(prompt)
        except Exception:
            # The fake BYOK endpoint intentionally errors after capture.
            pass
    finally:
        try:
            await session.disconnect()
        except Exception:
            # ignore disconnect errors for the fake BYOK endpoint
            pass


class TestByokBearerTokenProvider:
    async def test_applies_the_callbacks_token_as_the_authorization_header(self, bearer_fixture):
        client, handler = bearer_fixture
        handler.reset()

        sentinel = "sentinel-bearer-token-abc123"
        calls = 0

        async def get_bearer_token(args) -> str:
            nonlocal calls
            calls += 1
            return sentinel

        providers = [
            {
                "name": "mi",
                "type": "openai",
                "wire_api": "completions",
                "base_url": PRIMARY_BASE_URL,
                "bearer_token_provider": get_bearer_token,
            }
        ]
        models = [{"id": "default", "provider": "mi", "wire_model": "byok-gpt-4o"}]

        await _run_turn(client, providers, models, "mi/default", "What is 5+5?")

        # The runtime acquired a token via the callback and applied it verbatim
        # as the bearer credential on the outbound model request.
        assert f"Bearer {sentinel}" in handler.auth_headers()
        assert calls >= 1

    async def test_reacquires_a_fresh_token_for_each_request(self, bearer_fixture):
        client, handler = bearer_fixture
        handler.reset()

        calls = 0

        async def get_bearer_token(args) -> str:
            nonlocal calls
            calls += 1
            # A distinct token per acquisition proves the runtime re-invokes the
            # callback per request rather than caching a previous token.
            return f"rotating-token-{calls}"

        providers = [
            {
                "name": "mi",
                "type": "openai",
                "wire_api": "completions",
                "base_url": PRIMARY_BASE_URL,
                "bearer_token_provider": get_bearer_token,
            }
        ]
        models = [{"id": "default", "provider": "mi", "wire_model": "byok-gpt-4o"}]

        await _run_turn(client, providers, models, "mi/default", "What is 1+1?")
        await _run_turn(client, providers, models, "mi/default", "What is 2+2?")

        # Each outbound request carries a freshly-acquired, distinct token.
        auths = handler.auth_headers()
        assert len(auths) >= 2
        assert re.match(r"^Bearer rotating-token-\d+$", auths[0])
        assert re.match(r"^Bearer rotating-token-\d+$", auths[1])
        assert auths[0] != auths[1]
        assert calls >= 2

    async def test_dispatches_token_acquisition_per_provider(self, bearer_fixture):
        client, handler = bearer_fixture
        handler.reset()

        token_by_provider = {"red": "token-for-red", "blue": "token-for-blue"}
        acquired_for: list[str] = []

        def make_callback(provider_name: str) -> BearerTokenProvider:
            async def callback(args) -> str:
                # The runtime forwards the requesting provider's name so the
                # client can dispatch to the right credential.
                assert args["provider_name"] == provider_name
                # The runtime also forwards the owning session id so a
                # client-level shared callback can resolve the session.
                assert isinstance(args["session_id"], str) and args["session_id"]
                acquired_for.append(provider_name)
                return token_by_provider[provider_name]

            return callback

        providers = [
            {
                "name": "red",
                "type": "openai",
                "wire_api": "completions",
                "base_url": RED_BASE_URL,
                "bearer_token_provider": make_callback("red"),
            },
            {
                "name": "blue",
                "type": "openai",
                "wire_api": "completions",
                "base_url": BLUE_BASE_URL,
                "bearer_token_provider": make_callback("blue"),
            },
        ]
        models = [
            {"id": "default", "provider": "red", "wire_model": "byok-gpt-4o"},
            {"id": "default", "provider": "blue", "wire_model": "byok-gpt-4o"},
        ]

        await _run_turn(client, providers, models, "red/default", "What is 3+3?")
        await _run_turn(client, providers, models, "blue/default", "What is 4+4?")

        # Each provider's turn was authenticated with its own token AND that
        # token was delivered to that provider's endpoint, proving per-provider
        # dispatch (not a single session-global credential).
        assert handler.auth_header_for_host(RED_HOST) == f"Bearer {token_by_provider['red']}"
        assert handler.auth_header_for_host(BLUE_HOST) == f"Bearer {token_by_provider['blue']}"
        assert "red" in acquired_for
        assert "blue" in acquired_for
