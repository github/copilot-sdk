from __future__ import annotations

import asyncio
from typing import Any, cast

import pytest

from copilot import (
    CopilotClient,
    GitHubTokenAcquireReason,
    RuntimeConnection,
)
from copilot._jsonrpc import JsonRpcClient, JsonRpcError
from copilot.rpc import GitHubTokenAcquireRequest


class FakeJsonRpcClient:
    def __init__(self, *, fail_method: str | None = None) -> None:
        self.fail_method = fail_method
        self.requests: list[tuple[str, dict[str, Any]]] = []
        self.request_handlers: dict[str, Any] = {}
        self.notification_method_handlers: dict[str, Any] = {}

    async def request(self, method: str, params: dict[str, Any], **kwargs: Any) -> dict[str, Any]:
        self.requests.append((method, params))
        if method == self.fail_method:
            raise RuntimeError(f"{method} failed")
        if method in {"session.create", "session.resume"}:
            response = {"sessionId": params["sessionId"]}
            callback = kwargs.get("on_response_inline")
            if callback is not None:
                callback(response)
            return response
        if method == "session.destroy":
            return {}
        raise RuntimeError(f"Unexpected method: {method}")

    async def stop(self) -> None:
        pass

    def set_request_handler(self, method: str, handler: Any) -> None:
        self.request_handlers[method] = handler

    def set_notification_method_handler(self, method: str, handler: Any) -> None:
        self.notification_method_handlers[method] = handler


class ConcurrentResumeJsonRpcClient(FakeJsonRpcClient):
    def __init__(self) -> None:
        super().__init__()
        self.resume_responses: list[asyncio.Future[dict[str, Any]]] = []

    async def request(self, method: str, params: dict[str, Any], **kwargs: Any) -> dict[str, Any]:
        self.requests.append((method, params))
        if method == "session.create":
            response = {"sessionId": params["sessionId"]}
            callback = kwargs.get("on_response_inline")
            if callback is not None:
                callback(response)
            return response
        if method == "session.resume":
            response = asyncio.get_running_loop().create_future()
            self.resume_responses.append(response)
            return await response
        raise RuntimeError(f"Unexpected method: {method}")


def make_client(fake: FakeJsonRpcClient) -> CopilotClient:
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
    client._client = cast(JsonRpcClient, fake)
    return client


class TestGitHubTokenProvider:
    async def test_mutual_exclusion(self) -> None:
        client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))

        with pytest.raises(
            ValueError, match="github_token and github_token_provider are mutually exclusive"
        ):
            await client.create_session(
                github_token="static",
                github_token_provider=lambda _: {"kind": "cancelled"},
            )

    async def test_wire_mapping_token_and_cancelled(self) -> None:
        fake = FakeJsonRpcClient()
        client = make_client(fake)
        observed: list[dict[str, Any]] = []

        async def provider(args):
            observed.append(dict(args))
            if len(observed) == 1:
                return {
                    "kind": "token",
                    "accessToken": "secret-token",
                    "tokenType": "Bearer",
                    "expiresIn": 28_800,
                }
            return {"kind": "cancelled"}

        await client.create_session(
            session_id="python-session",
            github_token_provider=provider,
        )
        create_payload = fake.requests[0][1]
        registration_id = create_payload["gitHubTokenProviderRegistrationId"]
        assert "github_token_provider" not in create_payload
        assert "gitHubToken" not in create_payload
        client._register_client_global_handlers()
        get_token = fake.request_handlers["gitHubToken.getToken"]

        token = await get_token(
            {
                "registrationId": registration_id,
                "host": "github.example.com",
                "reason": "initial",
            }
        )
        cancelled = await get_token(
            {
                "registrationId": registration_id,
                "host": "github.example.com",
                "reason": "refresh",
                "sessionId": "python-session",
            }
        )

        assert token == {
            "kind": "token",
            "accessToken": "secret-token",
            "tokenType": "Bearer",
            "expiresIn": 28_800,
        }
        assert cancelled == {"kind": "cancelled"}
        assert observed == [
            {
                "host": "github.example.com",
                "session_id": "python-session",
                "reason": GitHubTokenAcquireReason.INITIAL,
            },
            {
                "host": "github.example.com",
                "session_id": "python-session",
                "reason": GitHubTokenAcquireReason.REFRESH,
            },
        ]

    async def test_callback_and_unknown_registration_errors(self) -> None:
        fake = FakeJsonRpcClient()
        client = make_client(fake)
        failure = RuntimeError("credential broker failed")

        def provider(_args):
            raise failure

        await client.create_session(
            session_id="error-session",
            github_token_provider=provider,
        )
        registration_id = fake.requests[0][1]["gitHubTokenProviderRegistrationId"]

        with pytest.raises(RuntimeError, match="credential broker failed") as exc:
            await client._github_token_provider_adapter.get_token(
                GitHubTokenAcquireRequest(
                    registration_id=registration_id,
                    host="github.com",
                    reason=GitHubTokenAcquireReason.INITIAL,
                )
            )
        assert exc.value is failure

        with pytest.raises(JsonRpcError, match="No GitHub token provider registered"):
            await client._github_token_provider_adapter.get_token(
                GitHubTokenAcquireRequest(
                    registration_id="unknown",
                    host="github.com",
                    reason=GitHubTokenAcquireReason.REFRESH,
                )
            )

    async def test_failure_session_close_and_client_close_cleanup(self) -> None:
        failing = make_client(FakeJsonRpcClient(fail_method="session.create"))
        with pytest.raises(RuntimeError, match="session.create failed"):
            await failing.create_session(github_token_provider=lambda _: {"kind": "cancelled"})
        assert failing._github_token_providers == {}

        fake = FakeJsonRpcClient()
        client = make_client(fake)
        first = await client.create_session(
            session_id="first",
            github_token_provider=lambda _: {"kind": "cancelled"},
        )
        await client.create_session(
            session_id="second",
            github_token_provider=lambda _: {"kind": "cancelled"},
        )
        assert len(client._github_token_providers) == 2

        await first.disconnect()
        assert len(client._github_token_providers) == 1
        await client.force_stop()
        assert client._github_token_providers == {}

    async def test_resume_rotates_provider(self) -> None:
        fake = FakeJsonRpcClient()
        client = make_client(fake)
        calls: list[str] = []

        await client.create_session(
            session_id="resumed",
            github_token_provider=lambda _: calls.append("first") or {"kind": "cancelled"},
        )
        first_registration = fake.requests[0][1]["gitHubTokenProviderRegistrationId"]
        await client.resume_session(
            "resumed",
            github_token_provider=lambda _: calls.append("second") or {"kind": "cancelled"},
        )
        second_registration = fake.requests[1][1]["gitHubTokenProviderRegistrationId"]

        with pytest.raises(JsonRpcError):
            await client._github_token_provider_adapter.get_token(
                GitHubTokenAcquireRequest(
                    registration_id=first_registration,
                    host="github.com",
                    reason=GitHubTokenAcquireReason.REFRESH,
                )
            )
        assert await client._github_token_provider_adapter.get_token(
            GitHubTokenAcquireRequest(
                registration_id=second_registration,
                host="github.com",
                reason=GitHubTokenAcquireReason.REFRESH,
            )
        ) == {"kind": "cancelled"}
        assert calls == ["second"]

    async def test_concurrent_resume_keeps_pending_registration(self) -> None:
        fake = ConcurrentResumeJsonRpcClient()
        client = make_client(fake)
        await client.create_session(
            session_id="concurrent",
            github_token_provider=lambda _: {"kind": "cancelled"},
        )

        first_resume = asyncio.create_task(
            client.resume_session(
                "concurrent",
                github_token_provider=lambda _: {"kind": "cancelled"},
            )
        )
        second_resume = asyncio.create_task(
            client.resume_session(
                "concurrent",
                github_token_provider=lambda _: {"kind": "cancelled"},
            )
        )
        while len(fake.resume_responses) < 2:
            await asyncio.sleep(0)

        fake.resume_responses[0].set_result({"sessionId": "concurrent"})
        await first_resume
        assert len(client._github_token_providers) == 2

        fake.resume_responses[1].set_result({"sessionId": "concurrent"})
        await second_resume
        assert len(client._github_token_providers) == 1
        second_registration = fake.requests[2][1]["gitHubTokenProviderRegistrationId"]
        assert second_registration in client._github_token_providers
