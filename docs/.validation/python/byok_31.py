# Source: auth/byok.md:22
import asyncio
import os
from copilot import CopilotClient

FOUNDRY_MODEL_URL = "https://your-resource.openai.azure.com/openai/v1/"
# Set FOUNDRY_API_KEY environment variable

async def main():
    client = CopilotClient()
    await client.start()

    session = await client.create_session({
        "model": "gpt-5.2-codex",  # Your deployment name
        "provider": {
            "type": "openai",
            "base_url": FOUNDRY_MODEL_URL,
            "wire_api": "responses",  # Use "completions" for older models
            "api_key": os.environ["FOUNDRY_API_KEY"],
        },
    })

    done = asyncio.Event()

    def on_event(event):
        if event.type.value == "assistant.message":
            print(event.data.content)
        elif event.type.value == "session.idle":
            done.set()

    session.on(on_event)
    await session.send({"prompt": "What is 2+2?"})
    await done.wait()

    await session.destroy()
    await client.stop()

asyncio.run(main())