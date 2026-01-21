# Copilot CLI SDKs

Language-specific SDKs for programmatic access to the GitHub Copilot CLI.

All SDKs are in technical preview and may change in breaking ways as we move towards a stable release.

## Available SDKs

| SDK                      | Location                          | Installation                              |
| ------------------------ | --------------------------------- | ----------------------------------------- |
| **Node.js / TypeScript** | [`./nodejs/`](./nodejs/README.md) | `npm install @github/copilot-sdk`         |
| **Python**               | [`./python/`](./python/README.md) | `pip install github-copilot-sdk`          |
| **Go**                   | [`./go/`](./go/README.md)         | `go get github.com/github/copilot-sdk/go` |
| **.NET**                 | [`./dotnet/`](./dotnet/README.md) | `dotnet add package GitHub.Copilot.SDK`   |
| **Rust**                 | [`./rust/`](./rust/README.md)     | `cargo add copilot-sdk`                   |

See the individual SDK READMEs for installation, usage examples, and API reference.

## Getting Started

1. **Install the Copilot CLI:**

   Follow the [Copilot CLI installation guide](https://docs.github.com/en/copilot/how-tos/set-up/install-copilot-cli) to install the CLI, or ensure `copilot` is available in your PATH.

2. **Install your preferred SDK** using the commands above.

3. **See the SDK README** for usage examples and API documentation.

## Architecture

All SDKs communicate with the Copilot CLI server via JSON-RPC:

```
Your Application
       ↓
  SDK Client
       ↓ JSON-RPC
  Copilot CLI (server mode)
```

The SDK manages the CLI process lifecycle automatically. You can also connect to an external CLI server—see individual SDK docs for details.

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md) for contribution guidelines.

## License

MIT
