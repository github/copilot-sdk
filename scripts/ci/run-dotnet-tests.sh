#!/usr/bin/env bash
# Copyright (c) Microsoft Corporation. All rights reserved.
# Runs the requested .NET SDK test shard with optional backend filtering.

set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$script_dir/../../dotnet"

args=(
    test/GitHub.Copilot.SDK.Test.csproj
    --no-build
    -v n
    --blame-hang
    --blame-hang-timeout 10m
    --blame-hang-dump-type none
    --logger "trx;LogFilePrefix=test-results"
    --results-directory TestResults
    -p:RunAnalyzers=false
)
filter="${DOTNET_TEST_FILTER:-}"
shard="${DOTNET_TEST_SHARD:-full}"
runtime="${DOTNET_TEST_RUNTIME:-}"

if [[ "$shard" != "full" ]]; then
    case "$shard" in
        1)
            initials="A C D H I J K L M N Q S U W Y"
            shard_filter="FullyQualifiedName~GitHub.Copilot.Test.ConnectionToken"
            ;;
        2)
            initials="B E F G O P R T V X Z"
            shard_filter=""
            ;;
        2a)
            initials="B E F G"
            shard_filter=""
            ;;
        2b-pending)
            initials=""
            shard_filter="FullyQualifiedName~GitHub.Copilot.Test.E2E.PendingWorkResumeE2ETests"
            ;;
        2b-permission)
            initials=""
            shard_filter="FullyQualifiedName~GitHub.Copilot.Test.E2E.PermissionE2ETests"
            ;;
        2b-auth)
            initials=""
            shard_filter="FullyQualifiedName~GitHub.Copilot.Test.E2E.PerSessionAuthE2ETests"
            ;;
        2b-hooks)
            initials=""
            shard_filter="FullyQualifiedName~GitHub.Copilot.Test.E2E.PreMcpToolCallHookE2ETests"
            ;;
        2b-unit-p)
            initials=""
            shard_filter="FullyQualifiedName~GitHub.Copilot.Test.Unit.P"
            ;;
        2b-provider)
            initials="O"
            shard_filter="FullyQualifiedName~GitHub.Copilot.Test.E2E.ProviderEndpointE2ETests|FullyQualifiedName~GitHub.Copilot.Test.E2E.RewindE2ETests|FullyQualifiedName~GitHub.Copilot.Test.Unit.R"
            ;;
        2b-rpc-additional)
            initials=""
            shard_filter="FullyQualifiedName~GitHub.Copilot.Test.E2E.RpcAdditionalEdgeCasesE2ETests"
            ;;
        2b-rpc-agent)
            initials=""
            shard_filter="FullyQualifiedName~GitHub.Copilot.Test.E2E.RpcAgentE2ETests"
            ;;
        2b-rpc-event-log)
            initials=""
            shard_filter="FullyQualifiedName~GitHub.Copilot.Test.E2E.RpcEventLogE2ETests"
            ;;
        2b-rpc-event-effects)
            initials=""
            shard_filter="FullyQualifiedName~GitHub.Copilot.Test.E2E.RpcEventSideEffectsE2ETests"
            ;;
        2b-rpc-mcp-skills)
            initials=""
            shard_filter="FullyQualifiedName~GitHub.Copilot.Test.E2E.RpcMcpAndSkillsE2ETests"
            ;;
        2b-rpc-mcp-config)
            initials=""
            shard_filter="FullyQualifiedName~GitHub.Copilot.Test.E2E.RpcMcpConfigE2ETests"
            ;;
        2b-rpc-mcp-lifecycle)
            initials=""
            shard_filter="FullyQualifiedName~GitHub.Copilot.Test.E2E.RpcMcpLifecycleE2ETests"
            ;;
        2b-rpc-q-z)
            initials=""
            shard_filter="FullyQualifiedName~GitHub.Copilot.Test.E2E.RpcQueueE2ETests|FullyQualifiedName~GitHub.Copilot.Test.E2E.RpcRemoteE2ETests|FullyQualifiedName~GitHub.Copilot.Test.E2E.RpcScheduleE2ETests|FullyQualifiedName~GitHub.Copilot.Test.E2E.RpcServerE2ETests|FullyQualifiedName~GitHub.Copilot.Test.E2E.RpcServerMiscE2ETests|FullyQualifiedName~GitHub.Copilot.Test.E2E.RpcServerPluginsE2ETests|FullyQualifiedName~GitHub.Copilot.Test.E2E.RpcServerRemoteControlE2ETests|FullyQualifiedName~GitHub.Copilot.Test.E2E.RpcSessionStateE2ETests|FullyQualifiedName~GitHub.Copilot.Test.E2E.RpcSessionStateExtrasE2ETests|FullyQualifiedName~GitHub.Copilot.Test.E2E.RpcShellAndFleetE2ETests|FullyQualifiedName~GitHub.Copilot.Test.E2E.RpcShellEdgeCaseE2ETests|FullyQualifiedName~GitHub.Copilot.Test.E2E.RpcShellUserRequestedE2ETests|FullyQualifiedName~GitHub.Copilot.Test.E2E.RpcTasksAndHandlersE2ETests|FullyQualifiedName~GitHub.Copilot.Test.E2E.RpcUiEphemeralQueryE2ETests|FullyQualifiedName~GitHub.Copilot.Test.E2E.RpcWorkspaceCheckpointsE2ETests"
            ;;
        extensions)
            initials=""
            shard_filter="FullyQualifiedName~GitHub.Copilot.Test.E2E.RpcExtensionsLoadedE2ETests"
            ;;
        2c)
            initials="T V X Z"
            shard_filter=""
            ;;
        *)
            echo "Unknown .NET test shard: $shard" >&2
            exit 2
            ;;
    esac

    for namespace in E2E Unit; do
        for initial in $initials; do
            clause="FullyQualifiedName~GitHub.Copilot.Test.${namespace}.${initial}"
            shard_filter="${shard_filter:+${shard_filter}|}${clause}"
        done
    done
    filter="${filter:+(${filter})&}(${shard_filter})"
fi

if [[ -n "$filter" ]]; then
    args+=(--filter "$filter")
fi
if [[ -n "$runtime" ]]; then
    args+=(--runtime "$runtime")
fi

dotnet test "${args[@]}"
