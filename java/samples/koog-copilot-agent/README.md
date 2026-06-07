# Koog + GitHub Copilot SDK agent sample

This sample shows two small integration points between [Koog](https://github.com/JetBrains/koog) and the GitHub Copilot SDK for Java:

1. `CopilotBackedLLMClient` adapts Copilot SDK sessions into a Koog `LLMClient`.
2. `CopilotToolSet` exposes an `ask_copilot` Koog tool that delegates analysis-only workspace questions to the Copilot SDK.

The sample is intentionally conservative: prompts tell Copilot to analyze only, and the SDK session disables file hooks, host git operations, and Koog-side skill loading.

## Requirements

- Java 17 or later.
- GitHub Copilot CLI 1.0.55-5 or later in `PATH`.
- An authenticated Copilot CLI session.

Check the local prerequisites:

```bash
copilot --version
java -version
```

## Run

From this directory:

```bash
gradle run --args="--workspace ../../../ --task 'Summarize the Java SDK entry points without modifying files.'"
```

Use a specific Copilot model:

```bash
gradle run --args="--workspace ../../../ --copilot-model gpt-5.2 --task 'Explain how sessions are created in the Java SDK.'"
```

By default the sample uses `--copilot-model auto`, letting Copilot select an available model for the authenticated account.

To test against a local checkout of Koog instead of Maven Central, set `KOOG_INCLUDE_BUILD` to the Koog checkout path:

```bash
KOOG_INCLUDE_BUILD=../../../../koog gradle build
```

## Notes

- The Koog LLM backend is also Copilot SDK in this sample. The adapter asks Copilot to return a strict JSON envelope so Koog can distinguish text responses from Koog tool calls.
- This is a POC, not a full production provider. Streaming and embeddings are intentionally not implemented.
- Tool calling is limited to text arguments/results and is meant to demonstrate the Koog agent loop.
