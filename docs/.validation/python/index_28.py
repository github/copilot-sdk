# Source: auth/index.md:108
import asyncio

async def main():
    from copilot import CopilotClient
    
    client = CopilotClient({
        "github_token": user_access_token,  # Token from OAuth flow
        "use_logged_in_user": False,        # Don't use stored CLI credentials
    })
    await client.start()

asyncio.run(main())