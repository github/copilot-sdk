# Bundled runtime installation memory

`examples/runtime_install.rs` exercises the public `install_bundled_runtime()`
API in a standalone process. It also checks that a second call returns the
same path. It does not launch the runtime, read credentials, authenticate, or
make service requests. Build-time downloads use the SDK's normal verified
public release artifacts.

## Reproduce on macOS

Requires Python 3.11+, the pinned Rust toolchain, and macOS's `/usr/bin/time`.
From `rust/` on the fixed revision:

```sh
work=$(mktemp -d)
git worktree add --detach "$work/baseline" a675b55531a9dfc647ee015e32d74279568550f3
cp examples/runtime_install.rs "$work/baseline/rust/examples/runtime_install.rs"
(
  cd "$work/baseline/rust"
  CARGO_PROFILE_RELEASE_DEBUG=1 cargo build --locked --release --example runtime_install
)
CARGO_PROFILE_RELEASE_DEBUG=1 cargo build --locked --release --example runtime_install

python3 benchmarks/runtime_install.py \
  "$work/baseline/rust/target/release/examples/runtime_install" --runs 5 \
  > "$work/baseline.json"
python3 benchmarks/runtime_install.py \
  target/release/examples/runtime_install --runs 5 > "$work/fixed.json"
```

Both builds must use the same runtime version, feature set, toolchain and
profile. These commands use default features (`bundled-cli`), release
optimization level 3 and debug level 1. Check the SHA-256 of each build's
`target/release/build/github-copilot-sdk-*/out/copilot_runtime.archive`; the
hashes must agree. Do not compare binaries with different runtime releases.

Every measured run gets a fresh process and temporary `HOME`, including the
platform cache. The environment contains only `HOME`, `TMPDIR`, and a system
`PATH`. For warm/repair runs, a separate, unmeasured installer process seeds
that home before the measured process starts. Cohorts are:

- **Cold:** no installed files.
- **Warm:** all installed files already match.
- **Corrupt:** flip the final byte of installed `runtime.node`, retaining its size.
- **Truncated:** truncate installed `runtime.node` to 1,024 bytes.

Each process waits before installation and remains alive for one second
after installation so the harness can observe retained memory. The harness
then lets it exit. Output sizes, permissions and streaming SHA-256 hashes
are collected outside the measured process after exit; every run must produce
the same file inventory. The JSON contains only relative output paths.
Temporary homes are removed after each run. Compare the two JSON `outputs`
objects for exact equality, not just the runtime file:

```sh
python3 - "$work/baseline.json" "$work/fixed.json" <<'PY'
import json, sys
before, after = [json.load(open(path)) for path in sys.argv[1:]]
assert before["outputs"] == after["outputs"]
print(len(after["outputs"]), "identical installed files")
PY
```

## Measurement definitions

**Peak RSS** and **peak physical footprint** are the process-lifetime kernel
high-water marks reported by `/usr/bin/time -l`. **Retained RSS** and
**retained physical footprint** are `proc_pid_rusage(RUSAGE_INFO_V0)`'s
`ri_resident_size` and `ri_phys_footprint`, sampled after the one-second idle
window. They are total process values, not baseline-subtracted allocations.
RSS includes resident file-backed pages such as the embedded compressed
archive; physical footprint is the kernel's charged-memory accounting and
is not interchangeable with RSS or live heap size.

Elapsed time comes from Rust's `Instant` around the first installation call,
excluding startup, the second cached call, idle waits, and output hashing.
Runs are independent processes on a shared host, not CPU-isolated trials.
"Cold" means an empty installation cache, not flushed filesystem pages.
Small samples and environmental noise limit latency conclusions.

## Observed before and after

Measured on 2026-09-15 with an Apple M4 Pro, 48 GiB RAM, macOS 26.6.2
(25G83), `aarch64-apple-darwin`, rustc 1.94.0
(`4a4ef493e`, LLVM 21.1.8). Baseline SDK revision:
`a675b55531a9dfc647ee015e32d74279568550f3` (`0.0.0-dev`).
Installer-only fixed revision:
`883edfbf89c04fb5165649abc9bda7efe8e719bd`.
The fixed build changes only the runtime installer; runtime version,
dependencies, release profile and probe source are identical.

Five runs per cohort per build. Values are **median (minimum-maximum)**.
Memory uses MiB (1,048,576 bytes).

| Cohort | Before retained physical MiB | After retained physical MiB | Before seconds | After seconds |
| --- | --- | --- | --- | --- |
| Cold | 155.438 (154.563-156.047) | 2.000 (1.969-2.078) | 0.880 (0.863-1.037) | 0.633 (0.618-0.703) |
| Warm | 174.360 (172.735-174.907) | 1.938 (1.875-1.953) | 1.031 (0.997-1.092) | 0.747 (0.359-0.788) |
| Corrupt | 174.376 (171.438-174.485) | 2.063 (2.000-2.079) | 1.043 (1.018-1.075) | 1.007 (0.991-1.092) |
| Truncated | 174.376 (172.938-174.422) | 1.907 (1.891-1.907) | 1.038 (0.978-1.090) | 0.830 (0.797-0.863) |

| Cohort | Before peak physical MiB | After peak physical MiB | Before peak RSS MiB | After peak RSS MiB |
| --- | --- | --- | --- | --- |
| Cold | 155.469 (154.594-156.079) | 2.032 (2.000-2.110) | 200.109 (199.219-200.703) | 46.766 (46.734-46.844) |
| Warm | 174.391 (172.766-174.938) | 1.969 (1.907-1.985) | 219.016 (217.391-219.547) | 46.688 (46.641-46.719) |
| Corrupt | 174.407 (171.469-174.516) | 2.094 (2.032-2.110) | 219.016 (216.078-219.125) | 46.812 (46.734-46.812) |
| Truncated | 174.407 (172.969-174.454) | 1.938 (1.922-1.938) | 219.031 (217.578-219.062) | 46.672 (46.641-46.672) |

| Cohort | Before retained RSS MiB | After retained RSS MiB |
| --- | --- | --- |
| Cold | 200.078 (199.188-200.672) | 46.719 (46.688-46.797) |
| Warm | 218.984 (217.359-219.516) | 46.641 (46.594-46.672) |
| Corrupt | 218.984 (216.047-219.094) | 46.766 (46.688-46.766) |
| Truncated | 219.000 (217.547-219.031) | 46.625 (46.594-46.625) |

The baseline/fixed median initial physical footprints were approximately
1.58/1.58 MiB before installation. All 68 output files were byte-identical,
with the same sizes and modes, across both builds and all cohorts.

| Input/output | Identity |
| --- | --- |
| Public runtime release | `github/copilot-cli` `v1.0.84-8`, `github-copilot-1.0.84-8-darwin-arm64.tgz` |
| Filtered embedded runtime archive SHA-256 | `6c43b789080fc06b25d406af8fae709daa99f0724c4d290cc8a31160c5a3ad64` |
| Installed `runtime.node` | 71,166,736 bytes; mode `0755`; SHA-256 `839cd681c72cb92f27697d5e3e3ee96d7bb8df4234ffb5f7b442956828ec173a` |
| Installed `copilot-runtime` | 386,992 bytes; mode `0755`; SHA-256 `b1bb3f4b9f6ee4c4d72eb206e68647fe0716a0d87e874472b411f4abc5608a8f` |
| Total installed output | 68 files; 95,721,242 bytes |
| Complete inventory SHA-256 | `dde1d211fd60d155cd5ec647ee0f5123371498a2e65cb9a6c52675b1b711ce17` |
| Baseline probe binary SHA-256 | `2ad5536afcc8052ad15d2af726cccd86b503f06068b690be7c6268e5a7557bb1` |
| Fixed probe binary SHA-256 | `4f37ab4cd9d5e6a5ece13900c85f6d6858732cfce1c80f3f2e47953edecabc2d` |

The inventory digest hashes UTF-8
`json.dumps(outputs, sort_keys=True, separators=(",", ":"))`.
Probe binary hashes identify the measured executables, not reproducible-build
expectations: build paths and debug information may differ on another host.
Output file sizes are identity checks, not memory measurements.

## Separate allocation diagnostics

Run profiling separately from the comparison above:

```sh
python3 benchmarks/runtime_install.py target/release/examples/runtime_install \
  --runs 1 --cohort warm --diagnostics "$work/fixed-diagnostics" \
  > "$work/fixed-instrumented.json"
```

This enables `MallocStackLogging` and `MallocStackLoggingNoCompact` and saves
`vmmap -summary`, live allocations, and allocation history. These tools can
require local profiling permission. Raw diagnostics may contain local paths;
keep them local rather than attaching them to an issue or PR.

In a separate baseline warm run, allocation history recorded two
71,172,096-byte VM allocations through `embeddedcli::install_runtime`, one
also through `std::fs::read`. The size is the page-rounded native runtime
payload. After installation, `vmmap` reported 165.7 MiB in
`MALLOC_LARGE (empty)` regions. These were freed allocations retained by the
allocator, not evidence of a live-object leak. The fixed warm diagnostic
did not contain either runtime-sized allocation or a `MALLOC_LARGE (empty)`
region. Instrumented memory/timing values are not included in the tables.

The fix uses bounded entry copying and 64 KiB comparison buffers. Cold and
valid warm installs traverse the archive once. Same-size corrupt files can
require one additional traversal to recover bytes consumed during comparison.
Warm verification does not stage or write matching files, so valid read-only
caches remain usable. Changed files require temporary disk space until
archive validation completes; publication is atomic per file, not for the
whole bundle. Abrupt process termination can leave temporary files, as before.

These measurements cover only bundled runtime installation on macOS arm64.
They do not measure full CLI installation, authentication, model sessions,
in-process runtime loading, or an application's overall performance.
Native Windows, Linux, and other architecture measurements were not available.
