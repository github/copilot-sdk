# Source: guides/skills.md:161
import asyncio

async def main():
    session = await client.create_session({
        "skill_directories": ["./skills"],
        "disabled_skills": ["experimental-feature", "deprecated-tool"],
    })

asyncio.run(main())