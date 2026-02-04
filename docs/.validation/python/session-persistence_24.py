# Source: guides/session-persistence.md:48
import asyncio

async def main():
    from copilot import CopilotClient
    
    client = CopilotClient()
    await client.start()
    
    # Create a session with a meaningful ID
    session = await client.create_session({
        "session_id": "user-123-task-456",
        "model": "gpt-5.2-codex",
    })
    
    # Do some work...
    await session.send_and_wait({"prompt": "Analyze my codebase"})
    
    # Session state is automatically persisted

asyncio.run(main())