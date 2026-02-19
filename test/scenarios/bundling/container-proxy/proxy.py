#!/usr/bin/env python3
"""
Minimal OpenAI-compatible proxy for the container-proxy sample.

This replaces a real LLM provider — Copilot CLI (running in Docker) sends
its model requests here and gets back a canned response.  The point is to
prove the network path:

    client  →  Copilot CLI (container :3000)  →  this proxy (host :4000)
"""

import json
import sys
import time
from http.server import HTTPServer, BaseHTTPRequestHandler


class ProxyHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        length = int(self.headers.get("Content-Length", 0))
        body = json.loads(self.rfile.read(length)) if length else {}

        model = body.get("model", "claude-haiku-4.5")
        stream = body.get("stream", False)

        if stream:
            self._handle_stream(model)
        else:
            self._handle_non_stream(model)

    def do_GET(self):
        # Health check
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(json.dumps({"status": "ok"}).encode())

    # ── Non-streaming ────────────────────────────────────────────────

    def _handle_non_stream(self, model: str):
        resp = {
            "id": "chatcmpl-proxy-0001",
            "object": "chat.completion",
            "created": int(time.time()),
            "model": model,
            "choices": [
                {
                    "index": 0,
                    "message": {
                        "role": "assistant",
                        "content": "The capital of France is Paris.",
                    },
                    "finish_reason": "stop",
                }
            ],
            "usage": {"prompt_tokens": 0, "completion_tokens": 0, "total_tokens": 0},
        }
        payload = json.dumps(resp).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    # ── Streaming (SSE) ──────────────────────────────────────────────

    def _handle_stream(self, model: str):
        self.send_response(200)
        self.send_header("Content-Type", "text/event-stream")
        self.send_header("Cache-Control", "no-cache")
        self.end_headers()

        ts = int(time.time())

        # Single content chunk
        chunk = {
            "id": "chatcmpl-proxy-0001",
            "object": "chat.completion.chunk",
            "created": ts,
            "model": model,
            "choices": [
                {
                    "index": 0,
                    "delta": {"role": "assistant", "content": "The capital of France is Paris."},
                    "finish_reason": None,
                }
            ],
        }
        self.wfile.write(f"data: {json.dumps(chunk)}\n\n".encode())
        self.wfile.flush()

        # Final chunk with finish_reason
        done_chunk = {
            "id": "chatcmpl-proxy-0001",
            "object": "chat.completion.chunk",
            "created": ts,
            "model": model,
            "choices": [
                {
                    "index": 0,
                    "delta": {},
                    "finish_reason": "stop",
                }
            ],
        }
        self.wfile.write(f"data: {json.dumps(done_chunk)}\n\n".encode())
        self.wfile.write(b"data: [DONE]\n\n")
        self.wfile.flush()

    def log_message(self, format, *args):
        print(f"[proxy] {args[0]}", file=sys.stderr)


def main():
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 4000
    server = HTTPServer(("0.0.0.0", port), ProxyHandler)
    print(f"Proxy listening on :{port}", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    main()
