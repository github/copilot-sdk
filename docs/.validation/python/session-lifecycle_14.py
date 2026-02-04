# Source: hooks/session-lifecycle.md:307
import asyncio

async def main():
    session_start_times = {}
    
    async def on_session_start(input_data, invocation):
        session_start_times[invocation["session_id"]] = input_data["timestamp"]
        return None
    
    async def on_session_end(input_data, invocation):
        start_time = session_start_times.get(invocation["session_id"])
        duration = input_data["timestamp"] - start_time if start_time else 0
        
        await record_metrics({
            "session_id": invocation["session_id"],
            "duration": duration,
            "end_reason": input_data["reason"],
        })
        
        session_start_times.pop(invocation["session_id"], None)
        return None
    
    session = await client.create_session({
        "hooks": {
            "on_session_start": on_session_start,
            "on_session_end": on_session_end,
        }
    })

asyncio.run(main())