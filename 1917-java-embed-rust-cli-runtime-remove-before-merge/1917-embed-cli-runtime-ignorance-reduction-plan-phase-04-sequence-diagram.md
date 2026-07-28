# Phase 4 Sequence Diagram (Implementation Order)

```mermaid
sequenceDiagram
    autonumber
    actor DRI as Human DRI
    participant Agent as Coding Agent
    participant Tests as Unit/IT Tests
    participant Build as Maven Build
    participant Repo as Git Branch
    participant CI as GitHub Actions CI

    Note over DRI,CI: Phase 4 invariant: native implementation scope is Ubuntu 24.04 linux-x64 only.
    Note over DRI,CI: Out of scope in this phase: linux-arm64, linuxmusl-x64, linuxmusl-arm64, darwin-x64, darwin-arm64, win32-x64, win32-arm64.

    DRI->>Agent: Start Phase 4 implementation

    rect rgba(240, 248, 255, 0.6)
    Note over Agent,Build: TDD cycle applied to each implementation step
    loop For each step (4.1, 4.2, 4.3, 4.4, 4.5, 4.6a, 4.6b, 4.7, 4.8)
        Agent->>Tests: Write tests first (red)
        Agent->>Agent: Implement minimum production code (green)
        Agent->>Agent: Refactor and run spotless
        Agent->>Build: Run mvn verify (current + all prior steps)
        Build-->>Agent: Gate pass/fail
        alt Gate fails
            Agent->>Agent: Fix code/tests and re-run gate
        else Gate passes
            Agent->>Repo: Commit step
        end
    end
    end

    Note over Agent,Tests: Step 4.1: PlatformDetector (linux-x64 correctness gate in this phase)
    Note over Agent,Tests: Step 4.2: NativeRuntimeLoader (linux-x64 extraction/cache now; uber-jar multi-platform readiness noted for future)
    Note over Agent,Tests: Step 4.3: NativeBinding + JnaNativeBinding + OutboundCallback
    Note over Agent,Tests: Step 4.4: FfiRuntimeHost lifecycle + callback drain + write/close safety
    Note over Agent,Tests: Step 4.5: RuntimeConnection integration in CopilotClient + env var resolution + compatibility validation

    Agent->>Build: Execute Step 4.6a (reactor restructure)
    Build-->>Agent: Parent/sdk module structure green

    Agent->>Build: Execute Step 4.6b (copilot-native packaging)
    Note over Agent,Build: Current phase: package linux-x64 classifier only; verify SHA-512 for linux-x64 tarball.
    Build-->>Agent: linux-x64 classifier JAR produced

    alt Step 4.6c requested now
        Agent-->>DRI: Deferred by Phase 4 invariant policy for this round
    else Step 4.6c not required now
        Note over Agent,DRI: Keep 4.6c deferred in this phase.
    end

    Agent->>Tests: Execute Step 4.7 InProcessTransportIT (real runtime.node)
    Tests-->>Agent: In-process linux-x64 flow verified

    Agent->>CI: Update Step 4.8 workflow
    Note over Agent,CI: InProcess CI coverage in this phase: ubuntu-latest (linux-x64) only.
    CI-->>Agent: CI green

    Agent->>Repo: Final Phase 4 ordered changes committed
    Agent->>Repo: Push branch updates
    Repo-->>DRI: Ready for human ordering/flow review
```
