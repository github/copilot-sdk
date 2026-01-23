# Copilot SDK Playground

An interactive web-based playground for testing and exploring the GitHub Copilot SDK. Create sessions, send messages, and watch responses stream in real-time.

## Quick Start

**Prerequisites:** [Install the Copilot CLI](https://docs.github.com/en/copilot/how-tos/set-up/install-copilot-cli)

```bash
cd demos/playground
npm install
npm start
```

Open **http://localhost:5173** in your browser.

### Point at a specific repo

```bash
COPILOT_CWD=/path/to/your/repo npm start
```

## Features

- **Session Management** - Create and manage multiple SDK sessions
- **Interactive Chat** - Send messages and receive streaming responses
- **Tool Execution** - Watch tool calls execute with collapsible details
- **Custom Providers** - Use your own API keys (OpenAI, Azure, Anthropic)
- **Multiple Models** - Test with GPT-5, Claude, Gemini, and more

## Usage

1. **Create a Session** - Select a model and click "Create Session"
2. **Send Messages** - Type in the input area and press Enter
3. **View Responses** - Watch the assistant respond in real-time
4. **Inspect Tools** - Click on tool cards to expand arguments/results

### Custom Providers (BYOK)

Expand "Provider Config" when creating a session to use your own API:

| Provider | Base URL | Notes |
|----------|----------|-------|
| OpenAI | `https://api.openai.com/v1` | Use your OpenAI API key |
| Azure | `https://your-resource.openai.azure.com` | Supports bearer token auth |
| Anthropic | `https://api.anthropic.com` | Use your Anthropic API key |
| Ollama | `http://localhost:11434/v1` | No API key needed |

## Architecture

```
React App (Vite)
     ↓ HTTP API
Express Server
     ↓ TypeScript SDK
Copilot CLI (stdio)
```

The playground uses the SDK to communicate with the CLI process via JSON-RPC over stdio. All session state is managed by the SDK.

## Development

```bash
# Run with custom CLI path (for local development)
COPILOT_CLI_PATH=/path/to/cli npm start

# Build for production
npm run build
```

## Project Structure

```
src/
├── App.tsx              # Main app component
├── App.css              # Styles
├── api.ts               # API client
├── types.ts             # TypeScript types
├── hooks/
│   └── useCopilotSDK.ts # Main SDK hook
└── components/
    ├── Sidebar.tsx      # Session list + form
    ├── ChatArea.tsx     # Messages + input
    ├── Message.tsx      # User/assistant message
    ├── ToolMessage.tsx  # Tool execution card
    └── StatusBar.tsx    # Connection status
```

The `useCopilotSDK` hook encapsulates all SDK interaction logic - a good starting point for understanding how to integrate the SDK into your own React application.
