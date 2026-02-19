# Transport Samples

Minimal samples organized by **transport model** — the wire protocol used to communicate with `copilot`. Each subfolder demonstrates one transport with the same "What is the capital of France?" flow.

## Transport Models

| Transport | Description | Languages |
|-----------|-------------|-----------|
| **[stdio](stdio/)** | SDK spawns `copilot` as a child process and communicates via stdin/stdout | TypeScript, Python, Go |
| **[tcp](tcp/)** | SDK connects to a pre-running `copilot` TCP server | TypeScript, Python, Go |
| **[wasm](wasm/)** | SDK loads `copilot` as an in-process WASM module | TypeScript |

## How They Differ

| | stdio | tcp | wasm |
|---|---|---|---|
| **Process model** | Child process | External server | In-process |
| **Binary required** | Yes (auto-spawned) | Yes (pre-started) | No (WASM module) |
| **Wire protocol** | Content-Length framed JSON-RPC over pipes | Content-Length framed JSON-RPC over TCP | In-memory function calls |
| **Best for** | CLI tools, desktop apps | Shared servers, multi-tenant | Serverless, edge, sandboxed |

## Prerequisites

- **Authentication** — set `GITHUB_TOKEN`, or run `gh auth login`
- **Copilot CLI** — required for stdio and tcp (set `COPILOT_CLI_PATH`)
- Language toolchains as needed (Node.js 20+, Python 3.10+, Go 1.24+)

## Verification

Each transport has its own `verify.sh` that builds and runs all language samples:

```bash
cd stdio && ./verify.sh
cd tcp   && ./verify.sh
cd wasm  && ./verify.sh
```
