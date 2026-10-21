#!/usr/bin/env bash
# Copyright (c) GitHub, Inc.
# Licensed under the MIT License.

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/lib/github-actions-apt.sh"

# ---------------------------------------------------------------------------
# Install bubblewrap and its schema 0.8 proxy-mode dependencies
#
# The sandbox E2E/Rust tests need a runnable `bwrap` on Linux, and the runner
# images do not ship it. Sandbox E2E files fail without it; before that, the
# whole suite silently skipped and a Linux-only regression merged green (#14647).
#
# Every apt invocation is wrapped in `timeout` and retried, and the script as a
# whole is capped at TOTAL_TIMEOUT. GitHub's Ubuntu runners also bypass their
# flaky Azure mirror with process-local source files. Unbounded apt is not
# hypothetical here: an `apt-get update` that stalled mid-fetch hung an
# executable-shard-linux job for the full 6h runner cap before the tests ever
# started. The cap lives here rather than in the workflow because the callers
# are composite actions, whose steps cannot carry `timeout-minutes`.
# ---------------------------------------------------------------------------

usage() {
  cat <<HELP
Install bubblewrap and the tools required by schema 0.8 proxy mode, then verify
bubblewrap can create a sandbox.

Usage:
  ./script/install-bubblewrap.sh [--run] [--help]

Safe by default: without --run, previews the bounded apt-get commands without
changing the host. Also honors RUN=1. No-ops on non-Linux hosts.
Requires passwordless sudo (as on CI runners) when run.

When BUBBLEWRAP_DEB_CACHE is set, first verify every .deb against SHA-256 digests
in the runner's trusted APT indexes, then try installing the requested packages
without network access or an index refresh. Missing package metadata, a digest
mismatch, or an unusable cache falls back to the normal bounded install,
replacing unusable archives with fresh downloads. Cached manifests are not used
as a source of trusted digests.
Use a cache scoped to the same runner image version and architecture; sandbox
and proxy readiness checks still run on every invocation. On GitHub Actions,
cache-refreshed=true marks a network install for the setup action to publish
under a new cache generation only after verification succeeds.
If the cache directory cannot be created, entered, read, or written, warn and
use APT's default archive directory without publishing a cache.

An apt error (status 100) during 'apt-get update' is a warning, not a failure: a
broken index in one of the runner image's third-party repositories fails the
whole update while the Ubuntu suites fetch fine. 'apt-get install' is the gate
instead. Any other status comes from 'timeout' rather than apt, and still
abandons the attempt.

Environment variables:
  TOTAL_TIMEOUT     Overall budget for the whole script (default: 10m)
  ATTEMPTS          Number of install attempts before giving up (default: 3)
  UPDATE_TIMEOUT    Timeout for each 'apt-get update' (default: 2m)
  INSTALL_TIMEOUT   Timeout for each 'apt-get install' (default: 3m)
  BUBBLEWRAP_DEB_CACHE  Optional directory for cached .deb packages
HELP
}

verify_cached_debs() {
  local archive package version architecture metadata hashes checksum
  for archive in "$@"; do
    package="$(dpkg-deb --field "$archive" Package)" || return 1
    version="$(dpkg-deb --field "$archive" Version)" || return 1
    architecture="$(dpkg-deb --field "$archive" Architecture)" || return 1
    if [[ ! "$package" =~ ^[a-z0-9][a-z0-9.+-]*$ || ! "$version" =~ ^[0-9][A-Za-z0-9.+:~-]*$ \
      || ! "$architecture" =~ ^[a-z0-9][a-z0-9-]*$ ]]; then
      return 1
    fi
    # APT's archive fast path checks only size. Bind the verified payload to the
    # cache filename APT selects, including its percent-escaped version epoch.
    if [[ "${archive##*/}" != "${package}_${version//:/%3a}_${architecture}.deb" ]]; then
      return 1
    fi
    metadata="$(apt-cache show -- "$package=$version")" || return 1
    hashes="$(printf '%s\n' "$metadata" | awk '$1 == "SHA256:" { print $2 }')" || return 1
    [[ -n "$hashes" ]] || return 1
    checksum="$(sha256sum -- "$archive")" || return 1
    if ! printf '%s\n' "$hashes" | grep -Fx -- "${checksum%% *}" >/dev/null; then
      return 1
    fi
  done
}

RUN="${RUN:-}"
for arg in "$@"; do
  case "$arg" in
    -h | --help)
      usage
      exit 0
      ;;
    --run) RUN=1 ;;
    *)
      echo "unknown argument: $arg" >&2
      usage >&2
      exit 2
      ;;
  esac
done

TOTAL_TIMEOUT="${TOTAL_TIMEOUT:-10m}"
ATTEMPTS="${ATTEMPTS:-3}"
UPDATE_TIMEOUT="${UPDATE_TIMEOUT:-2m}"
INSTALL_TIMEOUT="${INSTALL_TIMEOUT:-3m}"

if [ "$(uname -s)" != "Linux" ]; then
  echo "Not Linux, nothing to do."
  exit 0
fi

if [[ "$RUN" != 1 ]]; then
  cat <<PREVIEW
[dry-run] If BUBBLEWRAP_DEB_CACHE contains .deb packages, verify their SHA-256
digests against trusted APT indexes, then try a bounded offline install of the
requested packages. Otherwise, or if verification or installation fails,
install bubblewrap and its proxy-mode dependencies via up to ${ATTEMPTS} bounded attempts of:
  apt-get update -o Acquire::Retries=3
  apt-get install -y --no-install-recommends -o Acquire::Retries=3 bubblewrap slirp4netns util-linux iptables
After installation, the script verifies the proxy tools, util-linux namespace
features, and /dev/net/tun access required by MXC.
Re-run with --run (or RUN=1) to install it. On GitHub's Ubuntu runners only,
apt uses temporary source files that replace the runner's Azure mirror with
https://archive.ubuntu.com.
PREVIEW
  exit 0
fi

# Cap the whole script, not just the individual apt calls: retries and backoff
# would otherwise let the worst case add up well past any single timeout. The
# re-exec keeps this parent alive purely so it can turn `timeout`'s bare 124
# into an explanatory annotation.
if [ -z "${BUBBLEWRAP_INSTALL_WRAPPED:-}" ]; then
  export BUBBLEWRAP_INSTALL_WRAPPED=1
  status=0
  timeout "$TOTAL_TIMEOUT" "$0" --run || status=$?
  if [ "$status" -eq 124 ]; then
    echo "::error::bubblewrap install exceeded its ${TOTAL_TIMEOUT} budget"
  fi
  exit "$status"
fi

export DEBIAN_FRONTEND=noninteractive

trap cleanup_github_actions_apt_sources EXIT
packages=(bubblewrap slirp4netns util-linux iptables)
apt_cache_options=()
installed_from_cache=0
if [[ -n "${BUBBLEWRAP_DEB_CACHE:-}" ]]; then
  if ! mkdir -p "$BUBBLEWRAP_DEB_CACHE" \
    || ! BUBBLEWRAP_DEB_CACHE="$(cd -- "$BUBBLEWRAP_DEB_CACHE" && pwd)" \
    || [[ ! -r "$BUBBLEWRAP_DEB_CACHE" || ! -w "$BUBBLEWRAP_DEB_CACHE" || ! -x "$BUBBLEWRAP_DEB_CACHE" ]]; then
    echo "::warning::Bubblewrap package cache is unavailable; using APT's default archive directory."
    BUBBLEWRAP_DEB_CACHE=""
  fi
fi
if [[ -n "${BUBBLEWRAP_DEB_CACHE:-}" ]]; then
  apt_cache_options=(
    -o "Dir::Cache::archives=$BUBBLEWRAP_DEB_CACHE"
    -o APT::Keep-Downloaded-Packages=true
  )
  shopt -s nullglob
  cached_debs=("$BUBBLEWRAP_DEB_CACHE/"*.deb)
  shopt -u nullglob
  if [[ "${#cached_debs[@]}" -gt 0 ]]; then
    echo "::group::Install cached bubblewrap packages (offline)"
    status=0
    if verify_cached_debs "${cached_debs[@]}"; then
      timeout "$INSTALL_TIMEOUT" sudo apt-get install -y --no-install-recommends --no-download \
        "${apt_cache_options[@]}" "${packages[@]}" || status=$?
    else
      echo "::warning::Cached bubblewrap packages failed repository checksum verification."
      status=1
    fi
    echo "::endgroup::"
    if [[ "$status" -eq 0 ]]; then
      installed_from_cache=1
    else
      echo "::warning::Cached bubblewrap packages could not be installed (exit ${status}); falling back to apt."
      rm -f -- "${cached_debs[@]}"
    fi
  fi
fi

if [[ "$installed_from_cache" -eq 0 ]]; then
  configure_github_actions_apt_sources

  for attempt in $(seq 1 "$ATTEMPTS"); do
    echo "::group::Install bubblewrap attempt ${attempt}/${ATTEMPTS}"
    status=0
    # Acquire::Retries lets apt recover from a single flaky mirror on its own;
    # the outer `timeout` is the backstop for a connection that hangs rather than
    # fails, which retries alone never escape.
    update_status=0
    # `${a[@]+"${a[@]}"}`, not `"${a[@]}"` or `"${a[@]-}"`: under `set -u` bash 3.2
    # -- still /bin/bash on macOS -- treats an empty array as unset and aborts,
    # while the `-` fallback contributes one empty word. `apt-get update` rejects
    # that stray operand with `E: The update command takes no arguments` and exit
    # 100, which the case below would misread as a broken third-party index.
    timeout "$UPDATE_TIMEOUT" sudo apt-get update -o Acquire::Retries=3 \
      ${APT_SOURCE_OPTIONS[@]+"${APT_SOURCE_OPTIONS[@]}"} || update_status=$?
    case "$update_status" in
      0) ;;
      # apt-get reports every error as 100. A broken index in one of the runner
      # image's third-party repositories (Google Chrome, Microsoft) fails the whole
      # update even when every Ubuntu suite fetched fine and apt kept the indexes
      # it already had, so let the install be the gate: it still fails loudly when
      # the packages are genuinely unreachable.
      100)
        echo "::warning::apt-get update failed; installing with the indexes apt already has."
        ;;
      # Any other status did not come from apt: a budget kill (124), a duration
      # `timeout` rejected (125), a command it could not run (126/127), or an
      # external SIGKILL (137 -- this call sets no --kill-after, so `timeout` never
      # escalates on its own). None of those leave the indexes in a known state, so
      # abandon the attempt.
      *) status=$update_status ;;
    esac
    if [ "$status" -eq 0 ]; then
      timeout "$INSTALL_TIMEOUT" sudo apt-get install -y --no-install-recommends \
        -o Acquire::Retries=3 ${APT_SOURCE_OPTIONS[@]+"${APT_SOURCE_OPTIONS[@]}"} \
        ${apt_cache_options[@]+"${apt_cache_options[@]}"} "${packages[@]}" || status=$?
    fi
    echo "::endgroup::"

    if [ "$status" -eq 0 ]; then
      break
    fi
    if [ "$attempt" -eq "$ATTEMPTS" ]; then
      echo "::error::bubblewrap install failed after ${ATTEMPTS} attempts (last exit ${status})"
      exit "$status"
    fi
    sleep $((attempt * 10))
  done
  if [[ -n "${BUBBLEWRAP_DEB_CACHE:-}" && -n "${GITHUB_OUTPUT:-}" ]]; then
    echo "cache-refreshed=true" >> "$GITHUB_OUTPUT"
  fi
fi

# Ubuntu restricts unprivileged user namespaces via AppArmor, which bubblewrap
# needs; best-effort since not every image enforces it.
sudo sysctl -w kernel.apparmor_restrict_unprivileged_userns=0 || true

bwrap --ro-bind / / --dev /dev true

for command in slirp4netns unshare nsenter iptables ip6tables; do
  if ! command -v "$command" >/dev/null; then
    echo "::error::bubblewrap proxy dependency is unavailable after installation: ${command}"
    exit 1
  fi
done

verify_proxy_command() {
  local command="$1"
  shift
  if ! "$command" "$@" >/dev/null 2>&1; then
    echo "::error::bubblewrap proxy dependency failed its probe: ${command} $*"
    exit 1
  fi
}

verify_proxy_command slirp4netns --version
verify_proxy_command nsenter --version
verify_proxy_command iptables --version
verify_proxy_command ip6tables --version

if ! unshare_help="$(unshare --help 2>&1)"; then
  echo "::error::bubblewrap proxy dependency failed its probe: unshare --help"
  exit 1
fi
for option in --map-current-user --keep-caps; do
  if [[ "$unshare_help" != *"$option"* ]]; then
    echo "::error::bubblewrap proxy mode requires util-linux unshare with ${option} support"
    exit 1
  fi
done

if [[ ! -e /dev/net/tun ]] && command -v modprobe >/dev/null; then
  sudo modprobe tun || true
fi
if [[ ! -c /dev/net/tun || ! -r /dev/net/tun || ! -w /dev/net/tun ]]; then
  echo "::error::bubblewrap proxy mode requires read/write access to /dev/net/tun"
  exit 1
fi
