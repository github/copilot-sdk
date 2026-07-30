/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.generated.rpc.AccountQuotaSnapshot;
import com.github.copilot.generated.rpc.AgentsDiscoverParams;
import com.github.copilot.generated.rpc.AgentsGetDiscoveryPathsParams;
import com.github.copilot.generated.rpc.InstructionsDiscoverParams;
import com.github.copilot.generated.rpc.InstructionsGetDiscoveryPathsParams;
import com.github.copilot.generated.rpc.LlmInferenceHttpResponseChunkError;
import com.github.copilot.generated.rpc.LlmInferenceHttpResponseChunkParams;
import com.github.copilot.generated.rpc.LlmInferenceHttpResponseStartParams;
import com.github.copilot.generated.rpc.LocalSessionMetadataValue;
import com.github.copilot.generated.rpc.McpDiscoverParams;
import com.github.copilot.generated.rpc.PingParams;
import com.github.copilot.generated.rpc.SecretsAddFilterValuesParams;
import com.github.copilot.generated.rpc.ServerSkill;
import com.github.copilot.generated.rpc.SessionContext;
import com.github.copilot.generated.rpc.SessionFsSetProviderCapabilities;
import com.github.copilot.generated.rpc.SessionFsSetProviderConventions;
import com.github.copilot.generated.rpc.SessionFsSetProviderParams;
import com.github.copilot.generated.rpc.SessionsBulkDeleteParams;
import com.github.copilot.generated.rpc.SessionsCheckInUseParams;
import com.github.copilot.generated.rpc.SessionsCloseParams;
import com.github.copilot.generated.rpc.SessionsConnectParams;
import com.github.copilot.generated.rpc.SessionsEnrichMetadataParams;
import com.github.copilot.generated.rpc.SessionsFindByPrefixParams;
import com.github.copilot.generated.rpc.SessionsFindByTaskIdParams;
import com.github.copilot.generated.rpc.SessionsGetEventFilePathParams;
import com.github.copilot.generated.rpc.SessionsGetLastForContextParams;
import com.github.copilot.generated.rpc.SessionsGetPersistedRemoteSteerableParams;
import com.github.copilot.generated.rpc.SessionsLoadDeferredRepoHooksParams;
import com.github.copilot.generated.rpc.SessionsPruneOldParams;
import com.github.copilot.generated.rpc.SessionsReleaseLockParams;
import com.github.copilot.generated.rpc.SessionsReloadPluginHooksParams;
import com.github.copilot.generated.rpc.SessionsSaveParams;
import com.github.copilot.generated.rpc.SessionsSetAdditionalPluginsParams;
import com.github.copilot.generated.rpc.SkillsConfigSetDisabledSkillsParams;
import com.github.copilot.generated.rpc.SkillsDiscoverParams;
import com.github.copilot.generated.rpc.SkillsGetDiscoveryPathsParams;
import com.github.copilot.generated.rpc.ToolsListParams;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.InfiniteSessionConfig;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;

class RpcServerE2ETest {

    private static final long TIMEOUT_SECONDS = 30;
    private static final long SESSION_PERSISTENCE_TIMEOUT_MILLIS = 30_000;
    private static E2ETestContext ctx;

    @BeforeAll
    static void setup() throws Exception {
        ctx = E2ETestContext.create();
    }

    @AfterAll
    static void teardown() throws Exception {
        if (ctx != null) {
            ctx.close();
        }
    }

    @Test
    void testShouldCallRpcPingWithTypedParamsAndResult() throws Exception {
        ctx.configureForTest("rpc_server", "should_call_rpc_ping_with_typed_params_and_result");

        try (var client = ctx.createClient()) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            var result = client.getRpc().ping(new PingParams("typed rpc test")).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertEquals("pong: typed rpc test", result.message());
            assertNotNull(result.timestamp());
            assertNotNull(result.protocolVersion());
            assertTrue(result.protocolVersion() >= 0);
        }
    }

    @Test
    void testShouldRejectLlmInferenceResponseFramesForMissingRequest() throws Exception {
        ctx.initializeProxy();

        try (var client = ctx.createClient()) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var requestId = "missing-llm-inference-request";

            var start = client.getRpc().llmInference
                    .httpResponseStart(new LlmInferenceHttpResponseStartParams(requestId, 200L, "OK",
                            Map.of("content-type", List.of("text/event-stream"))))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertFalse(start.accepted());

            var chunk = client.getRpc().llmInference
                    .httpResponseChunk(
                            new LlmInferenceHttpResponseChunkParams(requestId, "data: {}\n\n", false, false, null))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertFalse(chunk.accepted());

            var error = client.getRpc().llmInference.httpResponseChunk(new LlmInferenceHttpResponseChunkParams(
                    requestId, "", null, true,
                    new LlmInferenceHttpResponseChunkError("No pending LLM inference request.", "missing_request")))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertFalse(error.accepted());
        }
    }

    @Test
    void testShouldCallRpcModelsListWithTypedResult() throws Exception {
        ctx.configureForTest("rpc_server", "should_call_rpc_models_list_with_typed_result");
        var token = "rpc-models-token";
        configureAuthenticatedUser(token, null);

        try (var client = createAuthenticatedClient(token)) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            var result = client.getRpc().models.list().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertNotNull(result.models());
            assertTrue(result.models().stream().anyMatch(model -> "claude-sonnet-4.5".equals(model.id())));
            result.models().forEach(model -> {
                assertFalse(model.id().isBlank());
                assertFalse(model.name().isBlank());
            });
        }
    }

    @Test
    void testShouldCallRpcAccountGetQuotaWhenAuthenticated() throws Exception {
        ctx.configureForTest("rpc_server", "should_call_rpc_account_get_quota_when_authenticated");
        var token = "rpc-quota-token";
        configureAuthenticatedUser(token, Map.of("chat", Map.of("entitlement", 100, "overage_count", 2,
                "overage_permitted", true, "percent_remaining", 75, "timestamp_utc", "2026-04-30T00:00:00Z")));

        try (var client = createAuthenticatedClient(token)) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            var result = client.getRpc().account.getQuota().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertNotNull(result.quotaSnapshots());
            var chatQuota = result.quotaSnapshots().get("chat");
            assertNotNull(chatQuota);
            assertQuota(chatQuota);
        }
    }

    @Test
    void testShouldCallRpcToolsListWithTypedResult() throws Exception {
        ctx.configureForTest("rpc_server", "should_call_rpc_tools_list_with_typed_result");

        try (var client = ctx.createClient()) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            var result = client.getRpc().tools.list(new ToolsListParams(null)).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertNotNull(result.tools());
            assertFalse(result.tools().isEmpty());
            result.tools().forEach(tool -> assertFalse(tool.name().isBlank()));
        }
    }

    @Test
    void testShouldCallRpcSessionFsSetProviderWithTypedResult() throws Exception {
        ctx.initializeProxy();

        try (var client = ctx.createClient()) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            var result = client.getRpc().sessionFs
                    .setProvider(new SessionFsSetProviderParams(ctx.getWorkDir().toString(),
                            ctx.getWorkDir().resolve("session-state").toString(), currentPathConventions(),
                            new SessionFsSetProviderCapabilities(true)))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertTrue(result.success());
        }
    }

    @Test
    void testShouldAddSecretFilterValues() throws Exception {
        ctx.initializeProxy();
        var env = new HashMap<>(ctx.getEnvironment());
        env.put("COPILOT_ENABLE_SECRET_FILTERING", "true");

        try (var client = ctx.createClient(new CopilotClientOptions().setEnvironment(env))) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var secret = "rpc-secret-" + UUID.randomUUID().toString().replace("-", "");

            var result = client.getRpc().secrets.addFilterValues(new SecretsAddFilterValuesParams(List.of(secret)))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertTrue(result.ok());
        }
    }

    @Test
    void testShouldListFindAndInspectPersistedSessionState() throws Exception {
        ctx.initializeProxy();

        try (var client = ctx.createClient()) {
            var requestedSessionId = UUID.randomUUID().toString();
            var workingDirectory = createUniqueWorkDirectory("server-rpc-list");
            var missingTaskId = "missing-task-" + UUID.randomUUID().toString().replace("-", "");
            var missingSessionId = UUID.randomUUID().toString();

            try (var session = client.createSession(persistedSessionConfig(requestedSessionId, workingDirectory))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                var sessionId = session.getSessionId();
                session.log("SERVER_RPC_LIST_READY").get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                saveSession(client, sessionId);
                assertNull(client.getRpc().sessions.close(new SessionsCloseParams(sessionId)).get(TIMEOUT_SECONDS,
                        TimeUnit.SECONDS));

                var listed = client.getRpc().sessions.list().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertNotNull(listed.sessions());

                var byPrefix = client.getRpc().sessions
                        .findByPrefix(new SessionsFindByPrefixParams(sessionId.substring(0, 8)))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertTrue(byPrefix.sessionId() == null || sessionId.equals(byPrefix.sessionId()));

                var byTaskId = client.getRpc().sessions.findByTaskId(new SessionsFindByTaskIdParams(missingTaskId))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertNull(byTaskId.sessionId());

                var lastForContext = client.getRpc().sessions
                        .getLastForContext(new SessionsGetLastForContextParams(
                                new SessionContext(workingDirectory.toString(), null, null, null, null)))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertTrue(lastForContext.sessionId() == null || sessionId.equals(lastForContext.sessionId()));

                var eventFile = client.getRpc().sessions.getEventFilePath(new SessionsGetEventFilePathParams(sessionId))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertTrue(eventFile.filePath().endsWith("events.jsonl"));

                var remoteSteerable = client.getRpc().sessions
                        .getPersistedRemoteSteerable(new SessionsGetPersistedRemoteSteerableParams(sessionId))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertNull(remoteSteerable.remoteSteerable());

                var sizes = client.getRpc().sessions.getSizes().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertNotNull(sizes.sizes());
                if (sizes.sizes().containsKey(sessionId)) {
                    assertTrue(sizes.sizes().get(sessionId) >= 0);
                }

                var inUse = client.getRpc().sessions
                        .checkInUse(new SessionsCheckInUseParams(List.of(sessionId, missingSessionId)))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertNotNull(inUse.inUse());
                assertFalse(inUse.inUse().contains(missingSessionId));
            }
        }
    }

    @Test
    void testShouldEnrichBasicSessionMetadata() throws Exception {
        ctx.initializeProxy();

        try (var client = ctx.createClient()) {
            var requestedSessionId = UUID.randomUUID().toString();
            var workingDirectory = createUniqueWorkDirectory("server-rpc-enrich");

            try (var session = client.createSession(persistedSessionConfig(requestedSessionId, workingDirectory))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                var sessionId = session.getSessionId();
                session.log("SERVER_RPC_ENRICH_READY").get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                saveSession(client, sessionId);

                var now = OffsetDateTime.now().toString();
                var basic = new LocalSessionMetadataValue(sessionId, now, now, null, "Basic metadata", null, false,
                        null, new SessionContext(workingDirectory.toString(), null, null, null, null), null);

                var result = client.getRpc().sessions.enrichMetadata(new SessionsEnrichMetadataParams(List.of(basic)))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

                assertNotNull(result.sessions());
                assertEquals(1, result.sessions().size());
                var enriched = result.sessions().get(0);
                assertEquals(sessionId, enriched.sessionId());
                assertNotNull(enriched.context());
                assertTrue(pathsEqual(workingDirectory.toString(), enriched.context().cwd()));
                assertFalse(enriched.isRemote());
            }
        }
    }

    @Test
    void testShouldCloseActiveSessionAndReleaseLock() throws Exception {
        ctx.initializeProxy();

        try (var client = ctx.createClient()) {
            var requestedSessionId = UUID.randomUUID().toString();
            var workingDirectory = createUniqueWorkDirectory("server-rpc-close");

            try (var session = client.createSession(persistedSessionConfig(requestedSessionId, workingDirectory))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                var sessionId = session.getSessionId();
                session.log("SERVER_RPC_CLOSE_READY").get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                saveSession(client, sessionId);

                var close = client.getRpc().sessions.close(new SessionsCloseParams(sessionId)).get(TIMEOUT_SECONDS,
                        TimeUnit.SECONDS);
                assertNull(close);

                var release = client.getRpc().sessions.releaseLock(new SessionsReleaseLockParams(sessionId))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertNull(release);

                var inUse = client.getRpc().sessions.checkInUse(new SessionsCheckInUseParams(List.of(sessionId)))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertFalse(inUse.inUse().contains(sessionId));
            }
        }
    }

    @Test
    void testShouldPruneDryRunAndBulkDeletePersistedSession() throws Exception {
        ctx.initializeProxy();

        try (var client = ctx.createClient()) {
            var requestedSessionId = UUID.randomUUID().toString();
            var missingSessionId = UUID.randomUUID().toString();
            var workingDirectory = createUniqueWorkDirectory("server-rpc-delete");

            var session = client.createSession(persistedSessionConfig(requestedSessionId, workingDirectory))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            try {
                var sessionId = session.getSessionId();
                saveSession(client, sessionId);
                client.getRpc().sessions.close(new SessionsCloseParams(sessionId)).get(TIMEOUT_SECONDS,
                        TimeUnit.SECONDS);

                var prune = client.getRpc().sessions.pruneOld(new SessionsPruneOldParams(0L, true, true, List.of()))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertTrue(prune.dryRun());
                assertNotNull(prune.candidates());
                assertNotNull(prune.deleted());
                assertFalse(prune.deleted().contains(sessionId));
                assertFalse(prune.candidates().contains(missingSessionId));
                assertNotNull(prune.freedBytes());
                assertTrue(prune.freedBytes() >= 0);

                var delete = client.getRpc().sessions
                        .bulkDelete(new SessionsBulkDeleteParams(List.of(sessionId, missingSessionId)))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertTrue(delete.freedBytes().containsKey(sessionId));
                assertTrue(delete.freedBytes().get(sessionId) >= 0);
                if (delete.freedBytes().containsKey(missingSessionId)) {
                    assertEquals(0L, delete.freedBytes().get(missingSessionId));
                }

                waitForSessionAbsent(client, sessionId);
            } finally {
                session.close();
            }
        }
    }

    @Test
    void testShouldSetAdditionalPluginsAndReloadDeferredHooks() throws Exception {
        ctx.initializeProxy();

        try (var client = ctx.createClient()) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertNull(client.getRpc().sessions.setAdditionalPlugins(new SessionsSetAdditionalPluginsParams(List.of()))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS));

            var requestedSessionId = UUID.randomUUID().toString();
            var workingDirectory = createUniqueWorkDirectory("server-rpc-hooks");

            try (var session = client.createSession(
                    persistedSessionConfig(requestedSessionId, workingDirectory).setEnableConfigDiscovery(false))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                var sessionId = session.getSessionId();
                var reload = client.getRpc().sessions
                        .reloadPluginHooks(new SessionsReloadPluginHooksParams(sessionId, true))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertNull(reload);

                var loaded = client.getRpc().sessions
                        .loadDeferredRepoHooks(new SessionsLoadDeferredRepoHooksParams(sessionId))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertNotNull(loaded.startupPrompts());
                assertEquals(0L, loaded.hookCount());
                assertTrue(loaded.startupPrompts().isEmpty());
            } finally {
                client.getRpc().sessions.setAdditionalPlugins(new SessionsSetAdditionalPluginsParams(List.of()))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
        }
    }

    @Test
    void testShouldReportImplementedErrorWhenConnectingUnknownRemoteSession() throws Exception {
        ctx.initializeProxy();

        try (var client = ctx.createClient()) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var remoteSessionId = "remote-" + UUID.randomUUID().toString().replace("-", "");

            var ex = assertThrows(Exception.class, () -> client.getRpc().sessions
                    .connect(new SessionsConnectParams(remoteSessionId)).get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            var text = ex.toString();
            assertFalse(text.toLowerCase().contains("unhandled method sessions.connect"));
            assertTrue(text.toLowerCase().contains("session"));
        }
    }

    @Test
    void testShouldDiscoverServerMcpSkillsAgentsAndInstructions() throws Exception {
        ctx.configureForTest("rpc_server", "should_discover_server_mcp_and_skills");

        try (var client = ctx.createClient()) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var workDir = ctx.getWorkDir().toString();
            var skillName = "server-rpc-skill-" + UUID.randomUUID().toString().replace("-", "");
            var skillDirectory = createSkillDirectory(skillName, "Skill discovered by server-scoped RPC tests.");

            var mcp = client.getRpc().mcp.discover(new McpDiscoverParams(workDir)).get(TIMEOUT_SECONDS,
                    TimeUnit.SECONDS);
            assertNotNull(mcp.servers());

            var skills = client.getRpc().skills
                    .discover(new SkillsDiscoverParams(null, List.of(skillDirectory.toString()), null))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var discoveredSkill = findSkill(skills.skills(), skillName);
            assertEquals("Skill discovered by server-scoped RPC tests.", discoveredSkill.description());
            assertTrue(discoveredSkill.enabled());
            assertTrue(discoveredSkill.path().replace('\\', '/').endsWith(skillName + "/SKILL.md"));

            var skillPaths = client.getRpc().skills
                    .getDiscoveryPaths(new SkillsGetDiscoveryPathsParams(List.of(workDir), true))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var projectSkillPath = skillPaths.paths().stream().filter(
                    path -> pathsEqual(workDir, path.projectPath()) && Boolean.TRUE.equals(path.preferredForCreation()))
                    .findFirst().orElseThrow(() -> new AssertionError("Expected project skill discovery path"));
            assertFalse(projectSkillPath.path().isBlank());

            var agents = client.getRpc().agents.discover(new AgentsDiscoverParams(List.of(workDir), true))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertNotNull(agents.agents());
            agents.agents().forEach(agent -> assertFalse(agent.name().isBlank()));

            var agentPaths = client.getRpc().agents
                    .getDiscoveryPaths(new AgentsGetDiscoveryPathsParams(List.of(workDir), true))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var projectAgentPath = agentPaths.paths().stream().filter(
                    path -> pathsEqual(workDir, path.projectPath()) && Boolean.TRUE.equals(path.preferredForCreation()))
                    .findFirst().orElseThrow(() -> new AssertionError("Expected project agent discovery path"));
            assertFalse(projectAgentPath.path().isBlank());

            var instructions = client.getRpc().instructions
                    .discover(new InstructionsDiscoverParams(List.of(workDir), true))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertNotNull(instructions.sources());
            instructions.sources().forEach(source -> {
                assertFalse(source.id().isBlank());
                assertFalse(source.label().isBlank());
                assertFalse(source.sourcePath().isBlank());
            });

            var instructionPaths = client.getRpc().instructions
                    .getDiscoveryPaths(new InstructionsGetDiscoveryPathsParams(List.of(workDir), true))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertFalse(instructionPaths.paths().isEmpty());
            assertTrue(instructionPaths.paths().stream().anyMatch(path -> pathsEqual(workDir, path.projectPath())));
            instructionPaths.paths().forEach(path -> assertFalse(path.path().isBlank()));

            try {
                client.getRpc().skills.config
                        .setDisabledSkills(new SkillsConfigSetDisabledSkillsParams(List.of(skillName)))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                var disabledSkills = client.getRpc().skills
                        .discover(new SkillsDiscoverParams(null, List.of(skillDirectory.toString()), null))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                var disabledSkill = findSkill(disabledSkills.skills(), skillName);
                assertFalse(disabledSkill.enabled());
            } finally {
                client.getRpc().skills.config.setDisabledSkills(new SkillsConfigSetDisabledSkillsParams(List.of()))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
        }
    }

    private static CopilotClient createAuthenticatedClient(String token) throws Exception {
        return ctx.createClient(new CopilotClientOptions().setGitHubToken(token));
    }

    private static void configureAuthenticatedUser(String token, Map<String, Object> quotaSnapshots) throws Exception {
        var user = new HashMap<String, Object>();
        user.put("login", "rpc-user");
        user.put("copilot_plan", "individual_pro");
        user.put("endpoints", Map.of("api", ctx.getProxyUrl(), "telemetry", "https://localhost:1/telemetry"));
        user.put("analytics_tracking_id", "rpc-user-tracking-id");
        if (quotaSnapshots != null) {
            user.put("quota_snapshots", quotaSnapshots);
        }
        ctx.setCopilotUserByToken(token, user);
    }

    private static void assertQuota(AccountQuotaSnapshot chatQuota) {
        assertEquals(100L, chatQuota.entitlementRequests());
        assertEquals(25L, chatQuota.usedRequests());
        assertEquals(75.0, chatQuota.remainingPercentage());
        assertEquals(2.0, chatQuota.overage());
        assertTrue(chatQuota.usageAllowedWithExhaustedQuota());
        assertTrue(chatQuota.overageAllowedWithExhaustedQuota());
        assertEquals(OffsetDateTime.parse("2026-04-30T00:00:00Z"), chatQuota.resetDate());
    }

    private static SessionFsSetProviderConventions currentPathConventions() {
        return isWindows() ? SessionFsSetProviderConventions.WINDOWS : SessionFsSetProviderConventions.POSIX;
    }

    private static Path createUniqueWorkDirectory(String prefix) throws Exception {
        var directory = ctx.getWorkDir().resolve(prefix + "-" + UUID.randomUUID().toString().replace("-", ""));
        Files.createDirectories(directory);
        return directory;
    }

    private static SessionConfig persistedSessionConfig(String sessionId, Path workingDirectory) {
        return new SessionConfig().setSessionId(sessionId).setWorkingDirectory(workingDirectory.toString())
                .setInfiniteSessions(new InfiniteSessionConfig().setEnabled(true))
                .setOnPermissionRequest(PermissionHandler.APPROVE_ALL);
    }

    private static void saveSession(CopilotClient client, String sessionId) throws Exception {
        var save = client.getRpc().sessions.save(new SessionsSaveParams(sessionId)).get(TIMEOUT_SECONDS,
                TimeUnit.SECONDS);
        assertNull(save);
    }

    private static void waitForSessionAbsent(CopilotClient client, String sessionId) throws Exception {
        var deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(SESSION_PERSISTENCE_TIMEOUT_MILLIS);
        do {
            var list = client.getRpc().sessions.list().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertNotNull(list.sessions());
            var present = list.sessions().stream()
                    .anyMatch(session -> session instanceof Map<?, ?> map && sessionId.equals(map.get("sessionId")));
            if (!present) {
                return;
            }
            Thread.sleep(100);
        } while (System.nanoTime() < deadline);

        throw new AssertionError("Timed out waiting for session '" + sessionId + "' to be removed.");
    }

    private static Path createSkillDirectory(String skillName, String description) throws Exception {
        var skillsDir = ctx.getWorkDir().resolve("server-rpc-skills")
                .resolve(UUID.randomUUID().toString().replace("-", ""));
        var skillSubdir = skillsDir.resolve(skillName);
        Files.createDirectories(skillSubdir);
        Files.writeString(skillSubdir.resolve("SKILL.md"), "---\nname: " + skillName + "\ndescription: " + description
                + "\n---\n\n# " + skillName + "\n\nThis skill is used by RPC E2E tests.\n");
        return skillsDir;
    }

    private static ServerSkill findSkill(List<ServerSkill> skills, String name) {
        return skills.stream().filter(skill -> name.equals(skill.name())).findFirst()
                .orElseThrow(() -> new AssertionError("Expected to discover skill " + name));
    }

    private static boolean pathsEqual(String expected, String actual) {
        if (actual == null) {
            return false;
        }

        var expectedPath = Path.of(expected).toAbsolutePath().normalize().toString();
        var actualPath = Path.of(actual).toAbsolutePath().normalize().toString();
        return isWindows() ? expectedPath.equalsIgnoreCase(actualPath) : expectedPath.equals(actualPath);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
