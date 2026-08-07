# Prompt: make the Java InProcess test run clean

You are working in `/home/edburns/workareas/copilot-sdk-01`, branch
`edburns/review-copilot-pr-2272`. Read these files first:

- `1917-java-embed-rust-cli-runtime-remove-before-merge/post-agentic-01-test-parity-fix-remaining-tests.md`
- `java/20260807-0145-job-logs.txt`
- the current git diff and the Java test/runtime/harness sources

The target command is:

```bash
cd java
mvn clean verify -Pinprocess
```

Make the implementation and test changes necessary for a genuinely clean,
non-hanging run. Do not solve this by broadly skipping tests, increasing
timeouts, weakening assertions, or hiding errors. Preserve the negative-test
assertions; expected negative cases may be logged, but they must not be
reported as test errors.

## What the interrupted log establishes

The run was interrupted after more than an hour; it has no `BUILD SUCCESS`.
There are 88 errors in 20 suites. The failures are highly clustered:

- `std/in stream corrupted` appears during `AskUserTest`.
- `ByokBearerTokenProviderE2ETest` has the expected fake 404 in one negative
  case, but the other two tests fail because
  `llmInference.setProvider` says “Another client is already the LLM inference
  provider.”
- The same provider-ownership error breaks
  `CopilotRequestCancelErrorE2ETest`, `CopilotRequestHandlerE2ETest`,
  `SessionConfigE2ETest`, and other provider/handler tests.
- `CompactionTest`, `CopilotSessionTest`, `ErrorHandlingTest`,
  `EventFidelityTest`, `ExecutorWiringTest`, `HooksTest`, `McpAndAgentsTest`,
  `ModeHandlersTest`, `MultiProviderRegistryE2ETest`, `PermissionsTest`,
  `PreMcpToolCallHookTest`, `RpcSessionStateExtrasE2ETest`,
  `SessionConfigE2ETest`, and `SessionEventsE2ETest` contain repeated
  approximately 60-second `sendAndWait`/future timeouts.
- `GitHubTelemetryTest` fails immediately because an InProcess connection
  receives `Method not found: connect` and `Method not found: ping`; determine
  whether this test must explicitly use the subprocess/socket transport or
  whether the InProcess endpoint is missing required handlers.
- `RpcServerE2ETest` has a 30-second RPC timeout and
  `RpcSessionStateExtrasE2ETest` has a 60-second timeout.
- `PerSessionAuthTest` has one skipped test and a negative 401 “Bad
  credentials” trace. The test itself is not an error.
- `ClientOptionsE2ETest` skips all three tests. Other suites also report
  intentional-looking skips: `CopilotClientTest` (14),
  `CopilotClientTransportTest` (4), `MetadataApiTest` (3),
  `RpcServerMiscE2ETest` (1), and `CompactionTest` (1).
- Many stack traces in `CreateSessionReKeyEntryTest`, `JsonRpcClientTest`,
  `LifecycleEventManagerTest`, `RpcHandlerDispatcherTest`, and
  `SessionHandlerTest` are deliberately generated negative-test traces and
  are followed by passing summaries. Do not misclassify them as failures.

## Priority 1: stop stream corruption and fix InProcess ownership/lifecycle

Investigate `std/in stream corrupted` first. Trace every process and stream
created by the InProcess FFI path, `host_start`, the bundled `copilot`
entrypoint, `NativeRuntimeLoader`, `InProcessRuntimeConnection`, `CapiProxy`,
and Surefire. Identify which native/child process is writing bytes to the
Surefire-controlled stdout/stdin protocol. Ensure child stdout/stderr are
consumed or redirected in the same way as the supported transport and that
the FFI receive/send streams are not closed or reused by another client.
Do not merely suppress Surefire output.

Then fix the “Another client is already the LLM inference provider” root
cause. Determine whether clients, native hosts, provider registrations, or
`InProcessEnvGuard` instances survive test teardown. Verify the close path on
both successful and failed `start()`, failed `createSession()`, and failed
requests. Ensure a failed startup cannot leave a provider registered and that
each test context closes its client/proxy/runtime deterministically. If the
InProcess runtime is process-global, serialize or otherwise coordinate provider
ownership rather than allowing overlapping providers. Add focused regression
coverage for failed-start cleanup and sequential client startup.

The earlier context notes that `E2ETestContext.applyContextOptions()` must
clear InProcess-incompatible `cwd` and `cliArgs` in addition to `environment`.
Implement that carefully, and verify the actual setter semantics:
`setEnvironment(null)` clears to an empty map, while `setCwd(null)` and
`setCliArgs(null)` must be checked rather than assumed. Add or update tests so
the options are truly absent according to constructor validation.

## Priority 2: isolate and repair the common timeout

After Priority 1, run small, serial selectors, not the full suite:

```bash
cd java
COPILOT_SDK_DEFAULT_CONNECTION=inprocess mvn test -pl sdk \
  -Dtest="AskUserTest,ByokBearerTokenProviderE2ETest,CopilotSessionTest" \
  -DfailIfNoTests=false
```

Use a bounded shell timeout while debugging so a regression cannot consume an
hour. For any remaining timeout, capture a thread dump and inspect the
corresponding Surefire report plus replay-proxy output. Follow one request
from Java JSON-RPC send, through the FFI callback/`QueueInputStream`, into the
replay proxy, and back to the Java reader. Confirm that:

1. `host_start` returns a valid handle and the child `copilot` entrypoint is
   reachable.
2. The request reaches the proxy with the expected snapshot.
3. Every response/event is framed correctly and enqueued to the receive
   stream.
4. stream completion/EOF and client close wake blocked readers.
5. callbacks do not depend on a thread or executor that has already shut down.

Use `StreamingFidelityTest.testShouldEmitStreamingDeltasWithReasoningEffortConfigured`
as the minimal streaming reproducer, but also test one ordinary
`CopilotSessionTest` request. Do not patch each timed-out suite individually;
the repeated 60-second failures indicate a shared transport or lifecycle
defect. Once the common path works, rerun representative handler, hook,
permission, event, session-config, MCP, and RPC-server selectors and only
then the complete profile.

`GitHubTelemetryTest` is a separate transport-contract issue: inspect its
test setup and the supported connection mode. If it intentionally uses a
minimal fake RPC peer that only supports telemetry, make it explicitly select
that transport so the global InProcess profile cannot route it to a runtime
without `connect`/`ping`. If InProcess is intended, implement the missing
protocol surface and add focused coverage.

## Priority 3: remove unjustified skips

Audit every skipped test in the log and the associated assumptions. For each:

- make it run under InProcess when the behavior is transport-independent;
- explicitly force subprocess/socket transport when the test is specifically
  validating subprocess-only options or protocol behavior; or
- change the test setup so the same public behavior is exercised through
  InProcess.

Do not add a profile-wide exclusion and do not convert skipped tests to
passing assertions. In particular, investigate all three
`ClientOptionsE2ETest` skips, the `PerSessionAuthTest` skip, and the skips in
`CopilotClientTest`, `CopilotClientTransportTest`, `MetadataApiTest`,
`RpcServerMiscE2ETest`, and `CompactionTest`. The final profile run should
have zero skips unless a test is demonstrably impossible on the platform and
the repository’s existing policy explicitly permits it; document any
remaining exception in the test source.

## Priority 4: make expected negative output intentional

Do not alter assertions for negative tests. After all tests pass, reduce noisy
expected stack-trace logging only where the repository’s logging conventions
support it: distinguish expected test-triggered failures from unexpected
transport failures, and avoid logging full stack traces at warning/error for
the expected path if that can be done without hiding real failures. The
`fake byok endpoint`, `401 Bad credentials`, `session.resume` not-found,
handler exceptions, malformed JSON, socket-close, and re-key traces must
remain asserted and diagnosable.

## Validation and completion criteria

Use the repository’s normal Java bootstrap and Maven logging conventions.
Format Java changes with `mvn spotless:apply` from `java`. Run focused tests
after each root-cause fix, then:

```bash
cd java
mvn clean verify -Pinprocess
```

The task is complete only when this command terminates normally with
`BUILD SUCCESS`, all test suites report zero failures and zero errors, no
test hangs or 60-second transport timeouts occur, no Surefire stream
corruption occurs, and the skip count is zero or each explicitly justified
platform exception is documented and approved by the existing test policy.
