# Source: hooks/error-handling.md:106
import asyncio

async def main():
    async def on_error_occurred(input_data, invocation):
        print(f"[{invocation['session_id']}] Error: {input_data['error']}")
        print(f"  Context: {input_data['errorContext']}")
        print(f"  Recoverable: {input_data['recoverable']}")
        return None
    
    session = await client.create_session({
        "hooks": {"on_error_occurred": on_error_occurred}
    })

asyncio.run(main())