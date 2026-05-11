"""
HTTP client for the Mission Control API.

Provides :class:`MissionControlClient` for creating cloud tasks, polling
task events, and steering tasks through the Mission Control REST API.
"""

from __future__ import annotations

import asyncio
import json
import logging
import urllib.error
import urllib.request
from typing import Any

from .types import (
    CloudSessionEvent,
    CloudSessionFailureReason,
    MissionControlTask,
)

logger = logging.getLogger(__name__)

CLOUD_SANDBOX_AGENT_SLUG = "copilot-developer-sandbox"

_DEFAULT_REQUEST_TIMEOUT_S = 10
_DEFAULT_CREATE_CLOUD_TASK_TIMEOUT_S = 10 * 60


class CloudSessionError(Exception):
    """Error from a Mission Control API request.

    Attributes:
        reason: Categorised failure reason.
        status: HTTP status code, if available.
    """

    def __init__(
        self,
        message: str,
        reason: CloudSessionFailureReason,
        status: int | None = None,
    ) -> None:
        super().__init__(message)
        self.reason = reason
        self.status = status


class _CreateCloudTaskRepository:
    """Repository reference sent in a create-task request body."""

    __slots__ = ("owner", "name")

    def __init__(self, owner: str, name: str) -> None:
        self.owner = owner
        self.name = name


class _CreateCloudTaskParams:
    """Parameters for creating a cloud task."""

    __slots__ = ("owner", "repository")

    def __init__(
        self,
        owner: str | None = None,
        repository: _CreateCloudTaskRepository | None = None,
    ) -> None:
        self.owner = owner
        self.repository = repository


class MissionControlClient:
    """HTTP client for the Mission Control task API.

    Args:
        base_url: Base URL for the Mission Control API (e.g. ``https://api.githubcopilot.com/agents``).
        auth_token: Bearer token for authentication.
        integration_id: Copilot integration identifier.
        frontend_base_url: Base URL for task frontend links.
        request_timeout_s: Timeout for normal requests in seconds.
        create_cloud_task_timeout_s: Timeout for task creation in seconds.
    """

    def __init__(
        self,
        *,
        base_url: str,
        auth_token: str | None = None,
        integration_id: str | None = None,
        frontend_base_url: str,
        request_timeout_s: float | None = None,
        create_cloud_task_timeout_s: float | None = None,
    ) -> None:
        self._base_url = base_url.rstrip("/")
        self._auth_token = auth_token.strip() if auth_token and auth_token.strip() else None
        self._integration_id = integration_id or "copilot-cli"
        self._frontend_base_url = frontend_base_url.rstrip("/")
        self._request_timeout_s = request_timeout_s or _DEFAULT_REQUEST_TIMEOUT_S
        self._create_cloud_task_timeout_s = (
            create_cloud_task_timeout_s or _DEFAULT_CREATE_CLOUD_TASK_TIMEOUT_S
        )

    async def create_cloud_task(
        self, params: _CreateCloudTaskParams | None = None
    ) -> MissionControlTask:
        """Create a new cloud sandbox task."""
        if params is None:
            params = _CreateCloudTaskParams()

        body: dict[str, Any] = {}
        if params.owner:
            body["owner"] = params.owner
        if params.repository:
            body["repositories"] = [
                {"owner": params.repository.owner, "name": params.repository.name}
            ]

        data = await self._request_json(
            f"{self._base_url}/tasks",
            method="POST",
            headers=self._headers({"X-Copilot-Agent-Slug": CLOUD_SANDBOX_AGENT_SLUG}),
            body=json.dumps(body),
            timeout=self._create_cloud_task_timeout_s,
        )
        return MissionControlTask.from_dict(data)

    async def list_task_events(self, task_id: str) -> list[CloudSessionEvent]:
        """Poll task events from Mission Control."""
        encoded_id = urllib.request.quote(task_id, safe="")
        data = await self._request_json(
            f"{self._base_url}/tasks/{encoded_id}/events",
            method="GET",
            headers=self._headers(),
            timeout=self._request_timeout_s,
        )

        events_raw = data.get("events") if isinstance(data, dict) else None
        if not isinstance(events_raw, list):
            raise CloudSessionError(
                f"Unexpected Mission Control events response for task {task_id}",
                "server",
            )

        return [CloudSessionEvent.from_dict(e) for e in events_raw if _is_cloud_session_event(e)]

    async def steer_task(
        self,
        task_id: str,
        request: dict[str, Any],
    ) -> None:
        """Send a steering command to a running task."""
        encoded_id = urllib.request.quote(task_id, safe="")
        await self._request_ok(
            f"{self._base_url}/tasks/{encoded_id}/steer",
            method="POST",
            headers=self._headers(),
            body=json.dumps(request),
            timeout=self._request_timeout_s,
        )

    async def get_task(self, task_id: str) -> MissionControlTask | None:
        """Get task metadata. Returns ``None`` if the task is not found."""
        encoded_id = urllib.request.quote(task_id, safe="")
        try:
            data = await self._request_json(
                f"{self._base_url}/tasks/{encoded_id}",
                method="GET",
                headers=self._headers(),
                timeout=self._request_timeout_s,
            )
            return MissionControlTask.from_dict(data)
        except CloudSessionError as exc:
            if exc.status == 404:
                return None
            raise

    def get_frontend_url(self, task_id: str) -> str:
        """Build the frontend URL for a task."""
        encoded_id = urllib.request.quote(task_id, safe="")
        return f"{self._frontend_base_url}/copilot/tasks/{encoded_id}"

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    def _headers(self, extra: dict[str, str] | None = None) -> dict[str, str]:
        headers: dict[str, str] = {
            "Content-Type": "application/json",
            "Copilot-Integration-Id": self._integration_id,
        }
        if self._auth_token:
            headers["Authorization"] = f"Bearer {self._auth_token}"
        if extra:
            headers.update(extra)
        return headers

    async def _request_json(
        self,
        url: str,
        *,
        method: str,
        headers: dict[str, str],
        body: str | None = None,
        timeout: float,
    ) -> Any:
        response_body = await self._request_ok(
            url, method=method, headers=headers, body=body, timeout=timeout
        )
        if not response_body:
            return None
        try:
            return json.loads(response_body)
        except json.JSONDecodeError as exc:
            raise CloudSessionError(
                f"Mission Control returned invalid JSON: {exc}",
                "server",
            ) from exc

    async def _request_ok(
        self,
        url: str,
        *,
        method: str,
        headers: dict[str, str],
        body: str | None = None,
        timeout: float,
    ) -> str:
        """Perform an HTTP request and return the response body text.

        Raises :class:`CloudSessionError` on non-2xx responses, timeouts,
        and network errors.
        """
        try:
            return await asyncio.wait_for(
                self._do_request(url, method=method, headers=headers, body=body),
                timeout=timeout,
            )
        except TimeoutError as exc:
            raise CloudSessionError(
                "Mission Control request timed out",
                "timeout",
            ) from exc
        except CloudSessionError:
            raise
        except Exception as exc:
            raise CloudSessionError(
                f"Mission Control request failed: {exc}",
                "network",
            ) from exc

    @staticmethod
    async def _do_request(
        url: str,
        *,
        method: str,
        headers: dict[str, str],
        body: str | None = None,
    ) -> str:
        """Execute the HTTP request in a thread to avoid blocking the event loop."""
        loop = asyncio.get_running_loop()

        def _sync_request() -> str:
            data = body.encode("utf-8") if body else None
            req = urllib.request.Request(url, data=data, headers=headers, method=method)
            try:
                with urllib.request.urlopen(req) as resp:
                    return resp.read().decode("utf-8")
            except urllib.error.HTTPError as http_err:
                error_body = ""
                try:
                    error_body = http_err.read().decode("utf-8")
                except Exception:
                    pass
                message = _extract_mission_control_message(error_body) or (
                    f"Mission Control request failed with HTTP {http_err.code}"
                )
                raise CloudSessionError(
                    message,
                    _reason_for_status(http_err.code),
                    http_err.code,
                ) from http_err

        return await loop.run_in_executor(None, _sync_request)


# ------------------------------------------------------------------
# Module-level helpers
# ------------------------------------------------------------------


def _reason_for_status(status: int) -> CloudSessionFailureReason:
    if status == 403:
        return "policy_blocked"
    if status in (400, 422):
        return "validation"
    return "server"


def _extract_mission_control_message(text: str) -> str | None:
    if not text:
        return None
    try:
        parsed = json.loads(text)
        if isinstance(parsed, dict):
            msg = parsed.get("message")
            if isinstance(msg, str) and msg:
                return msg
    except json.JSONDecodeError:
        pass
    return text


def _is_cloud_session_event(value: Any) -> bool:
    if not isinstance(value, dict):
        return False
    return (
        isinstance(value.get("id"), str)
        and isinstance(value.get("timestamp"), str)
        and isinstance(value.get("type"), str)
    )
