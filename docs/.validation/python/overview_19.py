# Source: hooks/overview.md:55
import asyncio

async def main():
    from copilot import CopilotClient
    
    async def main():
        client = CopilotClient()
        await client.start()
    
        async def on_pre_tool_use(input_data, invocation):
            print(f"Tool called: {input_data['toolName']}")
            return {"permissionDecision": "allow"}
    
        async def on_post_tool_use(input_data, invocation):
            print(f"Tool result: {input_data['toolResult']}")
            return None
    
        async def on_session_start(input_data, invocation):
            return {"additionalContext": "User prefers concise answers."}
    
        session = await client.create_session({
            "hooks": {
                "on_pre_tool_use": on_pre_tool_use,
                "on_post_tool_use": on_post_tool_use,
                "on_session_start": on_session_start,
            }
        })

asyncio.run(main())