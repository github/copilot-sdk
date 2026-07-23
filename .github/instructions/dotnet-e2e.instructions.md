---
applyTo: "dotnet/test/E2E/**/*.cs"
---

# .NET E2E test instructions

- Create and resume sessions through `E2ETestContext` using `Ctx.CreateSessionAsync` and `Ctx.ResumeSessionAsync`. Do not call these methods directly on `CopilotClient`; the context applies the backend selected by the E2E matrix while preserving providers explicitly configured by the test.
- Create clients with `Ctx.CreateClient` so they receive the harness environment, CLI path, authentication defaults, transport handling, and lifecycle tracking.
- Instantiate `CopilotClient` directly only when client construction, startup, shutdown, or disposal is the behavior under test. Keep cleanup explicit in those tests, and still create any sessions through `Ctx` so they participate in backend coverage.
