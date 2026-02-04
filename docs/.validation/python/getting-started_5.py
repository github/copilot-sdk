# Source: getting-started.md:1210
import asyncio

async def main():
    from copilot import CopilotClient
    
    client = CopilotClient({
        "cli_url": "localhost:4321"
    })
    await client.start()
    
    # Use the client normally
    session = await client.create_session()
    # ...

asyncio.run(main())