import asyncio
import json
import os
import sys
import urllib.request

from flask import Flask, request, jsonify
from copilot import CopilotClient

app = Flask(__name__)

CLI_URL = os.environ.get("CLI_URL", os.environ.get("COPILOT_CLI_URL", "localhost:3000"))


async def ask_copilot(prompt: str) -> str:
    client = CopilotClient({"cli_url": CLI_URL})

    try:
        session = await client.create_session({"model": "claude-haiku-4.5"})

        response = await session.send_and_wait({"prompt": prompt})

        await session.destroy()

        if response:
            return response.data.content
        return ""
    finally:
        await client.stop()


@app.route("/chat", methods=["POST"])
def chat():
    data = request.get_json(force=True)
    prompt = data.get("prompt", "")
    if not prompt:
        return jsonify({"error": "Missing 'prompt' in request body"}), 400

    content = asyncio.run(ask_copilot(prompt))
    if content:
        return jsonify({"response": content})
    return jsonify({"error": "No response content from Copilot CLI"}), 502


def self_test(port: int):
    """Send a test request to ourselves and print the response."""
    url = f"http://localhost:{port}/chat"
    payload = json.dumps({"prompt": "What is the capital of France?"}).encode()
    req = urllib.request.Request(url, data=payload, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req) as resp:
        result = json.loads(resp.read().decode())
    if result.get("response"):
        print(result["response"])
    else:
        print("Self-test failed:", result, file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    import threading

    port = int(os.environ.get("PORT", "8080"))

    if os.environ.get("SELF_TEST") == "1":
        # Start server in a background thread, run self-test, then exit
        server_thread = threading.Thread(
            target=lambda: app.run(host="0.0.0.0", port=port, debug=False),
            daemon=True,
        )
        server_thread.start()
        import time
        time.sleep(1)
        self_test(port)
    else:
        app.run(host="0.0.0.0", port=port, debug=False)
