# Source: hooks/session-lifecycle.md:112
import asyncio

async def main():
    async def on_session_start(input_data, invocation):
        print(f"Session {invocation['session_id']} started ({input_data['source']})")
        
        project_info = await detect_project_type(input_data["cwd"])
        
        return {
            "additionalContext": f"""
    This is a {project_info['type']} project.
    Main language: {project_info['language']}
    Package manager: {project_info['packageManager']}
            """.strip()
        }
    
    session = await client.create_session({
        "hooks": {"on_session_start": on_session_start}
    })

asyncio.run(main())