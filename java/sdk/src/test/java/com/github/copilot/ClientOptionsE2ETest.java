/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.rpc.SessionLimitsConfig;
import com.github.copilot.generated.AgentModelPolicy;
import com.github.copilot.rpc.CustomAgentConfig;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.ProviderConfig;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.WorkingDirectoryTrust;

class ClientOptionsE2ETest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ParameterizedTest
    @NullSource
    @EnumSource(WorkingDirectoryTrust.class)
    void testShouldForwardTrustAndCustomAgentModelPolicy(WorkingDirectoryTrust trust) throws Exception {
        try (var fake = FakeStdioCli.create()) {
            var policy = trust == null
                    ? null
                    : trust == WorkingDirectoryTrust.SESSION ? AgentModelPolicy.PREFERRED : AgentModelPolicy.REQUIRED;
            var config = new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                    .setWorkingDirectoryTrust(trust)
                    .setCustomAgents(java.util.List.of(new CustomAgentConfig().setName("policy-agent")
                            .setPrompt("Use the authored model.").setModel("claude-haiku-4.5").setModelPolicy(policy)));
            try (var client = fake.createClient()) {
                client.createSession(config).get(30, TimeUnit.SECONDS).close();
            }
            var create = fake.capturedRequest("session.create").path("params");
            var agent = create.path("customAgents").get(0);
            if (trust == null) {
                assertFalse(create.has("trustWorkingDirectory"), create.toString());
                assertFalse(agent.has("modelPolicy"), agent.toString());
            } else {
                assertEquals(trust.getValue(), create.path("trustWorkingDirectory").asText());
                assertEquals(policy.getValue(), agent.path("modelPolicy").asText());
            }
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {true, false})
    void testShouldForwardRefreshCustomInstructionsOnlyOnSessionCreation(Boolean refresh) throws Exception {
        try (var fake = FakeStdioCli.create()) {
            var config = new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL);
            if (refresh != null) {
                config.setRefreshCustomInstructions(refresh);
            }

            try (var client = fake.createClient()) {
                var session = client.createSession(config).get(30, TimeUnit.SECONDS);
                session.close();
                var resumed = client
                        .resumeSession(session.getSessionId(),
                                new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                        .get(30, TimeUnit.SECONDS);
                resumed.close();
            }

            var create = fake.capturedRequest("session.create").path("params");
            if (refresh == null) {
                assertFalse(create.has("refreshCustomInstructions"), create.toString());
            } else {
                assertEquals(MAPPER.valueToTree(refresh), create.get("refreshCustomInstructions"));
            }
            var resume = fake.capturedRequest("session.resume").path("params");
            assertFalse(resume.has("refreshCustomInstructions"), resume.toString());
        }
    }

    @Test
    void testRefreshCustomInstructionsIsNotExposedOnResumeConfig() {
        assertThrows(NoSuchMethodException.class,
                () -> ResumeSessionConfig.class.getMethod("getRefreshCustomInstructions"));
        assertThrows(NoSuchMethodException.class,
                () -> ResumeSessionConfig.class.getMethod("setRefreshCustomInstructions", boolean.class));
        assertThrows(NoSuchMethodException.class,
                () -> ResumeSessionConfig.class.getMethod("clearRefreshCustomInstructions"));
    }

    @Test
    void testShouldForwardAdvancedSessionCreationOptionsToTheCli() throws Exception {
        try (var fake = FakeStdioCli.create()) {
            var workDir = fake.path("create-work");
            var configDir = fake.path("create-config");

            try (var client = fake.createClient()) {
                var session = client.createSession(new SessionConfig().setSessionId("java-create-session")
                        .setName("java named at create").setClientName("java-e2e-client").setModel("gpt-5-mini")
                        .setReasoningEffort("low").setReasoningSummary("none").setContextTier("long_context")
                        .setAvailableTools(java.util.List.of("bash")).setExcludedTools(java.util.List.of("grep"))
                        .setExcludedBuiltInAgents(java.util.List.of("explore")).setEnableSessionTelemetry(true)
                        .setEnableCitations(true).setSessionLimits(new SessionLimitsConfig(42.0))
                        .setWorkingDirectory(workDir.toString()).setStreaming(true)
                        .setIncludeSubAgentStreamingEvents(true).setConfigDirectory(configDir.toString())
                        .setEnableConfigDiscovery(false).setSkipEmbeddingRetrieval(true)
                        .setOrganizationCustomInstructions("Use Java parity instructions.")
                        .setEnableOnDemandInstructionDiscovery(false).setEnableFileHooks(true)
                        .setEnableHostGitOperations(false).setEnableSessionStore(true).setEnableSkills(false)
                        .setEmbeddingCacheStorage("in-memory").setGitHubToken("java-session-token")
                        .setRemoteSession("export").setSkipCustomInstructions(true).setCustomAgentsLocalOnly(false)
                        .setCoauthorEnabled(true).setManageScheduleEnabled(true)
                        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get(30, TimeUnit.SECONDS);
                session.close();
            }

            var create = fake.capturedRequest("session.create").path("params");
            assertEquals("java-create-session", create.path("sessionId").asText());
            assertEquals("java named at create", create.path("name").asText());
            assertEquals("java-e2e-client", create.path("clientName").asText());
            assertEquals("gpt-5-mini", create.path("model").asText());
            assertEquals("low", create.path("reasoningEffort").asText());
            assertEquals("none", create.path("reasoningSummary").asText());
            assertEquals("long_context", create.path("contextTier").asText());
            assertEquals("bash", create.path("availableTools").get(0).asText());
            assertEquals("grep", create.path("excludedTools").get(0).asText());
            assertEquals("explore", create.path("excludedBuiltinAgents").get(0).asText());
            assertEquals(MAPPER.valueToTree(true), create.get("enableSessionTelemetry"));
            assertEquals(MAPPER.valueToTree(true), create.get("enableCitations"));
            assertEquals(42.0, create.path("sessionLimits").path("maxAiCredits").asDouble());
            assertEquals(workDir.toString(), create.path("workingDirectory").asText());
            assertEquals(MAPPER.valueToTree(true), create.get("streaming"));
            assertEquals(MAPPER.valueToTree(true), create.get("includeSubAgentStreamingEvents"));
            assertEquals(configDir.toString(), create.path("configDir").asText());
            assertEquals(MAPPER.valueToTree(false), create.get("enableConfigDiscovery"));
            assertEquals(MAPPER.valueToTree(true), create.get("skipEmbeddingRetrieval"));
            assertEquals("Use Java parity instructions.", create.path("organizationCustomInstructions").asText());
            assertEquals(MAPPER.valueToTree(false), create.get("enableOnDemandInstructionDiscovery"));
            assertEquals(MAPPER.valueToTree(true), create.get("enableFileHooks"));
            assertEquals(MAPPER.valueToTree(false), create.get("enableHostGitOperations"));
            assertEquals(MAPPER.valueToTree(true), create.get("enableSessionStore"));
            assertEquals(MAPPER.valueToTree(false), create.get("enableSkills"));
            assertEquals("in-memory", create.path("embeddingCacheStorage").asText());
            assertEquals("java-session-token", create.path("gitHubToken").asText());
            assertEquals("export", create.path("remoteSession").asText());
            assertEquals("direct", create.path("envValueMode").asText());
            assertEquals(MAPPER.valueToTree(true), create.get("requestPermission"));

            var update = fake.capturedRequest("session.options.update").path("params");
            assertEquals("java-create-session", update.path("sessionId").asText());
            assertEquals(MAPPER.valueToTree(true), update.get("skipCustomInstructions"));
            assertEquals(MAPPER.valueToTree(false), update.get("customAgentsLocalOnly"));
            assertEquals(MAPPER.valueToTree(true), update.get("coauthorEnabled"));
            assertEquals(MAPPER.valueToTree(true), update.get("manageScheduleEnabled"));
        }
    }

    @Test
    void testShouldForwardSingularProviderConfigurationOnSessionCreation() throws Exception {
        try (var fake = FakeStdioCli.create()) {
            try (var client = fake.createClient()) {
                var session = client.createSession(new SessionConfig()
                        .setProvider(new ProviderConfig().setType("openai").setWireApi("responses")
                                .setTransport("websockets").setBaseUrl("https://models.example.test/v1")
                                .setApiKey("provider-key").setModelId("base-model").setWireModel("wire-model")
                                .setMaxPromptTokens(1000).setMaxOutputTokens(2000)
                                .setHeaders(Map.of("x-provider", "java")))
                        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get(30, TimeUnit.SECONDS);
                session.close();
            }

            var provider = fake.capturedRequest("session.create").path("params").path("provider");
            assertEquals("openai", provider.path("type").asText());
            assertEquals("responses", provider.path("wireApi").asText());
            assertEquals("websockets", provider.path("transport").asText());
            assertEquals("https://models.example.test/v1", provider.path("baseUrl").asText());
            assertEquals("provider-key", provider.path("apiKey").asText());
            assertEquals("base-model", provider.path("modelId").asText());
            assertEquals("wire-model", provider.path("wireModel").asText());
            assertEquals(1000, provider.path("maxPromptTokens").asInt());
            assertEquals(2000, provider.path("maxOutputTokens").asInt());
            assertEquals("java", provider.path("headers").path("x-provider").asText());
        }
    }

    @Test
    void testShouldForwardAdvancedSessionResumeOptionsToTheCli() throws Exception {
        try (var fake = FakeStdioCli.create()) {
            var workDir = fake.path("resume-work");
            var configDir = fake.path("resume-config");

            try (var client = fake.createClient()) {
                client.createSession(new SessionConfig().setSessionId("java-resume-session")
                        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get(30, TimeUnit.SECONDS);
                var session = client.resumeSession("java-resume-session",
                        new ResumeSessionConfig().setClientName("java-resume-client").setModel("gpt-5-mini")
                                .setReasoningEffort("medium").setReasoningSummary("none").setContextTier("long_context")
                                .setEnableCitations(true).setSessionLimits(new SessionLimitsConfig(84.0))
                                .setWorkingDirectory(workDir.toString()).setConfigDirectory(configDir.toString())
                                .setEnableConfigDiscovery(false).setSkipEmbeddingRetrieval(true)
                                .setOrganizationCustomInstructions("Use resumed Java instructions.")
                                .setEnableOnDemandInstructionDiscovery(false).setEnableFileHooks(true)
                                .setEnableHostGitOperations(false).setEnableSessionStore(true).setEnableSkills(false)
                                .setEmbeddingCacheStorage("in-memory").setGitHubToken("java-resume-token")
                                .setRemoteSession("export").setSkipCustomInstructions(false)
                                .setCustomAgentsLocalOnly(true).setCoauthorEnabled(false).setManageScheduleEnabled(true)
                                .setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                        .get(30, TimeUnit.SECONDS);
                session.close();
            }

            var resume = fake.capturedRequest("session.resume").path("params");
            assertEquals("java-resume-session", resume.path("sessionId").asText());
            assertEquals("java-resume-client", resume.path("clientName").asText());
            assertEquals("gpt-5-mini", resume.path("model").asText());
            assertEquals("medium", resume.path("reasoningEffort").asText());
            assertEquals("none", resume.path("reasoningSummary").asText());
            assertEquals("long_context", resume.path("contextTier").asText());
            assertEquals(MAPPER.valueToTree(true), resume.get("enableCitations"));
            assertEquals(84.0, resume.path("sessionLimits").path("maxAiCredits").asDouble());
            assertEquals(workDir.toString(), resume.path("workingDirectory").asText());
            assertEquals(configDir.toString(), resume.path("configDir").asText());
            assertEquals(MAPPER.valueToTree(false), resume.get("enableConfigDiscovery"));
            assertEquals(MAPPER.valueToTree(true), resume.get("skipEmbeddingRetrieval"));
            assertEquals("Use resumed Java instructions.", resume.path("organizationCustomInstructions").asText());
            assertEquals(MAPPER.valueToTree(false), resume.get("enableOnDemandInstructionDiscovery"));
            assertEquals(MAPPER.valueToTree(true), resume.get("enableFileHooks"));
            assertEquals(MAPPER.valueToTree(false), resume.get("enableHostGitOperations"));
            assertEquals(MAPPER.valueToTree(true), resume.get("enableSessionStore"));
            assertEquals(MAPPER.valueToTree(false), resume.get("enableSkills"));
            assertEquals("in-memory", resume.path("embeddingCacheStorage").asText());
            assertEquals("java-resume-token", resume.path("gitHubToken").asText());
            assertEquals("export", resume.path("remoteSession").asText());
            assertEquals("direct", resume.path("envValueMode").asText());
            assertEquals(MAPPER.valueToTree(true), resume.get("requestPermission"));

            var update = fake.capturedRequest("session.options.update").path("params");
            assertEquals("java-resume-session", update.path("sessionId").asText());
            assertEquals(MAPPER.valueToTree(false), update.get("skipCustomInstructions"));
            assertEquals(MAPPER.valueToTree(true), update.get("customAgentsLocalOnly"));
            assertEquals(MAPPER.valueToTree(false), update.get("coauthorEnabled"));
            assertEquals(MAPPER.valueToTree(true), update.get("manageScheduleEnabled"));
        }
    }

    private record FakeStdioCli(Path dir, Path script, Path capture, Path workDir) implements AutoCloseable {

        static FakeStdioCli create() throws IOException {
            var dir = Files.createTempDirectory("java-fake-copilot-cli-");
            var script = dir.resolve("fake-copilot-cli.js");
            var capture = dir.resolve("capture.json");
            var workDir = dir.resolve("work");
            Files.createDirectories(workDir);
            Files.writeString(capture, "{\"requests\":[]}");
            Files.writeString(script, FAKE_STDIO_CLI_SCRIPT);
            return new FakeStdioCli(dir, script, capture, workDir);
        }

        CopilotClient createClient() {
            var options = new CopilotClientOptions().setCliPath(script.toString())
                    .setCliArgs(new String[]{"--capture-file", capture.toString()}).setCwd(workDir.toString())
                    .setUseLoggedInUser(false);
            return new CopilotClient(options);
        }

        Path path(String name) throws IOException {
            var path = workDir.resolve(name);
            Files.createDirectories(path);
            return path;
        }

        JsonNode capturedRequest(String method) throws IOException {
            for (JsonNode request : MAPPER.readTree(Files.readString(capture)).path("requests")) {
                if (method.equals(request.path("method").asText())) {
                    return request;
                }
            }
            fail("Expected captured request for " + method + " in " + Files.readString(capture));
            return null;
        }

        @Override
        public void close() throws IOException {
            if (Files.exists(dir)) {
                try (var paths = Files.walk(dir)) {
                    paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
                }
            }
        }
    }

    private static final String FAKE_STDIO_CLI_SCRIPT = """
            const fs = require('fs');

            const captureFileIndex = process.argv.indexOf('--capture-file');
            const captureFile = process.argv[captureFileIndex + 1];
            const capture = { requests: [] };
            fs.writeFileSync(captureFile, JSON.stringify(capture));

            let buffer = Buffer.alloc(0);

            function persist() {
              fs.writeFileSync(captureFile, JSON.stringify(capture));
            }

            function send(message) {
              const body = Buffer.from(JSON.stringify(message), 'utf8');
              process.stdout.write(`Content-Length: ${body.length}\\r\\n\\r\\n`);
              process.stdout.write(body);
            }

            function resultFor(message) {
              switch (message.method) {
                case 'connect':
                  return { ok: true, protocolVersion: 3, version: 'fake' };
                case 'llmInference.setProvider':
                  return {};
                case 'session.create':
                  return { sessionId: message.params?.sessionId ?? 'fake-session', openCanvases: [] };
                case 'session.resume':
                  return { sessionId: message.params?.sessionId ?? 'fake-session', openCanvases: [] };
                case 'session.detach':
                  return { success: true };
                case 'session.options.update':
                  return { success: true };
                default:
                  return {};
              }
            }

            function handle(message) {
              capture.requests.push({ method: message.method, params: message.params ?? null });
              persist();
              send({ jsonrpc: '2.0', id: message.id, result: resultFor(message) });
            }

            process.stdin.on('data', chunk => {
              buffer = Buffer.concat([buffer, chunk]);
              while (true) {
                const headerEnd = buffer.indexOf('\\r\\n\\r\\n');
                if (headerEnd < 0) {
                  return;
                }
                const header = buffer.subarray(0, headerEnd).toString('utf8');
                const match = /Content-Length:\\s*(\\d+)/i.exec(header);
                if (!match) {
                  throw new Error(`Missing Content-Length in ${header}`);
                }
                const length = Number(match[1]);
                const bodyStart = headerEnd + 4;
                if (buffer.length < bodyStart + length) {
                  return;
                }
                const body = buffer.subarray(bodyStart, bodyStart + length).toString('utf8');
                buffer = buffer.subarray(bodyStart + length);
                handle(JSON.parse(body));
              }
            });
            """;
}
