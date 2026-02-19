import asyncio
import os
from copilot import CopilotClient, define_tool
from pydantic import BaseModel, Field

# In-memory virtual filesystem
virtual_fs: dict[str, str] = {}


class CreateFileParams(BaseModel):
    path: str = Field(description="File path")
    content: str = Field(description="File content")


class ReadFileParams(BaseModel):
    path: str = Field(description="File path")


@define_tool(description="Create or overwrite a file at the given path with the provided content")
def create_file(params: CreateFileParams) -> str:
    virtual_fs[params.path] = params.content
    return f"Created {params.path} ({len(params.content)} bytes)"


@define_tool(description="Read the contents of a file at the given path")
def read_file(params: ReadFileParams) -> str:
    content = virtual_fs.get(params.path)
    if content is None:
        return f"Error: file not found: {params.path}"
    return content


@define_tool(description="List all files in the virtual filesystem")
def list_files() -> str:
    if not virtual_fs:
        return "No files"
    return "\n".join(virtual_fs.keys())


async def auto_approve_permission(request, invocation):
    return {"kind": "approved"}


async def auto_approve_tool(input_data, invocation):
    return {"permissionDecision": "allow"}


async def main():
    opts = {"github_token": os.environ.get("GITHUB_TOKEN")}
    if os.environ.get("COPILOT_CLI_PATH"):
        opts["cli_path"] = os.environ["COPILOT_CLI_PATH"]
    client = CopilotClient(opts)

    try:
        session = await client.create_session(
            {
                "model": "claude-haiku-4.5",
                "available_tools": [],
                "tools": [create_file, read_file, list_files],
                "on_permission_request": auto_approve_permission,
                "hooks": {"on_pre_tool_use": auto_approve_tool},
            }
        )

        response = await session.send_and_wait(
            {
                "prompt": (
                    "Create a file called plan.md with a brief 3-item project plan "
                    "for building a CLI tool. Then read it back and tell me what you wrote."
                )
            }
        )

        if response:
            print(response.data.content)

        # Dump the virtual filesystem to prove nothing touched disk
        print("\n--- Virtual filesystem contents ---")
        for path, content in virtual_fs.items():
            print(f"\n[{path}]")
            print(content)

        await session.destroy()
    finally:
        await client.stop()


asyncio.run(main())
