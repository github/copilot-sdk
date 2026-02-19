import asyncio

from copilot import CopilotClient, PermissionHandler

BLUE = "\033[34m"
RESET = "\033[0m"


async def main():
    client = CopilotClient()
    await client.start()
    session = await client.create_session(
        {
            "on_permission_request": PermissionHandler.approve_all,
        }
    )

    def on_event(event):
        output = None
        if event.type.value == "assistant.reasoning":
            output = f"[reasoning: {event.data.content}]"
        elif event.type.value == "tool.execution_start":
            output = f"[tool: {event.data.tool_name}]"
        if output:
            print(f"{BLUE}{output}{RESET}")

    session.on(on_event)

    print("Chat with Copilot (Ctrl+C to exit)\n")

    while True:
        user_input = input("You: ").strip()
        if not user_input:
            continue
        print()

        reply = await session.send_and_wait({"prompt": user_input})
        print(f"\nAssistant: {reply.data.content if reply else None}\n")


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\nBye!")
