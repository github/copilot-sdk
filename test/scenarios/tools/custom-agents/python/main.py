import asyncio
import os
from copilot import CopilotClient
from copilot.client import SubprocessConfig
from copilot.tools import Tool


async def analyze_handler(args):
    return f"Analysis result for: {args.get('query', '')}"


async def main():
    client = CopilotClient(SubprocessConfig(
        github_token=os.environ.get("GITHUB_TOKEN"),
        cli_path=os.environ.get("COPILOT_CLI_PATH"),
    ))

    try:
        session = await client.create_session(
            model="claude-haiku-4.5",
            tools=[
                Tool(
                    name="analyze-codebase",
                    description="Performs deep analysis of the codebase",
                    handler=analyze_handler,
                    parameters={
                        "type": "object",
                        "properties": {"query": {"type": "string"}},
                    },
                ),
            ],
            default_agent={"excluded_tools": ["analyze-codebase"]},
            custom_agents=[
                {
                    "name": "researcher",
                    "display_name": "Research Agent",
                    "description": "A research agent that can only read and search files, not modify them",
                    "tools": ["grep", "glob", "view", "analyze-codebase"],
                    "prompt": "You are a research assistant. You can search and read files but cannot modify anything. When asked about your capabilities, list the tools you have access to.",
                },
            ],
            on_permission_request=lambda _: {"action": "allow"},
        )

        response = await session.send_and_wait(
            "What custom agents are available? Describe the researcher agent and its capabilities."
        )

        if response:
            print(response.data.content)

        await session.disconnect()
    finally:
        await client.stop()


asyncio.run(main())
