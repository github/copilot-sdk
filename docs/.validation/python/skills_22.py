# Source: guides/skills.md:47
from copilot import CopilotClient

async def main():
    client = CopilotClient()
    await client.start()

    session = await client.create_session({
        "model": "gpt-4.1",
        "skill_directories": [
            "./skills/code-review",
            "./skills/documentation",
            "~/.copilot/skills",  # User-level skills
        ],
    })

    # Copilot now has access to skills in those directories
    await session.send_and_wait({"prompt": "Review this code for security issues"})

    await client.stop()