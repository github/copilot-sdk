/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.generated.AssistantMessageEvent;
import com.github.copilot.generated.SessionEvent;
import com.github.copilot.generated.ToolExecutionCompleteEvent;
import com.github.copilot.generated.rpc.Skill;
import com.github.copilot.generated.rpc.SkillSource;
import com.github.copilot.rpc.CloudSessionOptions;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.SessionConfig;

@AllowCopilotExperimental
class SkillProviderTest {

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
    void should_load_provider_skill_lazily_through_skill_tool() throws Exception {
        ctx.configureForTest("skill_provider", "should_load_provider_skill_lazily_through_skill_tool");

        var provider = new TestSkillProvider(
                List.of(skill("provider-lookup", "Reports the provider lookup verification word.",
                        "# Provider lookup\n\nThe verification word is TANGERINE_QUARTZ_19. Reply with it.\n")));

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig()
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setSkillProvider(provider))
                    .get(30, TimeUnit.SECONDS);

            try {
                Skill listed = session.getRpc().skills.list().get(30, TimeUnit.SECONDS).skills().stream()
                        .filter(s -> "provider-lookup".equals(s.name())).findFirst().orElse(null);
                assertNotNull(listed, "Expected provider-lookup to be listed");
                assertEquals(SkillSource.SDK, listed.source());
                assertTrue(listed.enabled());
                assertEquals("", listed.path() == null ? "" : listed.path());
                assertEquals(List.of(), provider.reads());

                AssistantMessageEvent message = session.sendAndWait(new MessageOptions().setPrompt(
                        "Use the skill tool to load the provider-lookup skill, then reply with its verification word."),
                        60_000).get(90, TimeUnit.SECONDS);

                assertEquals(List.of("provider-lookup"), provider.reads());
                // Validate the final assistant response arrived (guards against truncated
                // captures)
                assertTrue(assistantText(message).contains("TANGERINE_QUARTZ_19"));
            } finally {
                session.close();
            }
        }
    }

    @Test
    void should_load_provider_and_file_based_skills_together() throws Exception {
        ctx.configureForTest("skill_provider", "should_load_provider_and_file_based_skills_together");

        Path skillsDir = ctx.getWorkDir().resolve("file-skills");
        Files.createDirectories(skillsDir.resolve("file-notes"));
        Files.writeString(skillsDir.resolve("file-notes").resolve("SKILL.md"),
                "---\nname: file-notes\ndescription: Reports the file notes verification word.\n---\n\nThe file notes verification word is MAPLE_FALCON_27.\n");

        var provider = new TestSkillProvider(List.of(skill("provider-audit",
                "Reports the provider audit verification word.",
                "---\nname: provider-audit\nallowed-tools: view\n---\n\nThe provider audit verification word is COBALT_HERON_58.\n")));

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                            .setSkillDirectories(List.of(skillsDir.toString())).setSkillProvider(provider))
                    .get(30, TimeUnit.SECONDS);

            try {
                List<Skill> skills = session.getRpc().skills.list().get(30, TimeUnit.SECONDS).skills();
                Skill fileSkill = findSkill(skills, "file-notes");
                Skill providerSkill = findSkill(skills, "provider-audit");
                assertNotNull(fileSkill, "Expected file-notes to be listed");
                assertNotEquals(SkillSource.SDK, fileSkill.source());
                assertNotNull(fileSkill.path());
                assertNotNull(providerSkill, "Expected provider-audit to be listed");
                assertEquals(SkillSource.SDK, providerSkill.source());

                AssistantMessageEvent message = session.sendAndWait(new MessageOptions().setPrompt(
                        "Use the skill tool to load the file-notes skill and the provider-audit skill, then reply with both verification words."),
                        60_000).get(90, TimeUnit.SECONDS);

                assertEquals(List.of("provider-audit"), provider.reads());
                assertTrue(assistantText(message).contains("MAPLE_FALCON_27"));
                // Validate the final assistant response arrived (guards against truncated
                // captures)
                assertTrue(assistantText(message).contains("COBALT_HERON_58"));
            } finally {
                session.close();
            }
        }
    }

    @Test
    void should_rebind_skill_provider_on_resume() throws Exception {
        ctx.configureForTest("skill_provider", "should_rebind_skill_provider_on_resume");

        var original = new TestSkillProvider(List.of(skill("rebind-check", "Reports the rebind verification word.",
                "The rebind verification word is AMBER_ALPHA_11.\n")));
        var replacement = new TestSkillProvider(List.of(skill("rebind-check", "Reports the rebind verification word.",
                "The rebind verification word is BRONZE_BETA_22.\n")));

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession first = client.createSession(new SessionConfig()
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setSkillProvider(original))
                    .get(30, TimeUnit.SECONDS);
            String sessionId = first.getSessionId();
            AssistantMessageEvent ready = first
                    .sendAndWait(new MessageOptions()
                            .setPrompt("Without using any tools or skills, reply with exactly REBIND_READY."), 60_000)
                    .get(90, TimeUnit.SECONDS);
            assertTrue(assistantText(ready).contains("REBIND_READY"));
            first.close();
            assertEquals(List.of(), original.reads());
            int originalCallsBeforeResume = original.callCount();

            CopilotSession session = client
                    .resumeSession(sessionId, new ResumeSessionConfig()
                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setSkillProvider(replacement))
                    .get(30, TimeUnit.SECONDS);

            try {
                AssistantMessageEvent message = session.sendAndWait(new MessageOptions().setPrompt(
                        "Use the skill tool to load the rebind-check skill, then reply with its verification word."),
                        60_000).get(90, TimeUnit.SECONDS);

                assertEquals(List.of("rebind-check"), replacement.reads());
                assertEquals(originalCallsBeforeResume, original.callCount());
                // Validate the final assistant response arrived (guards against truncated
                // captures)
                assertTrue(assistantText(message).contains("BRONZE_BETA_22"));
                assertFalse(assistantText(message).contains("AMBER_ALPHA_11"));
            } finally {
                session.close();
            }
        }
    }

    @Test
    void should_report_provider_read_failure_without_leaking_details() throws Exception {
        ctx.configureForTest("skill_provider", "should_report_provider_read_failure_without_leaking_details");

        String secret = "PROVIDER_SECRET_7F3A9C";
        var provider = new TestSkillProvider(List.of(new ProvidedSkill(new SkillProviderDescriptor("broken-lookup",
                "Reports the broken lookup verification word.", null, null, null), () -> {
                    throw new IllegalStateException("database unavailable: " + secret);
                })));
        List<SessionEvent> events = new CopyOnWriteArrayList<>();

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig()
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setSkillProvider(provider))
                    .get(30, TimeUnit.SECONDS);
            session.on(events::add);

            try {
                AssistantMessageEvent message = session.sendAndWait(new MessageOptions().setPrompt(
                        "Use the skill tool to load the broken-lookup skill. If loading fails, reply with exactly LOAD_FAILED."),
                        60_000).get(90, TimeUnit.SECONDS);

                assertTrue(provider.reads().contains("broken-lookup"));
                List<ToolExecutionCompleteEvent> failures = failedToolExecutions(events);
                assertEquals(1, failures.size());
                assertFalse(JsonRpcClient.getObjectMapper().writeValueAsString(events).contains(secret));
                // Validate the final assistant response arrived (guards against truncated
                // captures)
                assertTrue(assistantText(message).contains("LOAD_FAILED"));
            } finally {
                session.close();
            }
        }
    }

    @Test
    void should_report_missing_provider_skill_as_not_found() throws Exception {
        ctx.configureForTest("skill_provider", "should_report_missing_provider_skill_as_not_found");

        var provider = new TestSkillProvider(List.of(new ProvidedSkill(new SkillProviderDescriptor("vanished-lookup",
                "Reports the vanished lookup verification word.", null, null, null), () -> null)));
        List<SessionEvent> events = new CopyOnWriteArrayList<>();

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig()
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setSkillProvider(provider))
                    .get(30, TimeUnit.SECONDS);
            session.on(events::add);

            try {
                AssistantMessageEvent message = session.sendAndWait(new MessageOptions().setPrompt(
                        "Use the skill tool to load the vanished-lookup skill. If loading fails, reply with exactly LOAD_FAILED."),
                        60_000).get(90, TimeUnit.SECONDS);

                assertTrue(provider.reads().contains("vanished-lookup"));
                List<ToolExecutionCompleteEvent> failures = failedToolExecutions(events);
                assertEquals(1, failures.size());
                String failureJson = JsonRpcClient.getObjectMapper().writeValueAsString(failures.get(0))
                        .toLowerCase(Locale.ROOT);
                assertTrue(failureJson.contains("not found"), "Expected not found failure, got: " + failureJson);
                // Validate the final assistant response arrived (guards against truncated
                // captures)
                assertTrue(assistantText(message).contains("LOAD_FAILED"));
            } finally {
                session.close();
            }
        }
    }

    @Test
    void should_keep_provider_dormant_when_skills_disabled() throws Exception {
        ctx.initializeProxy();

        var provider = new TestSkillProvider(List.of(skill("dormant-lookup", "Never listed.", "Never read.\n")));

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                            .setEnableSkills(false).setSkillProvider(provider))
                    .get(30, TimeUnit.SECONDS);

            try {
                session.getRpc().skills.ensureLoaded().get(30, TimeUnit.SECONDS);
                List<Skill> skills = session.getRpc().skills.list().get(30, TimeUnit.SECONDS).skills();

                assertEquals(List.of(), sdkSkills(skills));
                assertEquals(List.of(), provider.calls());
            } finally {
                session.close();
            }
        }
    }

    @Test
    void should_unbind_provider_when_resumed_without_one() throws Exception {
        ctx.initializeProxy();

        var provider = new TestSkillProvider(
                List.of(skill("unbound-lookup", "Reports the unbound lookup word.", "Unbound.\n")));

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession first = client.createSession(new SessionConfig()
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setSkillProvider(provider))
                    .get(30, TimeUnit.SECONDS);
            List<Skill> before = first.getRpc().skills.list().get(30, TimeUnit.SECONDS).skills();
            assertTrue(before.stream().anyMatch(s -> "unbound-lookup".equals(s.name())));
            int callsBeforeResume = provider.callCount();

            CopilotSession session = client
                    .resumeSession(first.getSessionId(),
                            new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                    .get(30, TimeUnit.SECONDS);

            try {
                session.getRpc().skills.reload().get(30, TimeUnit.SECONDS);
                List<Skill> skills = session.getRpc().skills.list().get(30, TimeUnit.SECONDS).skills();

                assertEquals(List.of(), sdkSkills(skills));
                assertEquals(callsBeforeResume, provider.callCount());
            } finally {
                session.close();
            }
        }
    }

    @Test
    void should_cancel_a_blocked_provider_call_when_the_session_disconnects() throws Exception {
        ctx.initializeProxy();

        var entered = new CountDownLatch(1);
        var pending = new CompletableFuture<List<SkillProviderDescriptor>>();
        var provider = new SkillProvider() {
            @Override
            public CompletableFuture<List<SkillProviderDescriptor>> listSkills() {
                entered.countDown();
                return pending;
            }

            @Override
            public CompletableFuture<String> readSkill(String name) {
                return CompletableFuture.completedFuture(null);
            }
        };

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig()
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setSkillProvider(provider))
                    .get(30, TimeUnit.SECONDS);

            // The list RPC fails once the binding is removed; only the provider's
            // cancellation matters here.
            session.getRpc().skills.list();
            assertTrue(entered.await(30, TimeUnit.SECONDS), "provider listSkills was not called");

            session.close();
            assertThrows(CancellationException.class, () -> pending.get(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void should_reject_skill_provider_for_cloud_sessions() throws Exception {
        ctx.initializeProxy();

        var provider = new TestSkillProvider(List.of(skill("cloud-lookup", "Never listed.", "Never read.\n")));

        try (CopilotClient client = ctx.createClient()) {
            var config = new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                    .setCloud(new CloudSessionOptions()).setSkillProvider(provider);

            ExecutionException error = assertThrows(ExecutionException.class,
                    () -> client.createSession(config).get(30, TimeUnit.SECONDS));
            assertInstanceOf(IllegalArgumentException.class, error.getCause());
            assertEquals("Skill providers are not supported for cloud sessions.", error.getCause().getMessage());
            assertEquals(List.of(), provider.calls());
        }
    }

    private static ProvidedSkill skill(String name, String description, String markdown) {
        return new ProvidedSkill(new SkillProviderDescriptor(name, description, null, null, null), () -> markdown);
    }

    private static Skill findSkill(List<Skill> skills, String name) {
        return skills.stream().filter(s -> name.equals(s.name())).findFirst().orElse(null);
    }

    private static List<Skill> sdkSkills(List<Skill> skills) {
        return skills.stream().filter(s -> s.source() == SkillSource.SDK).toList();
    }

    private static List<ToolExecutionCompleteEvent> failedToolExecutions(List<SessionEvent> events) {
        return events.stream().filter(ToolExecutionCompleteEvent.class::isInstance)
                .map(ToolExecutionCompleteEvent.class::cast)
                .filter(event -> !Boolean.TRUE.equals(event.getData().success())).toList();
    }

    private static String assistantText(AssistantMessageEvent response) {
        assertNotNull(response, "Expected a response from the assistant");
        assertNotNull(response.getData(), "Expected assistant response data");
        assertNotNull(response.getData().content(), "Expected assistant response content");
        return response.getData().content();
    }

    private record ProvidedSkill(SkillProviderDescriptor descriptor, Supplier<String> reader) {
        String read() {
            return reader.get();
        }
    }

    private static final class TestSkillProvider implements SkillProvider {
        private final List<String> calls = new CopyOnWriteArrayList<>();
        private final List<ProvidedSkill> skills;

        private TestSkillProvider(List<ProvidedSkill> skills) {
            this.skills = skills;
        }

        List<String> calls() {
            return List.copyOf(calls);
        }

        int callCount() {
            return calls.size();
        }

        List<String> reads() {
            return calls.stream().filter(call -> call.startsWith("read:")).map(call -> call.substring("read:".length()))
                    .toList();
        }

        @Override
        public CompletableFuture<List<SkillProviderDescriptor>> listSkills() {
            calls.add("list");
            return CompletableFuture.completedFuture(skills.stream().map(ProvidedSkill::descriptor).toList());
        }

        @Override
        public CompletableFuture<String> readSkill(String name) {
            calls.add("read:" + name);
            return CompletableFuture
                    .completedFuture(skills.stream().filter(skill -> skill.descriptor().name().equals(name)).findFirst()
                            .map(ProvidedSkill::read).orElse(null));
        }
    }
}
