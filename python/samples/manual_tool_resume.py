import asyncio
from typing import TypeVar

from copilot import CopilotClient, Tool
from copilot.generated.rpc import HandlePendingToolCallRequest, PermissionDecisionRequest
from copilot.generated.session_events import (
    AssistantMessageData,
    ExternalToolRequestedData,
    PermissionRequestedData,
    SessionEvent,
)

T = TypeVar("T")


def watch_event(session, data_type: type[T], predicate=None) -> asyncio.Future:
    loop = asyncio.get_running_loop()
    future = loop.create_future()

    def on_event(event):
        if isinstance(event.data, data_type) and (predicate is None or predicate(event.data)):
            unsubscribe()
            future.set_result(event)

    unsubscribe = session.on(on_event)
    return future


async def wait_for_event(future: asyncio.Future) -> SessionEvent:
    return await asyncio.wait_for(future, timeout=120)


async def pause():
    print("Simulating time passing...\n")
    await asyncio.sleep(1)


tool = Tool(
    name="manual_resume_status",
    description="Looks up a status value. The SDK consumer supplies the result manually.",
    parameters={
        "type": "object",
        "properties": {
            "id": {"type": "string", "description": "Identifier to look up"},
        },
        "required": ["id"],
    },
    # No handler: the SDK exposes the declaration and leaves execution pending.
)


async def main():
    # 1. Create a session with a declaration-only tool, then stop after the permission prompt.
    client1 = CopilotClient()
    await client1.start()
    session1 = await client1.create_session(tools=[tool])

    # Subscribe before sending so the permission event cannot be missed.
    permission_requested = watch_event(session1, PermissionRequestedData)
    await session1.send(
        "Use the manual_resume_status tool with id 'alpha', then tell me the status."
    )

    permission_event = await wait_for_event(permission_requested)
    await client1.force_stop()
    await pause()

    # 2. Resume pending work and grant permission to invoke the tool.
    client2 = CopilotClient()
    await client2.start()
    session2 = await client2.resume_session(
        session1.session_id,
        tools=[tool],
        continue_pending_work=True,
    )

    # Subscribe before approving so the external tool request cannot be missed.
    tool_requested = watch_event(
        session2,
        ExternalToolRequestedData,
        lambda data: data.tool_name == "manual_resume_status",
    )

    await session2.rpc.permissions.handle_pending_permission_request(
        PermissionDecisionRequest.from_dict(
            {
                "requestId": permission_event.data.request_id,
                "result": {"kind": "approve-once"},
            }
        )
    )

    tool_event = await wait_for_event(tool_requested)
    await client2.force_stop()
    await pause()

    # 3. Resume again and manually provide the pending tool result.
    client3 = CopilotClient()
    await client3.start()
    session3 = await client3.resume_session(
        session1.session_id,
        tools=[tool],
        continue_pending_work=True,
    )

    assistant_message = watch_event(session3, AssistantMessageData)
    await session3.rpc.tools.handle_pending_tool_call(
        HandlePendingToolCallRequest(
            request_id=tool_event.data.request_id,
            result="MANUAL_STATUS_READY",
        )
    )

    answer = await wait_for_event(assistant_message)
    print(answer.data.content)
    await client3.force_stop()


if __name__ == "__main__":
    asyncio.run(main())
