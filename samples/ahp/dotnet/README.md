# SDK-owned AHP with .NET and Kestrel

This sample exposes the same SDK-created Bert session to an independent standard
AHP client. The runtime implements AHP; the SDK forwards opaque messages; Kestrel
owns the listener and WebSocket framing. No server package is added to the SDK.

Requires .NET 8+ and a runtime built with the SDK-owned AHP endpoint RPCs:

```sh
# From the repository root; GITHUB_TOKEN or GH_TOKEN is read without being printed.
dotnet run --project samples/ahp/dotnet/AhpHost.csproj \
  -p:CopilotSkipCliDownload=true -- /absolute/path/to/copilot-runtime
```

The runtime's native library must be beside the runtime executable. The sample
prints an ephemeral loopback URL and session ID. In another terminal:

```sh
cd samples/ahp
npm run client -- 'ws://127.0.0.1:PORT/ahp' 'SESSION_ID'
```

The AHP client should receive `I am Bert.` and the .NET host should print the same
final response under `[SDK observed ...]`, plus a streaming delta count.

**Demo authentication policy:** every loopback process is trusted. The listener
binds only to `127.0.0.1`, checks the peer address before upgrading, and must not be
forwarded or exposed. Production applications must authenticate and authorize
callers before `AcceptConnection`; an endpoint exposes all live local sessions in
the runtime engine. Endpoint ownership does not provide per-user session authorization.

`ReceiveChunkAsync` assembles WebSocket fragments with strict UTF-8 decoding.
Queues are bounded to 8 MiB and 64 pending messages per direction, with ten-second
admission/send deadlines. Each connection serializes sends and receives
independently. The listener remains application-owned when the endpoint is disposed.
