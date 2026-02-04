# Source: hooks/post-tool-use.md:105
import asyncio

async def main():
    async def on_post_tool_use(input_data, invocation):
        print(f"[{invocation['session_id']}] Tool: {input_data['toolName']}")
        print(f"  Args: {input_data['toolArgs']}")
        print(f"  Result: {input_data['toolResult']}")
        return None  # Pass through unchanged
    
    session = await client.create_session({
        "hooks": {"on_post_tool_use": on_post_tool_use}
    })

asyncio.run(main())