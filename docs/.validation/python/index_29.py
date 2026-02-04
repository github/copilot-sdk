# Source: auth/index.md:195
import asyncio

async def main():
    from copilot import CopilotClient
    
    # Token is read from environment variable automatically
    client = CopilotClient()
    await client.start()

asyncio.run(main())