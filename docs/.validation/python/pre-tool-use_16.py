# Source: hooks/pre-tool-use.md:113
import asyncio

async def main():
    async def on_pre_tool_use(input_data, invocation):
        print(f"[{invocation['session_id']}] Calling {input_data['toolName']}")
        print(f"  Args: {input_data['toolArgs']}")
        return {"permissionDecision": "allow"}
    
    session = await client.create_session({
        "hooks": {"on_pre_tool_use": on_pre_tool_use}
    })

asyncio.run(main())