# Source: guides/session-persistence.md:135
import asyncio

async def main():
    # Resume from a different client instance (or after restart)
    session = await client.resume_session("user-123-task-456")
    
    # Continue where you left off
    await session.send_and_wait({"prompt": "What did we discuss earlier?"})

asyncio.run(main())