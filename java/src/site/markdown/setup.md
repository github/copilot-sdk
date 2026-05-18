# Setup & Deployment Guide

This guide explains how to configure the Copilot SDK for different deployment scenarios — from local development to production multi-user applications.

## Quick Reference

| Scenario | Configuration | Guide Section |
|----------|--------------|---------------|
| Local development | Default (no options) | [Local CLI](#local-cli) |
| Multi-user app | `setGitHubToken(userToken)` | [GitHub OAuth](#github-oauth-authentication) |
| Server deployment | `setCliUrl("host:port")` | [Backend Services](#backend-services) |
| Custom CLI location | `setCliPath("/path/to/copilot")` | [Bundled CLI](#bundled-cli) |
| Own model keys | Provider configuration | [BYOK](advanced.html#Bring_Your_Own_Key_BYOK) |

## Local CLI

The simplest setup uses the Copilot CLI already signed in on your development machine.

**Use when:** Building personal projects, prototyping, or learning the SDK.

```java
try (var client = new CopilotClient()) {
    client.start().get();
    var session = client.createSession(
        new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setModel("gpt-4.1")
    ).get();
    // Use session...
}
```

**How it works:** The SDK automatically spawns the CLI process and uses credentials from your system keychain.

**Requirements:**
- Copilot CLI installed and signed in (`copilot auth login`)
- Active Copilot subscription

See [Getting Started](getting-started.html) for a complete tutorial.

## GitHub OAuth Authentication

For multi-user applications where users authenticate with their GitHub accounts.

**Use when:** Building apps where users have GitHub accounts and Copilot subscriptions.

### Basic Setup

After obtaining a user's GitHub OAuth token, pass it to the SDK:

```java
var options = new CopilotClientOptions()
    .setGitHubToken(userAccessToken)
    .setUseLoggedInUser(false);

try (var client = new CopilotClient(options)) {
    client.start().get();
    var session = client.createSession(
        new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setModel("gpt-4.1")
    ).get();
    // Requests are made on behalf of the authenticated user
}
```

### OAuth Flow Integration

Your application handles the OAuth flow:

1. Create a GitHub OAuth App in your GitHub settings
2. Redirect users to GitHub's authorization URL
3. Exchange the authorization code for an access token
4. Pass the token to `CopilotClientOptions.setGitHubToken()`

### Per-User Client Management

Each authenticated user should get their own client instance:

```java
private final Map<String, CopilotClient> clients = new ConcurrentHashMap<>();

public CopilotClient getClientForUser(String userId, String gitHubToken) {
    return clients.computeIfAbsent(userId, id -> {
        var options = new CopilotClientOptions()
            .setGitHubToken(gitHubToken)
            .setUseLoggedInUser(false);
        var client = new CopilotClient(options);
        try {
            client.start().get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start client for user: " + userId, e);
        }
        return client;
    });
}
```

### Token Types

| Token Prefix | Description | Supported |
|--------------|-------------|-----------|
| `gho_` | OAuth user access token | ✅ |
| `ghu_` | GitHub App user access token | ✅ |
| `github_pat_` | Fine-grained personal access token | ✅ |

**Note:** Token lifecycle management (storage, refresh, expiration) is your application's responsibility.

## Backend Services

Run the SDK in server-side applications by connecting to an external CLI server.

**Use when:** Building web backends, APIs, microservices, or any server-side workload.

### Architecture

Instead of spawning a CLI process, your application connects to a separately-running CLI server:

```
┌─────────────────┐       ┌─────────────────┐
│   Your Backend  │       │  CLI Server     │
│                 │       │  (headless)     │
│  ┌───────────┐  │       │  ┌───────────┐  │
│  │    SDK    ├──┼──────►│  │JSON-RPC   │  │
│  └───────────┘  │ TCP   │  │:4321      │  │
└─────────────────┘       │  └───────────┘  │
                          └─────────────────┘
```

### Start the CLI Server

Run the CLI in headless server mode:

```bash
copilot server --port 4321
```

Or with authentication:

```bash
export GITHUB_TOKEN=your_token
copilot server --port 4321
```

### Connect from Your Application

Configure the SDK to connect to the external server:

```java
var options = new CopilotClientOptions()
    .setCliUrl("localhost:4321");

try (var client = new CopilotClient(options)) {
    client.start().get();
    // Client connects to the external server
}
```

### Multiple SDK Clients, One Server

Multiple application instances can share a single CLI server:

```java
// Use an explicit connection token so all clients can authenticate
var token = "my-shared-secret";
var client1 = new CopilotClient(new CopilotClientOptions()
    .setCliUrl("cli-server:4321").setTcpConnectionToken(token));
var client2 = new CopilotClient(new CopilotClientOptions()
    .setCliUrl("cli-server:4321").setTcpConnectionToken(token));
// Both connect to the same CLI server
```

### Connection Token (TCP Security)

When the SDK spawns the CLI in TCP mode, a random connection token is generated automatically
to protect the loopback listener. You can also provide an explicit token:

```java
var options = new CopilotClientOptions()
    .setUseStdio(false)
    .setTcpConnectionToken("my-secret-token");
```

> **Note:** `tcpConnectionToken` cannot be used with `useStdio = true`.

### Deployment Patterns

**Container deployment:**
```yaml
# docker-compose.yml
services:
  cli-server:
    image: copilot-cli:latest
    command: copilot server --port 4321
    environment:
      - GITHUB_TOKEN=${GITHUB_TOKEN}
    ports:
      - "4321:4321"
  
  backend:
    image: your-backend:latest
    environment:
      - CLI_URL=cli-server:4321
    depends_on:
      - cli-server
```

**Kubernetes deployment:**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: copilot-cli
spec:
  selector:
    app: copilot-cli
  ports:
    - port: 4321
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: copilot-cli
spec:
  replicas: 1
  template:
    spec:
      containers:
      - name: cli
        image: copilot-cli:latest
        args: ["server", "--port", "4321"]
        env:
        - name: GITHUB_TOKEN
          valueFrom:
            secretKeyRef:
              name: copilot-auth
              key: token
```

## Bundled CLI

Package the Copilot CLI with your application so users don't need to install it separately.

**Use when:** Distributing desktop applications or standalone tools.

### Configuration

Point the SDK to your bundled CLI binary:

```java
var options = new CopilotClientOptions()
    .setCliPath("./bundled/copilot");  // Relative to working directory

try (var client = new CopilotClient(options)) {
    client.start().get();
    // SDK uses the bundled CLI
}
```

### Packaging

1. Download the appropriate CLI binary for your target platform
2. Include it in your application bundle:
   ```
   my-app/
   ├── bin/
   │   └── copilot      # CLI binary
   ├── lib/
   │   └── my-app.jar
   └── run.sh
   ```
3. Configure the path in your application

### Platform-Specific Binaries

For cross-platform applications, detect the platform and use the appropriate binary:

```java
private String getCliPathForPlatform() {
    String os = System.getProperty("os.name").toLowerCase();
    String arch = System.getProperty("os.arch").toLowerCase();
    
    if (os.contains("win")) {
        return "./bin/copilot-windows-" + arch + ".exe";
    } else if (os.contains("mac")) {
        return "./bin/copilot-darwin-" + arch;
    } else {
        return "./bin/copilot-linux-" + arch;
    }
}

var options = new CopilotClientOptions()
    .setCliPath(getCliPathForPlatform());
```

## Scaling & Multi-Tenancy

For applications serving many concurrent users, consider these patterns:

### Session Isolation

Each user's sessions are automatically isolated within their client instance. For strongest isolation, use one CLI server per user:

```java
// Pattern: Isolated CLI per user (requires CLI server per user)
public CopilotClient createIsolatedClient(String userId, int port) {
    // Start CLI server for this user: copilot server --port {port}
    var options = new CopilotClientOptions()
        .setCliUrl("localhost:" + port);
    return new CopilotClient(options);
}
```

### Resource Management

For high-concurrency scenarios:

```java
// Use a client pool with bounded resources
public class CopilotClientPool {
    private final Semaphore permits;
    private final CopilotClient sharedClient;
    
    public CopilotClientPool(int maxConcurrentSessions) {
        this.permits = new Semaphore(maxConcurrentSessions);
        this.sharedClient = new CopilotClient(/* options */);
    }
    
    public <T> T withSession(SessionConfig config, 
                              Function<CopilotSession, T> action) throws Exception {
        permits.acquire();
        try {
            var session = sharedClient.createSession(config).get();
            try {
                return action.apply(session);
            } finally {
                session.close();
            }
        } finally {
            permits.release();
        }
    }
}
```

### Horizontal Scaling

When scaling beyond a single server:

1. Run multiple CLI servers (one per app server or shared)
2. Use load balancing at the application tier
3. Each app server connects to its assigned CLI server via `setCliUrl()`

## Configuration Reference

Complete list of `CopilotClientOptions` settings:

| Option | Type | Description | Default |
|--------|------|-------------|---------|
| `cliPath` | String | Path to CLI executable | `"copilot"` from PATH |
| `cliUrl` | String | External CLI server URL | `null` (spawn process) |
| `cliArgs` | String[] | Extra CLI arguments | `null` |
| `copilotHome` | String | Base directory for Copilot data | `null` (~/.copilot) |
| `gitHubToken` | String | GitHub OAuth token | `null` |
| `useLoggedInUser` | Boolean | Use system credentials | `true` |
| `useStdio` | boolean | Use stdio transport | `true` |
| `port` | int | TCP port for CLI | `0` (random) |
| `tcpConnectionToken` | String | Connection token for TCP mode | `null` (auto-generated) |
| `autoStart` | boolean | Auto-start server | `true` |
| `autoRestart` | boolean | Auto-restart on crash | `true` |
| `logLevel` | String | CLI log level | `"info"` |
| `environment` | Map | Environment variables | inherited |
| `cwd` | String | Working directory | current dir |
| `onListModels` | Supplier | Custom model listing implementation | `null` (use CLI) |

### Extra CLI Arguments

Pass additional command-line arguments to the CLI process:

```java
var options = new CopilotClientOptions()
    .setCliArgs(new String[]{"--verbose", "--no-telemetry"});

try (var client = new CopilotClient(options)) {
    client.start().get();
    // CLI process receives the extra flags
}
```

### Custom Environment Variables

Set environment variables for the CLI process (merged with the inherited system environment):

```java
var options = new CopilotClientOptions()
    .setEnvironment(Map.of(
        "HTTPS_PROXY", "http://proxy.example.com:8080",
        "NO_PROXY", "localhost,127.0.0.1"
    ));

try (var client = new CopilotClient(options)) {
    client.start().get();
    // CLI process uses the proxy settings
}
```

This is useful for configuring proxy servers, custom CA certificates, or any environment-specific settings the CLI needs.

## Best Practices

### Development

- Use default configuration (local CLI) for fastest iteration
- Enable debug logging: `setLogLevel("debug")`
- Test with multiple models to ensure compatibility

### Production

- Use external CLI servers (`setCliUrl`) for better resource management
- Implement health checks on the CLI server endpoint
- Monitor CLI server resource usage (CPU, memory)
- Use connection pooling for high-concurrency scenarios
- Implement proper token refresh for OAuth-based auth
- Set appropriate timeouts for session operations

### Security

- Never log or expose GitHub tokens
- Use environment variables for tokens in production
- Regularly rotate tokens
- Implement proper access controls for multi-user apps
- Validate user input before sending to sessions

## Next Steps

- **[Getting Started](getting-started.html)** - Complete tutorial
- **[Documentation](documentation.html)** - Core concepts
- **[Advanced Usage](advanced.html)** - Tools, BYOK, MCP servers
- **[API Javadoc](apidocs/index.html)** - Complete API reference
