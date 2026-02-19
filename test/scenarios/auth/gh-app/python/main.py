import asyncio
import json
import os
import time
import urllib.request

from copilot import CopilotClient


DEVICE_CODE_URL = "https://github.com/login/device/code"
ACCESS_TOKEN_URL = "https://github.com/login/oauth/access_token"
USER_URL = "https://api.github.com/user"


def post_json(url: str, payload: dict) -> dict:
    req = urllib.request.Request(
        url=url,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Accept": "application/json", "Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req) as response:
        return json.loads(response.read().decode("utf-8"))


def get_json(url: str, token: str) -> dict:
    req = urllib.request.Request(
        url=url,
        headers={
            "Accept": "application/json",
            "Authorization": f"Bearer {token}",
            "User-Agent": "copilot-sdk-samples-auth-gh-app",
        },
        method="GET",
    )
    with urllib.request.urlopen(req) as response:
        return json.loads(response.read().decode("utf-8"))


def start_device_flow(client_id: str) -> dict:
    return post_json(DEVICE_CODE_URL, {"client_id": client_id, "scope": "read:user"})


def poll_for_access_token(client_id: str, device_code: str, interval: int) -> str:
    delay_seconds = interval
    while True:
        time.sleep(delay_seconds)
        data = post_json(
            ACCESS_TOKEN_URL,
            {
                "client_id": client_id,
                "device_code": device_code,
                "grant_type": "urn:ietf:params:oauth:grant-type:device_code",
            },
        )
        if data.get("access_token"):
            return data["access_token"]
        if data.get("error") == "authorization_pending":
            continue
        if data.get("error") == "slow_down":
            delay_seconds = int(data.get("interval", delay_seconds + 5))
            continue
        raise RuntimeError(data.get("error_description") or data.get("error") or "OAuth polling failed")


async def main():
    client_id = os.environ.get("GITHUB_OAUTH_CLIENT_ID")
    if not client_id:
        raise RuntimeError("Missing GITHUB_OAUTH_CLIENT_ID")

    print("Starting GitHub OAuth device flow...")
    device = start_device_flow(client_id)
    print(f"Open {device['verification_uri']} and enter code: {device['user_code']}")
    input("Press Enter after you authorize this app...")

    token = poll_for_access_token(client_id, device["device_code"], int(device["interval"]))
    user = get_json(USER_URL, token)
    display_name = f" ({user.get('name')})" if user.get("name") else ""
    print(f"Authenticated as: {user.get('login')}{display_name}")

    opts = {"github_token": token}
    if os.environ.get("COPILOT_CLI_PATH"):
        opts["cli_path"] = os.environ["COPILOT_CLI_PATH"]
    client = CopilotClient(opts)

    try:
        session = await client.create_session({"model": "claude-haiku-4.5"})
        response = await session.send_and_wait({"prompt": "What is the capital of France?"})
        if response:
            print(response.data.content)
        await session.destroy()
    finally:
        await client.stop()


if __name__ == "__main__":
    asyncio.run(main())
