# SDK-hosted AHP child proof of concept

This sample uses an ordinary Node SDK client and an unmodified runtime. The
application launches a separate AHP adapter, which reuses the existing
`copilot-ahp-host` `Host` and `CopilotBackend`, including its filesystem service.
It does not use the runtime-embedded AHP prototype.

```text
Independent AHP client -- WebSocket --> AHP adapter child
                                          |
                                      Rust SDK
                                          |
                                      stdio frames
                                          |
Node application -------------------- forwarder
  |                                       |
ordinary CopilotClient                    |
  |                                       |
  +---- runtime TCP connection A          +---- runtime TCP connection B
                    |                                      |
                    +-------- same unmodified runtime -----+
```

Each SDK connection has its own request ID namespace. The forwarder preserves
ordinary SDK JSON-RPC frames and IDs; it does not implement session operations.
It adds the runtime's connection token to the child's `connect` request.

Two small control requests, `sdkHost.createSession` and
`sdkHost.resumeSession`, invoke application factory code. The factory uses
ordinary SDK APIs to return `{ sessionId }` for a live, application-configured
session. The child then uses ordinary SDK resume to join that session on its own
connection. The application retains its attachment and its custom tool handler.

## Run

Requires Node 22+, a compatible native `copilot-runtime` binary with its adjacent
provider library, and the `copilot-ahp-adapter` binary from the
`prototype/sdk-hosted-ahp-adapter` host branch.

Build the ordinary Node SDK:

```sh
cd nodejs
npm ci --ignore-scripts
npm run build
cd ../samples/ahp-adapter
npm ci --ignore-scripts
```

Set `COPILOT_SDK_AUTH_TOKEN`, `GITHUB_TOKEN`, or `GH_TOKEN` to a token with Copilot
access, then run:

```sh
npm run host -- /absolute/path/to/copilot-runtime /absolute/path/to/copilot-ahp-adapter --verify
```

The application starts the runtime on an ephemeral TCP port with a random
connection token. The adapter starts its own loopback WebSocket listener.
`--verify` launches the independent standard AHP client as another process.

The client lists the fixture workspace, reads `hello.txt`, and requires an
outside-root read to fail with AHP `PermissionDenied`. These commands are served
by the existing backend's filesystem implementation: there is no filesystem
implementation in the Node bridge or new filesystem RPC in the runtime.

Next, an AHP `createSession` invokes the application's factory, which installs
the Bert system message, streaming, permission callback, and `bert_identity`
tool. The client sends `who are you`, requires `I am Bert.` with streaming
deltas, and explicitly invokes the original application-owned tool. The
application also observes the same session's response events through its SDK
connection.

Finally, the application signals the adapter to stop (keeping stdio open while
it disconnects) and sends another Bert prompt through its original SDK session.
This checks that shutting
down the borrowed AHP participant does not shut down the runtime or dispose
the application's session.

Omit `--verify` to leave the host running; it prints a client command. Use
`COPILOT_MODEL` to change the default `gpt-4.1` model.

## Deliberate limits

This is a local development proof, not a hardened public service. The AHP
listener is loopback-only and unauthenticated. The application factory fixes
the workspace and model rather than accepting arbitrary AHP configuration.
It automatically approves permissions for this controlled demonstration.
Production applications must supply their own authorization and permission
policy.

The forwarder and factories are sample code, not new supported SDK APIs.
Production packaging, bounded forwarding queues, reconnect recovery,
cross-language support, application-owned HTTP hosting, session catalogue
authorization, and full AHP feature coverage remain separate work. The
runtime and Node SDK product code are unchanged.
