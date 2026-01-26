# Node.js Agentic Issue Resolver

This sample demonstrates how to build a robust, autonomous developer agent using the **GitHub Copilot SDK**.

The **Node.js Issue Resolver** is a CLI-based agent that bridges the gap between a high-level task description and actual codebase modifications. It leverages the Copilot SDK to reason about a problem, explore the project structure, and apply fixes automatically.

---

## 🚀 Key Features

* **Autonomous Problem Solving**: The agent analyzes your request, identifies relevant files, and determines the best course of action.
* **Intelligent File Handling**: Optimized for the **Technical Preview** by explicitly guiding the agent to use `write_file` for full-file updates. This avoids the common `400 Bad Request` errors associated with partial string-matching in the `edit` tool.
* **Real-Time Observability**: Implements session event listeners to stream the agent's "thinking process" (reasoning) and tool execution logs directly to your terminal.
* **Modern ESM Stack**: Built with TypeScript and `tsx` for high-performance, native ECMAScript Modules execution on Node.js (v18 to v24+).

---

## 🛠 Prerequisites

1.  **GitHub Copilot CLI**: Must be installed and authenticated.
    ```bash
    npm install -g @github/copilot-cli
    copilot -i login
    ```
2.  **Node.js**: Version 18.x or higher (Node 22+ or 24+ recommended).
3.  **Subscription**: An active GitHub Copilot subscription is required.

---

## 📦 Setup

1.  **Navigate to the sample directory**:
    ```bash
    cd samples/nodejs-issue-resolver
    ```
2.  **Install dependencies**:
    ```bash
    npm install
    ```

---

## 🕹 Usage

Run the agent by providing a descriptive task in quotes. The agent will autonomously explore the directory and apply the changes.

### Example: Metadata Update
```bash
npm start "Update the description in package.json to 'AI-Powered Agentic Sample' and bump the version to 0.1.1"
```

### Example: Documentation Generation
```bash
npm start "Create a file named ARCHITECTURE.md explaining that this project uses a JSON-RPC bridge to Copilot CLI"
```

### Example: Code Refactoring
```bash
npm start "Find the main entry point and add a try-catch block around the client initialization"
```
