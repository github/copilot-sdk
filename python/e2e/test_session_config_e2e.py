"""E2E tests for session configuration including model capabilities overrides."""

import base64
import json
import os
import uuid

import httpx
import pytest

from copilot import (
    CopilotClient,
    CopilotRequestHandler,
    ModelCapabilitiesOverride,
    ModelSupportsOverride,
    RuntimeConnection,
)
from copilot.copilot_request_handler import CopilotRequestContext
from copilot.session import PermissionHandler

from ._copilot_request_helpers import (
    build_inference_response,
    build_non_inference_response,
    is_inference_url,
)
from .testharness import DEFAULT_GITHUB_TOKEN, E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")

PROVIDER_HEADER_NAME = "x-copilot-sdk-provider-header"
CLIENT_NAME = "python-public-surface-client"


def has_image_url_content(exchanges: list[dict]) -> bool:
    """Check if any exchange contains an image_url content part in user messages."""
    for ex in exchanges:
        for msg in ex.get("request", {}).get("messages", []):
            if msg.get("role") == "user" and isinstance(msg.get("content"), list):
                if any(p.get("type") == "image_url" for p in msg["content"]):
                    return True
    return False


def _make_proxy_provider(proxy_url: str, header_value: str) -> dict:
    return {
        "type": "openai",
        "base_url": proxy_url,
        "api_key": "test-provider-key",
        "headers": {PROVIDER_HEADER_NAME: header_value},
    }


def _normalize_headers(headers) -> dict[str, str]:
    if isinstance(headers, list):
        flat: dict[str, str] = {}
        for entry in headers:
            if isinstance(entry, dict):
                key = entry.get("name") or entry.get("key")
                value = entry.get("value")
                if key is not None:
                    flat[str(key).lower()] = str(value)
        return flat
    if isinstance(headers, dict):
        flat = {}
        for key, value in headers.items():
            if isinstance(value, list):
                flat[str(key).lower()] = ", ".join(str(v) for v in value)
            else:
                flat[str(key).lower()] = str(value)
        return flat
    return {}


def _assert_header_contains(headers, name: str, expected: str) -> None:
    flat = _normalize_headers(headers)
    actual = flat.get(name.lower(), "")
    assert expected in actual, (
        f"Expected header {name!r} to contain {expected!r}; got {actual!r}. All headers: {flat!r}"
    )


def _get_system_message(exchange: dict) -> str:
    for msg in exchange.get("request", {}).get("messages", []):
        if msg.get("role") == "system":
            value = msg.get("content")
            if isinstance(value, str):
                return value
    return ""


def _get_tool_names(exchange: dict) -> list[str]:
    tools = exchange.get("request", {}).get("tools") or []
    names: list[str] = []
    for tool in tools:
        function = tool.get("function") if isinstance(tool, dict) else None
        if isinstance(function, dict):
            name = function.get("name")
            if isinstance(name, str):
                names.append(name)
    return names


async def _send_and_get_next_exchange(session, ctx: E2ETestContext, prompt: str) -> dict:
    existing_count = len(await ctx.get_exchanges())
    await session.send_and_wait(prompt)
    exchanges = await ctx.get_exchanges()
    assert len(exchanges) > existing_count
    return exchanges[existing_count]


def _assert_session_limits_status(exchange: dict, expected_remaining: str) -> None:
    for message in exchange.get("request", {}).get("messages", []):
        content = message.get("content")
        if message.get("role") == "user" and isinstance(content, str):
            if "<session_limits_status>" in content:
                assert f"Remaining session limits: {expected_remaining}." in content
                assert (
                    "Be frugal; avoid optional exploration and unnecessary tool calls." in content
                )
                return
    raise AssertionError("Expected session limits status message")


def _get_task_agent_types(exchange: dict) -> list[str]:
    for tool in exchange.get("request", {}).get("tools", []) or []:
        function = tool.get("function") if isinstance(tool, dict) else None
        if isinstance(function, dict) and function.get("name") == "task":
            parameters = function.get("parameters")
            assert isinstance(parameters, dict)
            values = parameters["properties"]["agent_type"]["enum"]
            assert isinstance(values, list)
            return [str(value) for value in values]
    raise AssertionError("Expected task tool in request")


class _RecordingRequestHandler(CopilotRequestHandler):
    def __init__(self):
        self.records: list[tuple[str, bytes]] = []

    async def send_request(
        self, request: httpx.Request, ctx: CopilotRequestContext
    ) -> httpx.Response:
        del ctx
        self.records.append((str(request.url), request.content))
        if is_inference_url(str(request.url)):
            return build_inference_response(request)
        return build_non_inference_response(str(request.url))

    def inference_requests(self) -> list[tuple[str, bytes]]:
        return [(url, body) for url, body in self.records if is_inference_url(url)]


def _create_pdf_attachment() -> dict:
    pdf_text = (
        "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF\n"
    )
    return {
        "type": "blob",
        "data": base64.b64encode(pdf_text.encode("ascii")).decode("ascii"),
        "displayName": "citation-source.pdf",
        "mimeType": "application/pdf",
    }


def _create_anthropic_provider() -> dict:
    return {
        "type": "anthropic",
        "base_url": "https://anthropic-citations.invalid/v1",
        "api_key": "test-provider-key",
        "model_id": "claude-sonnet-4.5",
        "wire_model": "claude-sonnet-4.5",
    }


def _assert_anthropic_document_citations_enabled(request_body: bytes) -> None:
    body = json.loads(request_body.decode("utf-8"))
    document_blocks = [
        block
        for message in body["messages"]
        for block in message["content"]
        if block.get("type") == "document"
    ]
    assert len(document_blocks) == 1
    assert document_blocks[0]["title"] == "citation-source.pdf"
    assert document_blocks[0]["citations"] == {"enabled": True}


PNG_1X1 = base64.b64decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
)
VIEW_IMAGE_PROMPT = "Use the view tool to look at the file test.png and describe what you see"


class TestSessionConfig:
    """Tests for session configuration including model capabilities overrides."""

    async def test_vision_disabled_then_enabled_via_setmodel(self, ctx: E2ETestContext):
        png_path = os.path.join(ctx.work_dir, "test.png")
        with open(png_path, "wb") as f:
            f.write(PNG_1X1)

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            model_capabilities=ModelCapabilitiesOverride(
                supports=ModelSupportsOverride(vision=False)
            ),
        )

        # Turn 1: vision off — no image_url expected
        await session.send_and_wait(VIEW_IMAGE_PROMPT)
        traffic_after_t1 = await ctx.get_exchanges()
        assert not has_image_url_content(traffic_after_t1)

        # Switch vision on
        await session.set_model(
            "claude-sonnet-4.5",
            model_capabilities=ModelCapabilitiesOverride(
                supports=ModelSupportsOverride(vision=True)
            ),
        )

        # Turn 2: vision on — image_url expected in new exchanges
        await session.send_and_wait(VIEW_IMAGE_PROMPT)
        traffic_after_t2 = await ctx.get_exchanges()
        new_exchanges = traffic_after_t2[len(traffic_after_t1) :]
        assert has_image_url_content(new_exchanges)

        await session.disconnect()

    async def test_vision_enabled_then_disabled_via_setmodel(self, ctx: E2ETestContext):
        png_path = os.path.join(ctx.work_dir, "test.png")
        with open(png_path, "wb") as f:
            f.write(PNG_1X1)

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            model_capabilities=ModelCapabilitiesOverride(
                supports=ModelSupportsOverride(vision=True)
            ),
        )

        # Turn 1: vision on — image_url expected
        await session.send_and_wait(VIEW_IMAGE_PROMPT)
        traffic_after_t1 = await ctx.get_exchanges()
        assert has_image_url_content(traffic_after_t1)

        # Switch vision off
        await session.set_model(
            "claude-sonnet-4.5",
            model_capabilities=ModelCapabilitiesOverride(
                supports=ModelSupportsOverride(vision=False)
            ),
        )

        # Turn 2: vision off — no image_url expected in new exchanges
        await session.send_and_wait(VIEW_IMAGE_PROMPT)
        traffic_after_t2 = await ctx.get_exchanges()
        new_exchanges = traffic_after_t2[len(traffic_after_t1) :]
        assert not has_image_url_content(new_exchanges)

        await session.disconnect()

    async def test_should_use_custom_sessionid(self, ctx: E2ETestContext):
        from copilot.session_events import SessionStartData

        requested_session_id = str(uuid.uuid4())
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            session_id=requested_session_id,
        )
        assert session.session_id == requested_session_id

        messages = await session.get_events()
        assert messages
        start_event = messages[0]
        assert isinstance(start_event.data, SessionStartData)
        assert start_event.data.session_id == requested_session_id

        await session.disconnect()

    async def test_should_forward_clientname_in_useragent(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            client_name=CLIENT_NAME,
        )

        await session.send_and_wait("What is 1+1?")

        exchanges = await ctx.get_exchanges()
        assert exchanges
        _assert_header_contains(exchanges[-1].get("requestHeaders"), "user-agent", CLIENT_NAME)

        await session.disconnect()

    async def test_should_forward_custom_provider_headers_on_create(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            model="claude-sonnet-4.5",
            provider=_make_proxy_provider(ctx.proxy_url, "create-provider-header"),
        )

        message = await session.send_and_wait("What is 1+1?")
        assert "2" in (message.data.content or "")

        exchanges = await ctx.get_exchanges()
        assert exchanges
        headers = exchanges[-1].get("requestHeaders")
        _assert_header_contains(headers, "authorization", "Bearer test-provider-key")
        _assert_header_contains(headers, PROVIDER_HEADER_NAME, "create-provider-header")

        await session.disconnect()

    async def test_should_forward_custom_provider_headers_on_resume(self, ctx: E2ETestContext):
        session1 = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        session_id = session1.session_id

        session2 = await ctx.client.resume_session(
            session_id,
            on_permission_request=PermissionHandler.approve_all,
            model="claude-sonnet-4.5",
            provider=_make_proxy_provider(ctx.proxy_url, "resume-provider-header"),
        )

        message = await session2.send_and_wait("What is 2+2?")
        assert "4" in (message.data.content or "")

        exchanges = await ctx.get_exchanges()
        assert exchanges
        headers = exchanges[-1].get("requestHeaders")
        _assert_header_contains(headers, "authorization", "Bearer test-provider-key")
        _assert_header_contains(headers, PROVIDER_HEADER_NAME, "resume-provider-header")

        await session2.disconnect()
        await session1.disconnect()

    async def test_should_forward_provider_wire_model(self, ctx: E2ETestContext):
        # Verifies that ProviderConfig.wire_model overrides the model name sent
        # to the provider API, while SessionConfig.model still drives runtime
        # configuration lookup (capabilities, prompts, reasoning behavior).
        # max_output_tokens is also set here to confirm the SDK accepts it
        # without serialization errors; the CLI does not echo it as
        # `max_tokens` on the OpenAI-style wire request, so we don't assert on
        # it directly (see unit tests for serialization coverage).
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            model="claude-sonnet-4.5",
            provider={
                "type": "openai",
                "base_url": ctx.proxy_url,
                "api_key": "test-provider-key",
                "wire_model": "test-wire-model",
                "max_output_tokens": 1024,
            },
        )

        await session.send_and_wait("What is 1+1?")

        exchanges = await ctx.get_exchanges()
        assert len(exchanges) == 1
        request = exchanges[0]["request"]
        assert request["model"] == "test-wire-model"

        await session.disconnect()

    async def test_should_use_provider_model_id_as_wire_model(self, ctx: E2ETestContext):
        # ProviderConfig.model_id drives both the runtime resolved model AND the wire
        # model when wire_model is not specified. SessionConfig.model is intentionally
        # omitted so that model_id is the only model source.
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            provider={
                "type": "openai",
                "base_url": ctx.proxy_url,
                "api_key": "test-provider-key",
                "model_id": "claude-sonnet-4.5",
            },
        )

        await session.send_and_wait("What is 1+1?")

        exchanges = await ctx.get_exchanges()
        assert len(exchanges) == 1
        assert exchanges[0]["request"]["model"] == "claude-sonnet-4.5"

        await session.disconnect()

    async def test_should_apply_session_limits_on_create(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            session_limits={"max_ai_credits": 30},
        )

        exchange = await _send_and_get_next_exchange(
            session, ctx, "Acknowledge the current session limits."
        )
        _assert_session_limits_status(exchange, "30 AI credits")

        await session.disconnect()

    async def test_should_apply_session_limits_on_resume(self, ctx: E2ETestContext):
        session1 = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        session2 = await ctx.client.resume_session(
            session1.session_id,
            on_permission_request=PermissionHandler.approve_all,
            session_limits={"max_ai_credits": 30},
        )

        exchange = await _send_and_get_next_exchange(
            session2, ctx, "Acknowledge the current session limits."
        )
        _assert_session_limits_status(exchange, "30 AI credits")

        await session2.disconnect()
        await session1.disconnect()

    async def test_should_apply_excluded_built_in_agents_on_create(self, ctx: E2ETestContext):
        excluded_agent = "explore"
        prompt = "What is 1+1?"

        baseline_session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        baseline_exchange = await _send_and_get_next_exchange(baseline_session, ctx, prompt)
        assert excluded_agent in _get_task_agent_types(baseline_exchange)
        await baseline_session.disconnect()

        excluded_session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            excluded_builtin_agents=[excluded_agent],
        )
        excluded_exchange = await _send_and_get_next_exchange(excluded_session, ctx, prompt)
        agent_types = _get_task_agent_types(excluded_exchange)
        assert agent_types
        assert excluded_agent not in agent_types

        await excluded_session.disconnect()

    async def test_should_apply_excluded_built_in_agents_on_resume(self, ctx: E2ETestContext):
        excluded_agent = "explore"
        session1 = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        session2 = await ctx.client.resume_session(
            session1.session_id,
            on_permission_request=PermissionHandler.approve_all,
            excluded_builtin_agents=[excluded_agent],
        )

        exchange = await _send_and_get_next_exchange(session2, ctx, "What is 1+1?")
        agent_types = _get_task_agent_types(exchange)
        assert agent_types
        assert excluded_agent not in agent_types

        await session2.disconnect()
        await session1.disconnect()

    async def test_should_enable_citations_for_anthropic_file_attachments_on_create(
        self, ctx: E2ETestContext
    ):
        handler = _RecordingRequestHandler()
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=ctx.cli_path),
            working_directory=ctx.work_dir,
            env=ctx.get_env(),
            github_token=DEFAULT_GITHUB_TOKEN,
            request_handler=handler,
        )
        await client.start()
        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                model="claude-sonnet-4.5",
                enable_citations=True,
                provider=_create_anthropic_provider(),
            )
            try:
                await session.send_and_wait(
                    "Summarize the attached PDF with citations enabled.",
                    attachments=[_create_pdf_attachment()],
                )
                inference_requests = handler.inference_requests()
                assert len(inference_requests) == 1
                _assert_anthropic_document_citations_enabled(inference_requests[0][1])
            finally:
                await session.disconnect()
        finally:
            await client.stop()

    async def test_should_enable_citations_for_anthropic_file_attachments_on_resume(
        self, ctx: E2ETestContext
    ):
        handler = _RecordingRequestHandler()
        connection_token = "python-citation-resume-token"
        server_client = CopilotClient(
            connection=RuntimeConnection.for_tcp(
                path=ctx.cli_path,
                connection_token=connection_token,
            ),
            working_directory=ctx.work_dir,
            env=ctx.get_env(),
            github_token=DEFAULT_GITHUB_TOKEN,
            request_handler=handler,
        )
        await server_client.start()
        try:
            session1 = await server_client.create_session(
                on_permission_request=PermissionHandler.approve_all,
            )
            assert server_client.runtime_port is not None
            resume_client = CopilotClient(
                connection=RuntimeConnection.for_uri(
                    f"localhost:{server_client.runtime_port}",
                    connection_token=connection_token,
                )
            )
            try:
                session2 = await resume_client.resume_session(
                    session1.session_id,
                    on_permission_request=PermissionHandler.approve_all,
                    model="claude-sonnet-4.5",
                    enable_citations=True,
                    provider=_create_anthropic_provider(),
                )
                try:
                    await session2.send_and_wait(
                        "Summarize the attached PDF with citations enabled.",
                        attachments=[_create_pdf_attachment()],
                    )
                    inference_requests = handler.inference_requests()
                    assert len(inference_requests) == 1
                    _assert_anthropic_document_citations_enabled(inference_requests[0][1])
                finally:
                    await session2.disconnect()
            finally:
                await resume_client.stop()
                await session1.disconnect()
        finally:
            await server_client.stop()

    async def test_should_use_workingdirectory_for_tool_execution(self, ctx: E2ETestContext):
        sub_dir = os.path.join(ctx.work_dir, "subproject")
        os.makedirs(sub_dir, exist_ok=True)
        with open(os.path.join(sub_dir, "marker.txt"), "w", encoding="utf-8") as f:
            f.write("I am in the subdirectory")

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            working_directory=sub_dir,
        )

        message = await session.send_and_wait("Read the file marker.txt and tell me what it says")
        assert "subdirectory" in (message.data.content or "")

        await session.disconnect()

    async def test_should_apply_workingdirectory_on_session_resume(self, ctx: E2ETestContext):
        sub_dir = os.path.join(ctx.work_dir, "resume-subproject")
        os.makedirs(sub_dir, exist_ok=True)
        with open(os.path.join(sub_dir, "resume-marker.txt"), "w", encoding="utf-8") as f:
            f.write("I am in the resume working directory")

        session1 = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        session_id = session1.session_id

        session2 = await ctx.client.resume_session(
            session_id,
            on_permission_request=PermissionHandler.approve_all,
            working_directory=sub_dir,
        )

        message = await session2.send_and_wait(
            "Read the file resume-marker.txt and tell me what it says"
        )
        assert "resume working directory" in (message.data.content or "")

        await session2.disconnect()
        await session1.disconnect()

    async def test_should_apply_systemmessage_on_session_resume(self, ctx: E2ETestContext):
        session1 = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        session_id = session1.session_id

        resume_instruction = "End the response with RESUME_SYSTEM_MESSAGE_SENTINEL."
        session2 = await ctx.client.resume_session(
            session_id,
            on_permission_request=PermissionHandler.approve_all,
            system_message={"mode": "append", "content": resume_instruction},
        )

        message = await session2.send_and_wait("What is 1+1?")
        assert "RESUME_SYSTEM_MESSAGE_SENTINEL" in (message.data.content or "")

        exchanges = await ctx.get_exchanges()
        assert exchanges
        assert resume_instruction in _get_system_message(exchanges[-1])

        await session2.disconnect()
        await session1.disconnect()

    async def test_should_apply_instruction_directories_on_create(self, ctx: E2ETestContext):
        project_dir = os.path.join(ctx.work_dir, "instruction-create-project")
        instruction_dir = os.path.join(ctx.work_dir, "extra-create-instructions")
        instruction_files_dir = os.path.join(instruction_dir, ".github", "instructions")
        sentinel = "PY_CREATE_INSTRUCTION_DIRECTORIES_SENTINEL"
        os.makedirs(project_dir, exist_ok=True)
        os.makedirs(instruction_files_dir, exist_ok=True)
        with open(
            os.path.join(instruction_files_dir, "extra.instructions.md"), "w", encoding="utf-8"
        ) as f:
            f.write(f"Always include {sentinel}.")

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            working_directory=project_dir,
            instruction_directories=[instruction_dir],
        )

        await session.send_and_wait("What is 1+1?")

        exchanges = await ctx.get_exchanges()
        assert exchanges
        assert sentinel in _get_system_message(exchanges[-1])

        await session.disconnect()

    async def test_should_apply_instruction_directories_on_resume(self, ctx: E2ETestContext):
        project_dir = os.path.join(ctx.work_dir, "instruction-resume-project")
        instruction_dir = os.path.join(ctx.work_dir, "extra-resume-instructions")
        instruction_files_dir = os.path.join(instruction_dir, ".github", "instructions")
        sentinel = "PY_RESUME_INSTRUCTION_DIRECTORIES_SENTINEL"
        os.makedirs(project_dir, exist_ok=True)
        os.makedirs(instruction_files_dir, exist_ok=True)
        with open(
            os.path.join(instruction_files_dir, "extra.instructions.md"), "w", encoding="utf-8"
        ) as f:
            f.write(f"Always include {sentinel}.")

        session1 = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            working_directory=project_dir,
        )

        session2 = await ctx.client.resume_session(
            session1.session_id,
            on_permission_request=PermissionHandler.approve_all,
            working_directory=project_dir,
            instruction_directories=[instruction_dir],
        )

        await session2.send_and_wait("What is 1+1?")

        exchanges = await ctx.get_exchanges()
        assert exchanges
        assert sentinel in _get_system_message(exchanges[-1])

        await session2.disconnect()
        await session1.disconnect()

    async def test_should_apply_availabletools_on_session_resume(self, ctx: E2ETestContext):
        session1 = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        session_id = session1.session_id

        session2 = await ctx.client.resume_session(
            session_id,
            on_permission_request=PermissionHandler.approve_all,
            available_tools=["view"],
        )

        try:
            await session2.send("What is 1+1?")

            exchanges = await ctx.wait_for_exchanges()
            assert _get_tool_names(exchanges[-1]) == ["view"]
        finally:
            await session2.disconnect()
            await session1.disconnect()
