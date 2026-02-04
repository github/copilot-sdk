# Source: auth/index.md:40
import asyncio

async def main():
    from copilot import CopilotClient
    
    # Default: uses logged-in user credentials
    client = CopilotClient()
    await client.start()

asyncio.run(main())