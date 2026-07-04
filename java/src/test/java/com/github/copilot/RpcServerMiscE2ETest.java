/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.generated.rpc.AccountAllUsers;
import com.github.copilot.generated.rpc.AccountLoginParams;
import com.github.copilot.generated.rpc.AccountLogoutParams;
import com.github.copilot.generated.rpc.AgentsDiscoverParams;
import com.github.copilot.generated.rpc.AgentsGetDiscoveryPathsParams;
import com.github.copilot.generated.rpc.InstructionsDiscoverParams;
import com.github.copilot.generated.rpc.InstructionsGetDiscoveryPathsParams;
import com.github.copilot.generated.rpc.LlmInferenceHttpResponseChunkParams;
import com.github.copilot.generated.rpc.LlmInferenceHttpResponseStartParams;
import com.github.copilot.generated.rpc.McpDiscoverParams;
import com.github.copilot.generated.rpc.ServerSkill;
import com.github.copilot.generated.rpc.SkillsConfigSetDisabledSkillsParams;
import com.github.copilot.generated.rpc.SkillsDiscoverParams;
import com.github.copilot.generated.rpc.SkillsGetDiscoveryPathsParams;
import com.github.copilot.generated.rpc.UserSettingMetadata;
import com.github.copilot.generated.rpc.UserSettingsSetParams;
import com.github.copilot.rpc.CopilotClientOptions;

class RpcServerMiscE2ETest {

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
    void testShouldRejectLlmResponseFramesForUnknownRequest() throws Exception {
        ctx.initializeProxy();

        try (var client = ctx.createClient()) {
            client.start().get(30, TimeUnit.SECONDS);
            var requestId = "missing-llm-response-request";

            var start = client.getRpc().llmInference
                    .httpResponseStart(new LlmInferenceHttpResponseStartParams(requestId, 200L, "OK",
                            Map.of("content-type", List.of("application/json"))))
                    .get(30, TimeUnit.SECONDS);
            assertFalse(start.accepted());

            var chunk = client.getRpc().llmInference
                    .httpResponseChunk(new LlmInferenceHttpResponseChunkParams(requestId, "{}", false, true, null))
                    .get(30, TimeUnit.SECONDS);
            assertFalse(chunk.accepted());
        }
    }

    @Test
    void testShouldGetSetAndClearUserSettings() throws Exception {
        ctx.configureForTest("rpc_server_misc", "should_get_set_and_clear_user_settings");

        try (var client = ctx.createClient()) {
            client.start().get(30, TimeUnit.SECONDS);
            var before = client.getRpc().user.settings.get().get(30, TimeUnit.SECONDS);
            var entry = before.settings().entrySet().stream().filter(e -> isBooleanSetting(e.getValue())).findFirst()
                    .orElseThrow(() -> new AssertionError("Expected at least one boolean user setting"));
            var key = entry.getKey();
            var original = settingBoolean(entry.getValue());
            var updated = !original;

            var set = client.getRpc().user.settings.set(new UserSettingsSetParams(Map.of(key, updated))).get(30,
                    TimeUnit.SECONDS);
            assertTrue(set.shadowedKeys().isEmpty());
            client.getRpc().user.settings.reload().get(30, TimeUnit.SECONDS);
            var afterSet = client.getRpc().user.settings.get().get(30, TimeUnit.SECONDS);
            assertEquals(updated, settingBoolean(afterSet.settings().get(key)));
            assertFalse(afterSet.settings().get(key).isDefault());

            var clearSettings = new HashMap<String, Object>();
            clearSettings.put(key, null);
            var clear = client.getRpc().user.settings.set(new UserSettingsSetParams(clearSettings)).get(30,
                    TimeUnit.SECONDS);
            assertTrue(clear.shadowedKeys().isEmpty());
            client.getRpc().user.settings.reload().get(30, TimeUnit.SECONDS);
            var afterClear = client.getRpc().user.settings.get().get(30, TimeUnit.SECONDS);
            assertTrue(afterClear.settings().get(key).isDefault());
        }
    }

    @Test
    void testShouldLoginListGetCurrentAuthAndLogoutAccount() throws Exception {
        ctx.configureForTest("rpc_server_misc", "should_login_list_getcurrentauth_and_logout_account");
        var token = "java-account-token";
        var login = "java-account-user";
        ctx.setCopilotUserByToken(token, login, "individual_pro", ctx.getProxyUrl(), "https://localhost:1/telemetry",
                "java-account-tracking-id");

        var env = new HashMap<>(ctx.getEnvironment());
        env.put("GH_TOKEN", "");
        env.put("GITHUB_TOKEN", "");
        env.put("COPILOT_SDK_AUTH_TOKEN", "");

        try (var client = new CopilotClient(
                new CopilotClientOptions().setCliPath(ctx.getCliPath()).setCwd(ctx.getWorkDir().toString())
                        .setEnvironment(env).setGitHubToken("").setUseLoggedInUser(false))) {
            client.start().get(30, TimeUnit.SECONDS);

            var initial = client.getRpc().account.getCurrentAuth().get(30, TimeUnit.SECONDS);
            assertNull(initial.authInfo());

            var loginResult = client.getRpc().account.login(new AccountLoginParams("https://github.com", login, token))
                    .get(30, TimeUnit.SECONDS);
            assertNotNull(loginResult);

            var current = client.getRpc().account.getCurrentAuth().get(30, TimeUnit.SECONDS);
            assertNull(current.authErrors());
            assertInstanceOf(Map.class, current.authInfo());
            @SuppressWarnings("unchecked")
            var authInfo = (Map<String, Object>) current.authInfo();
            assertEquals(login, authInfo.get("login"));
            assertEquals("https://github.com", authInfo.get("host"));

            var users = client.getRpc().account.getAllUsers().get(30, TimeUnit.SECONDS);
            users.stream().filter(user -> accountLogin(user).equals(login)).findFirst()
                    .ifPresent(user -> assertEquals(token, user.token()));

            var logout = client.getRpc().account.logout(new AccountLogoutParams(authInfo)).get(30, TimeUnit.SECONDS);
            assertFalse(logout.hasMoreUsers());
            assertNull(client.getRpc().account.getCurrentAuth().get(30, TimeUnit.SECONDS).authInfo());
        }
    }

    @Test
    void testShouldDiscoverServerMcpSkillsAgentsAndInstructions() throws Exception {
        ctx.initializeProxy();

        try (var client = ctx.createClient()) {
            client.start().get(30, TimeUnit.SECONDS);
            var workDir = ctx.getWorkDir().toString();
            var skillName = "server-rpc-skill-" + UUID.randomUUID().toString().replace("-", "");
            var skillDirectory = createSkillDirectory(skillName, "Skill discovered by server-scoped RPC tests.");

            var mcp = client.getRpc().mcp.discover(new McpDiscoverParams(workDir)).get(30, TimeUnit.SECONDS);
            assertNotNull(mcp.servers());

            var skills = client.getRpc().skills
                    .discover(new SkillsDiscoverParams(null, List.of(skillDirectory.toString()), null))
                    .get(30, TimeUnit.SECONDS);
            var discoveredSkill = skills.skills().stream().filter(skill -> skillName.equals(skill.name())).findFirst()
                    .orElseThrow(() -> new AssertionError("Expected to discover skill " + skillName));
            assertEquals("Skill discovered by server-scoped RPC tests.", discoveredSkill.description());
            assertTrue(Boolean.TRUE.equals(discoveredSkill.enabled()));
            assertTrue(discoveredSkill.path().replace('\\', '/').endsWith(skillName + "/SKILL.md"));

            var skillPaths = client.getRpc().skills
                    .getDiscoveryPaths(new SkillsGetDiscoveryPathsParams(List.of(workDir), true))
                    .get(30, TimeUnit.SECONDS);
            var projectSkillPath = skillPaths.paths().stream().filter(
                    path -> pathsEqual(workDir, path.projectPath()) && Boolean.TRUE.equals(path.preferredForCreation()))
                    .findFirst().orElseThrow(() -> new AssertionError("Expected project skill discovery path"));
            assertFalse(projectSkillPath.path().isBlank());

            var agents = client.getRpc().agents.discover(new AgentsDiscoverParams(List.of(workDir), true)).get(30,
                    TimeUnit.SECONDS);
            assertNotNull(agents.agents());
            agents.agents().forEach(agent -> assertFalse(agent.name().isBlank()));

            var agentPaths = client.getRpc().agents
                    .getDiscoveryPaths(new AgentsGetDiscoveryPathsParams(List.of(workDir), true))
                    .get(30, TimeUnit.SECONDS);
            var projectAgentPath = agentPaths.paths().stream().filter(
                    path -> pathsEqual(workDir, path.projectPath()) && Boolean.TRUE.equals(path.preferredForCreation()))
                    .findFirst().orElseThrow(() -> new AssertionError("Expected project agent discovery path"));
            assertFalse(projectAgentPath.path().isBlank());

            var instructions = client.getRpc().instructions
                    .discover(new InstructionsDiscoverParams(List.of(workDir), true)).get(30, TimeUnit.SECONDS);
            assertNotNull(instructions.sources());
            instructions.sources().forEach(source -> {
                assertFalse(source.id().isBlank());
                assertFalse(source.label().isBlank());
                assertFalse(source.sourcePath().isBlank());
            });

            var instructionPaths = client.getRpc().instructions
                    .getDiscoveryPaths(new InstructionsGetDiscoveryPathsParams(List.of(workDir), true))
                    .get(30, TimeUnit.SECONDS);
            assertFalse(instructionPaths.paths().isEmpty());
            assertTrue(instructionPaths.paths().stream().anyMatch(path -> pathsEqual(workDir, path.projectPath())));
            instructionPaths.paths().forEach(path -> assertFalse(path.path().isBlank()));

            try {
                client.getRpc().skills.config
                        .setDisabledSkills(new SkillsConfigSetDisabledSkillsParams(List.of(skillName)))
                        .get(30, TimeUnit.SECONDS);
                var disabledSkills = client.getRpc().skills
                        .discover(new SkillsDiscoverParams(null, List.of(skillDirectory.toString()), null))
                        .get(30, TimeUnit.SECONDS);
                var disabledSkill = findSkill(disabledSkills.skills(), skillName);
                assertFalse(Boolean.TRUE.equals(disabledSkill.enabled()));
            } finally {
                client.getRpc().skills.config.setDisabledSkills(new SkillsConfigSetDisabledSkillsParams(List.of()))
                        .get(30, TimeUnit.SECONDS);
            }
        }
    }

    private static boolean isBooleanSetting(UserSettingMetadata metadata) {
        return metadata.value() instanceof Boolean || metadata.default_() instanceof Boolean;
    }

    private static boolean settingBoolean(UserSettingMetadata metadata) {
        if (metadata.value() instanceof Boolean value) {
            return value;
        }
        return (Boolean) metadata.default_();
    }

    private static String accountLogin(AccountAllUsers user) {
        if (user.authInfo() instanceof Map<?, ?> authInfo) {
            return String.valueOf(authInfo.get("login"));
        }
        return "";
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
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return expectedPath.equalsIgnoreCase(actualPath);
        }

        return expectedPath.equals(actualPath);
    }
}
