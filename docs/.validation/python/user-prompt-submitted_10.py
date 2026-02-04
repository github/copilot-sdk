# Source: hooks/user-prompt-submitted.md:101
import asyncio

async def main():
    async def on_user_prompt_submitted(input_data, invocation):
        print(f"[{invocation['session_id']}] User: {input_data['prompt']}")
        return None
    
    session = await client.create_session({
        "hooks": {"on_user_prompt_submitted": on_user_prompt_submitted}
    })

asyncio.run(main())