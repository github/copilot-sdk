"""Client-owned MCP tool search through Copilot SDK with OpenAI Responses.

Requires OPENAI_API_KEY. In a source checkout, also set COPILOT_CLI_PATH to the
matching CLI runtime. From python/, run:
    uv run --with 'mcp>=2,<3' python samples/copilot_sdk_client_mcp_tool_search.py

Uses the experimental session.tools.set RPC; verified with Copilot CLI 1.0.89-1.
"""

import asyncio
import json
import os
from tempfile import TemporaryDirectory

from mcp import Client as McpClient

from copilot import CopilotClient, CopilotRequestHandler, RuntimeConnection
from copilot.rpc import (
    HandlePendingToolCallRequest,
    MCPServerConfigDeferTools,
    PermissionDecisionReject,
    ProtocolExternalToolDefinition,
    ToolsSetRequest,
)
from copilot.session_events import ExternalToolRequestedData
from copilot.tools import Tool, ToolInvocation, ToolResult

MODEL = "gpt-5.6-sol"
MCP_LABEL = "openai_docs"
MCP_URL = "https://developers.openai.com/mcp"
SEARCH_SCHEMA = {
    "type": "object",
    "properties": {
        "paths": {"type": "array", "items": {"type": "string", "enum": [MCP_LABEL]}},
        "goal": {"type": "string"},
    },
    "required": ["paths", "goal"],
    "additionalProperties": False,
}


class WireAudit(CopilotRequestHandler):
    """Log only the Responses wire shape; never log headers or message content."""

    def __init__(self) -> None:
        self.rounds: list[dict] = []

    async def send_request(self, request, ctx):
        if request.url.path.endswith("/responses"):
            body = json.loads(await request.aread())
            inputs = body.get("input", [])
            search_outputs = [
                item
                for item in inputs
                if isinstance(item, dict) and item.get("type") == "tool_search_output"
            ]
            search_call_ids = {
                item.get("call_id")
                for item in inputs
                if isinstance(item, dict) and item.get("type") == "tool_search_call"
            }
            summary = {
                "round": len(self.rounds) + 1,
                "path": request.url.path,
                "wire_tools": [tool.get("type") for tool in body.get("tools", [])],
                "input_types": [item.get("type") for item in inputs if isinstance(item, dict)],
                "search_outputs": [
                    {
                        "call_id": item.get("call_id"),
                        "execution": item.get("execution"),
                        "matches_call": bool(item.get("call_id"))
                        and item.get("call_id") in search_call_ids,
                        "tools": [tool.get("name") for tool in item.get("tools", [])],
                    }
                    for item in search_outputs
                ],
            }
            response = await super().send_request(request, ctx)
            if 200 <= response.status_code < 300:
                self.rounds.append(summary)
                print("OpenAI wire:", json.dumps(summary))
            return response
        return await super().send_request(request, ctx)


async def main() -> None:
    session = None
    loaded_refs: list[str] = []
    mcp_names: dict[str, str] = {}
    served: list[str] = []
    pending: list[asyncio.Task] = []
    seen_requests: set[str] = set()
    native_mcp_calls: list[str] = []
    wire_audit = WireAudit()

    async def search_mcp(invocation: ToolInvocation) -> ToolResult:
        paths = invocation.arguments.get("paths")
        print("tool_search_tool paths:", paths)
        if paths != [MCP_LABEL]:
            raise ValueError("Tool search selected an unconfigured MCP server")
        if loaded_refs:
            return ToolResult(
                text_result_for_llm="Tools already loaded.", tool_references=loaded_refs
            )

        # The model asked for this server. Discover its live tools only now.
        async with McpClient(MCP_URL) as mcp:
            listed = await mcp.list_tools()
        definitions = [
            ProtocolExternalToolDefinition(
                name="tool_search_tool",
                description="Search a deferred MCP server for tools needed by this task.",
                parameters=SEARCH_SCHEMA,
                overrides_built_in_tool=True,
                skip_permission=True,
                defer=MCPServerConfigDeferTools.NEVER,
            )
        ]
        for tool in listed.tools:
            if tool.input_schema.get("type") != "object":
                continue
            alias = f"client_{tool.name}"
            mcp_names[alias] = tool.name
            definitions.append(
                ProtocolExternalToolDefinition(
                    name=alias,
                    description=tool.description or tool.name,
                    parameters=tool.input_schema,
                    skip_permission=True,
                    defer=MCPServerConfigDeferTools.AUTO,
                )
            )
        if not mcp_names:
            raise RuntimeError("The selected MCP server returned no usable tools")

        # This experimental RPC replaces this connection's external tool list.
        # Keep the search override in the replacement, then load the new names.
        assert session is not None
        await session.rpc.tools.set(ToolsSetRequest(tools=definitions))
        metadata = await session.rpc.tools.get_current_metadata()
        current_names = {tool.name for tool in metadata.tools or []}
        if not mcp_names.keys() <= current_names:
            raise RuntimeError("New tools did not appear in the live Copilot catalog")
        loaded_refs.extend(mcp_names)
        print("Client-injected tool references:", loaded_refs)
        return ToolResult(
            text_result_for_llm="Loaded OpenAI Docs tools.", tool_references=loaded_refs
        )

    async def serve_tool(data: ExternalToolRequestedData) -> None:
        assert session is not None
        try:
            mcp_name = mcp_names[data.tool_name]
            arguments = data.arguments or {}
            if isinstance(arguments, str):
                arguments = json.loads(arguments)
            async with McpClient(MCP_URL) as mcp:
                result = await mcp.call_tool(mcp_name, arguments)
            output = "\n".join(part.text for part in result.content if part.type == "text")
            if result.is_error or not output:
                raise RuntimeError(output or f"MCP tool {mcp_name} returned no text")
        except Exception as exc:
            await session.rpc.tools.handle_pending_tool_call(
                HandlePendingToolCallRequest(request_id=data.request_id, error=str(exc))
            )
            raise
        await session.rpc.tools.handle_pending_tool_call(
            HandlePendingToolCallRequest(request_id=data.request_id, result=output)
        )
        served.append(mcp_name)
        print("Executed through MCP:", mcp_name)

    search_tool = Tool(
        name="tool_search_tool",
        description="Search a deferred MCP server for tools needed by this task.",
        parameters=SEARCH_SCHEMA,
        handler=search_mcp,
        overrides_built_in_tool=True,
        skip_permission=True,
        defer="never",
    )
    cli_path = os.environ.get("COPILOT_CLI_PATH")
    connection = RuntimeConnection.for_stdio(path=cli_path) if cli_path else None
    with TemporaryDirectory(prefix="copilot-tool-search-") as home:
        async with CopilotClient(
            connection=connection,
            base_directory=home,
            use_logged_in_user=False,
            mode="empty",
            request_handler=wire_audit,
        ) as client:
            session = await client.create_session(
                model=MODEL,
                provider={
                    "type": "openai",
                    "base_url": "https://api.openai.com/v1",
                    "wire_api": "responses",
                    "api_key": os.environ["OPENAI_API_KEY"],
                },
                mcp_servers={MCP_LABEL: {"type": "http", "url": MCP_URL, "tools": ["*"]}},
                tools=[search_tool],
                tool_search={"enabled": True, "defer_threshold": 1},
                available_tools=["tool_search_tool", "mcp:*", "custom:*"],
                on_permission_request=lambda _request, _invocation: PermissionDecisionReject(),
            )

            def on_event(event) -> None:
                if isinstance(event.data, ExternalToolRequestedData):
                    if (
                        event.data.tool_name in mcp_names
                        and event.data.request_id not in seen_requests
                    ):
                        seen_requests.add(event.data.request_id)
                        pending.append(asyncio.create_task(serve_tool(event.data)))
                elif event.type.value == "tool.execution_start":
                    name = event.data.tool_name
                    if name.startswith(f"{MCP_LABEL}-"):
                        native_mcp_calls.append(name)
                    print("Copilot tool:", name)

            session.on(on_event)
            try:
                reply = await session.send_and_wait(
                    "Use tool search to find the openai_docs MCP tools. Then search the OpenAI "
                    "documentation for client-executed tool search and give one finding.",
                    timeout=120.0,
                )
                if pending:
                    await asyncio.gather(*pending)
                if not loaded_refs or not served:
                    raise RuntimeError(
                        "The client search and MCP execution sequence did not complete"
                    )
                if native_mcp_calls:
                    raise RuntimeError(f"CLI-managed MCP tools executed: {native_mcp_calls}")
                if not any(round_["search_outputs"] for round_ in wire_audit.rounds):
                    raise RuntimeError("No native tool_search_output reached OpenAI Responses")
                if not all(
                    output["matches_call"] and output["execution"] == "client"
                    for round_ in wire_audit.rounds
                    for output in round_["search_outputs"]
                ):
                    raise RuntimeError("A tool_search_output did not match its client call")
                if reply is None or not reply.data.content:
                    raise RuntimeError("Copilot did not return a final answer")
                print("Final answer:", reply.data.content)
            finally:
                for task in pending:
                    if not task.done():
                        task.cancel()
                if pending:
                    await asyncio.gather(*pending, return_exceptions=True)
                await session.disconnect()


if __name__ == "__main__":
    asyncio.run(main())
