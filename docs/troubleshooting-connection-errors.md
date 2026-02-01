# Troubleshooting Connection Errors

This guide helps you diagnose and fix common connection errors when using the GitHub Copilot SDK.

## "The JSON-RPC connection with the remote party was lost before the request could complete"

This error occurs when the SDK cannot communicate with the Copilot CLI. Here are the most common causes and solutions:

### 1. CLI Not Installed or Not in PATH

**Symptom**: Error message mentions "Failed to start Copilot CLI process"

**Solution**: 
- Verify the CLI is installed: `copilot --version`
- If not installed, follow the [installation guide](https://docs.github.com/en/copilot/how-tos/set-up/install-copilot-cli)
- If installed but not in PATH, specify the full path in your code:

<details open>
<summary><strong>.NET</strong></summary>

```csharp
var client = new CopilotClient(new CopilotClientOptions
{
    CliPath = "/path/to/copilot" // or "C:\\path\\to\\copilot.exe" on Windows
});
```

</details>

<details>
<summary><strong>Node.js</strong></summary>

```typescript
const client = new CopilotClient({
    cliPath: "/path/to/copilot"
});
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
client = CopilotClient({
    "cli_path": "/path/to/copilot"
})
```

</details>

### 2. CLI Process Exits Immediately

**Symptom**: Error message mentions "CLI process exited immediately with code X" or "CLI process exited unexpectedly"

**Common causes**:
- **Not authenticated**: The CLI requires authentication with GitHub
  - Solution: Run `copilot auth login` to authenticate
  - Verify authentication: `copilot auth status`

- **Missing dependencies**: The CLI may require Node.js or other dependencies
  - For JavaScript-based CLI: Ensure Node.js 18+ is installed
  - Check the error output included in the exception message for clues

- **Permissions issues**: The CLI executable may not have execute permissions
  - On Unix/Linux/Mac: `chmod +x /path/to/copilot`

### 3. Version Incompatibility

**Symptom**: Error mentioning "protocol version mismatch"

**Solution**: Update either the SDK or CLI to compatible versions
- Check the [release notes](https://github.com/github/copilot-sdk/releases) for compatibility information
- Update CLI: Follow the installation guide to get the latest version
- Update SDK: Install the latest SDK package

### 4. Port Already in Use (TCP Mode)

**Symptom**: Connection error when using TCP mode

**Solution**: 
- Let the SDK choose a random port (don't specify the port option)
- Or specify a different port:

```csharp
var client = new CopilotClient(new CopilotClientOptions
{
    UseStdio = false,
    Port = 8080 // Choose an available port
});
```

### 5. Timeout Waiting for CLI

**Symptom**: "Timed out waiting for CLI server to announce its port"

**Causes**:
- CLI is taking too long to start (slow machine, antivirus scanning, etc.)
- CLI failed to start but didn't exit
- Firewall blocking network communication

**Solutions**:
- Check if antivirus is scanning the CLI executable
- Try using stdio mode instead of TCP (default in SDK):
  ```csharp
  var client = new CopilotClient(new CopilotClientOptions { UseStdio = true });
  ```
- Check firewall settings if using TCP mode

## Getting More Information

### Enable Debug Logging

To see detailed diagnostic information:

<details open>
<summary><strong>.NET</strong></summary>

```csharp
using Microsoft.Extensions.Logging;

var loggerFactory = LoggerFactory.Create(builder =>
{
    builder.AddConsole();
    builder.SetMinimumLevel(LogLevel.Debug);
});

var client = new CopilotClient(new CopilotClientOptions
{
    Logger = loggerFactory.CreateLogger<CopilotClient>(),
    LogLevel = "debug" // CLI log level
});
```

</details>

<details>
<summary><strong>Node.js</strong></summary>

```typescript
const client = new CopilotClient({
    logLevel: "debug"
});
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
import logging
logging.basicConfig(level=logging.DEBUG)

client = CopilotClient({
    "log_level": "debug"
})
```

</details>

### Check CLI Directly

Test the CLI independently to isolate SDK issues:

```bash
# Test basic CLI functionality
copilot --version

# Check authentication
copilot auth status

# Start CLI in server mode manually
copilot --server --port 4321

# In another terminal, try to connect using the SDK
# with cliUrl: "localhost:4321"
```

### Examine Error Output

The latest versions of the SDK include stderr output from the CLI in error messages when processes fail. Look for:
- Authentication errors
- Missing file or permission errors
- Node.js errors (if CLI is JS-based)
- Network/proxy configuration issues

## Common Error Messages and Solutions

| Error Message | Likely Cause | Solution |
|--------------|--------------|----------|
| "Failed to start Copilot CLI process" | CLI not found or not executable | Check installation and PATH |
| "exited immediately with code 1" | Authentication or configuration error | Run `copilot auth login` |
| "exited immediately with code 127" | Command not found | Verify CLI is in PATH |
| "Timed out waiting for CLI server" | CLI failed to start or network issue | Check logs, try stdio mode |
| "protocol version mismatch" | SDK and CLI versions incompatible | Update SDK or CLI |

## Still Having Issues?

If you're still experiencing problems:

1. **Collect diagnostic information**:
   - SDK version
   - CLI version (`copilot --version`)
   - Operating system and version
   - Full error message with stack trace
   - CLI stderr output (included in recent error messages)

2. **Create a minimal reproduction**:
   - Simplest possible code that reproduces the error
   - Share your client configuration options

3. **Report the issue**:
   - Open an issue on the [GitHub repository](https://github.com/github/copilot-sdk)
   - Include all diagnostic information collected above

## Related Documentation

- [Getting Started Guide](./getting-started.md)
- [API Reference](../dotnet/README.md)
- [Copilot CLI Documentation](https://docs.github.com/en/copilot/how-tos/set-up/install-copilot-cli)
