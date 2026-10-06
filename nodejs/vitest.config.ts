import { defineConfig } from "vitest/config";
import { isByokBackend } from "./test/e2e/harness/testBackend";

const integrationTestTimeout = process.platform === "win32" ? 60000 : 30000;

const isInProcessTransport =
    (process.env.COPILOT_SDK_DEFAULT_CONNECTION ?? "").toLowerCase() === "inprocess";

// TODO(cli-1.0.81-2): CLI 1.0.81-5 still stops completing model-driven turns when
// hosted in-process against CAPI. The shared runtime then poisons every later
// model-driven test. These suites still run over stdio on all three OSes, while
// pure-RPC in-process coverage remains enabled.
const inProcessBlockedE2E = [
    "**/test/e2e/abort.e2e.test.ts",
    "**/test/e2e/agent_and_compact_rpc.e2e.test.ts",
    "**/test/e2e/ask_user.e2e.test.ts",
    "**/test/e2e/builtin_tools.e2e.test.ts",
    "**/test/e2e/client_api.e2e.test.ts",
    "**/test/e2e/client_lifecycle.e2e.test.ts",
    "**/test/e2e/compaction.e2e.test.ts",
    "**/test/e2e/copilot_request_cancel_error.e2e.test.ts",
    "**/test/e2e/copilot_request_handler.e2e.test.ts",
    "**/test/e2e/copilot_request_session_id.e2e.test.ts",
    "**/test/e2e/disabled_mcp_servers.e2e.test.ts",
    "**/test/e2e/event_fidelity.e2e.test.ts",
    "**/test/e2e/hooks.e2e.test.ts",
    "**/test/e2e/hooks_extended.e2e.test.ts",
    "**/test/e2e/mcp_and_agents.e2e.test.ts",
    "**/test/e2e/mcp_instruction_recovery.e2e.test.ts",
    "**/test/e2e/memory_retrieval.e2e.test.ts",
    "**/test/e2e/mode_empty.e2e.test.ts",
    "**/test/e2e/multi_turn.e2e.test.ts",
    "**/test/e2e/permissions.e2e.test.ts",
    "**/test/e2e/pre_mcp_tool_call_hook.e2e.test.ts",
    "**/test/e2e/provider_endpoint.e2e.test.ts",
    "**/test/e2e/rewind.e2e.test.ts",
    "**/test/e2e/rpc_event_side_effects.e2e.test.ts",
    "**/test/e2e/rpc_session_state.e2e.test.ts",
    "**/test/e2e/rpc_session_state_extras.e2e.test.ts",
    "**/test/e2e/rpc_shell_and_fleet.e2e.test.ts",
    "**/test/e2e/session.e2e.test.ts",
    "**/test/e2e/session_config.e2e.test.ts",
    "**/test/e2e/session_fs.e2e.test.ts",
    "**/test/e2e/session_fs_sqlite.e2e.test.ts",
    "**/test/e2e/session_lifecycle.e2e.test.ts",
    "**/test/e2e/session_todos_changed.e2e.test.ts",
    "**/test/e2e/skills.e2e.test.ts",
    "**/test/e2e/streaming_fidelity.e2e.test.ts",
    "**/test/e2e/subagent_hooks.e2e.test.ts",
    "**/test/e2e/suspend.e2e.test.ts",
    "**/test/e2e/system_message_sections.e2e.test.ts",
    "**/test/e2e/system_message_transform.e2e.test.ts",
    "**/test/e2e/tool_results.e2e.test.ts",
    "**/test/e2e/tools.e2e.test.ts",
];

// These suites own their provider or CAPI auth/request-handler setup. They run
// in the ordinary CAPI job, not as repetitions of the matrix-selected backend.
const selfConfiguredBackendE2E = [
    "**/test/e2e/byok_bearer_token_provider.e2e.test.ts",
    "**/test/e2e/copilot_request_handler.e2e.test.ts",
    "**/test/e2e/copilot_request_session_id.e2e.test.ts",
    "**/test/e2e/disabled_mcp_servers.e2e.test.ts",
    "**/test/e2e/mcp_cached_oauth_overlap.e2e.test.ts",
    "**/test/e2e/mcp_catalog_recovery.e2e.test.ts",
    "**/test/e2e/memory_retrieval.e2e.test.ts",
    "**/test/e2e/mode_handlers.e2e.test.ts",
    "**/test/e2e/multi_provider_registry.e2e.test.ts",
    "**/test/e2e/per_session_auth.e2e.test.ts",
    "**/test/e2e/provider_endpoint.e2e.test.ts",
    "**/test/e2e/subagent_prompt_resume.e2e.test.ts",
    "**/test/e2e/websocket_terminals.e2e.test.ts",
];

export default defineConfig({
    test: {
        ...(isByokBackend ? { include: ["test/e2e/*.e2e.test.ts"] } : {}),
        globals: true,
        environment: "node",
        testTimeout: integrationTestTimeout,
        hookTimeout: integrationTestTimeout,
        teardownTimeout: 10000,
        isolate: true, // Run each test file in isolation
        pool: "forks", // Use process forking for better isolation
        globalSetup: ["./test/e2e/harness/globalSetup.ts"],
        // Exclude our ad-hoc test files that aren't vitest-based
        exclude: [
            "**/node_modules/**",
            "**/dist/**",
            "**/*.d.ts",
            "**/basic-test.ts", // Old manual test
            ...(isInProcessTransport ? inProcessBlockedE2E : []),
            ...(isByokBackend ? selfConfiguredBackendE2E : []),
        ],
    },
});
