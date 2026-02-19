# Config Sample: File Attachments

Demonstrates sending **file attachments** alongside a prompt using the Copilot SDK. This validates that the SDK correctly passes file content to the model and the model can reference it in its response.

## What Each Sample Does

1. Creates a session with a custom system prompt in `replace` mode
2. Resolves the path to `sample-data.txt` (a small text file in the scenario root)
3. Sends: _"What languages are listed in the attached file?"_ with the file as an attachment
4. Prints the response — which should list TypeScript, Python, and Go

## Attachment Format

| Field | Value | Description |
|-------|-------|-------------|
| `type` | `"file"` | Indicates a local file attachment |
| `path` | Absolute path to file | The SDK reads and sends the file content to the model |

### Language-Specific Usage

| Language | Attachment Syntax |
|----------|------------------|
| TypeScript | `attachments: [{ type: "file", path: sampleFile }]` |
| Python | `"attachments": [{"type": "file", "path": sample_file}]` |
| Go | `Attachments: []copilot.Attachment{{Type: "file", Path: sampleFile}}` |

## Sample Data

The `sample-data.txt` file contains basic project metadata used as the attachment target:

```
Project: Copilot SDK Samples
Version: 1.0.0
Description: Minimal buildable samples demonstrating the Copilot SDK
Languages: TypeScript, Python, Go
```

## Run

```bash
./verify.sh
```

Requires the `copilot` binary (auto-detected or set `COPILOT_CLI_PATH`) and `GITHUB_TOKEN`.
