import { defineConfig } from "vitest/config";

const integrationTestTimeout = process.platform === "win32" ? 60000 : 30000;

const isInProcessTransport =
    (process.env.COPILOT_SDK_DEFAULT_CONNECTION ?? "").toLowerCase() === "inprocess";

// TODO(cli-1.0.81-2): under @github/copilot 1.0.81-2 every model-driven turn hangs when the
// runtime is hosted in-process (FFI). The session never reaches idle, so each test fails on
// its own timeout with no error surfaced by the runtime; suites that only exercise RPC
// without a model turn still pass. The same suites pass on the default (stdio) cell on all
// three OSes, and they all passed in-process on 1.0.76-5, so the lost coverage is limited to
// the transport rather than the behavior. Delete this list once a @github/copilot build
// carrying the fix is picked up.
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

export default defineConfig({
    test: {
        globals: true,
        environment: "node",
        testTimeout: integrationTestTimeout,
        hookTimeout: integrationTestTimeout,
        teardownTimeout: 10000,
        isolate: true, // Run each test file in isolation
        pool: "forks", // Use process forking for better isolation
        // Exclude our ad-hoc test files that aren't vitest-based
        exclude: [
            "**/node_modules/**",
            "**/dist/**",
            "**/*.d.ts",
            "**/basic-test.ts", // Old manual test
            ...(isInProcessTransport ? inProcessBlockedE2E : []),
        ],
    },
});
