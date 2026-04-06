/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.events.AbstractSessionEvent;
import com.github.copilot.sdk.json.CopilotClientOptions;
import com.github.copilot.sdk.json.InfiniteSessionConfig;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.ModelInfo;
import com.github.copilot.sdk.json.ResumeSessionConfig;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.SystemMessageConfig;
import com.github.copilot.sdk.json.TelemetryConfig;

class ConfigCloneTest {

    @Test
    void copilotClientOptionsCloneBasic() {
        CopilotClientOptions original = new CopilotClientOptions();
        original.setCliPath("/usr/local/bin/copilot");
        original.setLogLevel("debug");
        original.setPort(9000);
        original.setGitHubToken("ghp_test");
        original.setUseLoggedInUser(false);

        CopilotClientOptions cloned = original.clone();

        assertEquals(original.getCliPath(), cloned.getCliPath());
        assertEquals(original.getLogLevel(), cloned.getLogLevel());
        assertEquals(original.getPort(), cloned.getPort());
        assertEquals(original.getGitHubToken(), cloned.getGitHubToken());
        assertEquals(original.getUseLoggedInUser(), cloned.getUseLoggedInUser());
    }

    @Test
    void copilotClientOptionsArrayIndependence() {
        CopilotClientOptions original = new CopilotClientOptions();
        String[] args = {"--flag1", "--flag2"};
        original.setCliArgs(args);

        CopilotClientOptions cloned = original.clone();

        // Mutate the source array after set — should not affect original or clone
        args[0] = "--changed";

        assertEquals("--flag1", original.getCliArgs()[0]);
        assertEquals("--flag1", cloned.getCliArgs()[0]);

        // getCliArgs() returns a copy, so mutating it should not affect internals
        original.getCliArgs()[0] = "--mutated";
        assertEquals("--flag1", original.getCliArgs()[0]);
    }

    @Test
    void copilotClientOptionsEnvironmentIndependence() {
        CopilotClientOptions original = new CopilotClientOptions();
        Map<String, String> env = new HashMap<>();
        env.put("KEY1", "value1");
        original.setEnvironment(env);

        CopilotClientOptions cloned = original.clone();

        // Mutate the source map after set — should not affect original or clone
        env.put("KEY2", "value2");

        assertEquals(1, original.getEnvironment().size());
        assertEquals(1, cloned.getEnvironment().size());

        // getEnvironment() returns a copy, so mutating it should not affect internals
        original.getEnvironment().put("KEY3", "value3");
        assertEquals(1, original.getEnvironment().size());
    }

    @Test
    void copilotClientOptionsOnListModelsCloned() {
        CopilotClientOptions original = new CopilotClientOptions();
        List<ModelInfo> models = List.of(new ModelInfo());
        original.setOnListModels(() -> CompletableFuture.completedFuture(models));

        CopilotClientOptions cloned = original.clone();

        assertNotNull(cloned.getOnListModels());
        assertSame(original.getOnListModels(), cloned.getOnListModels());
    }

    @Test
    void sessionConfigCloneBasic() {
        SessionConfig original = new SessionConfig();
        original.setSessionId("my-session");
        original.setClientName("my-app");
        original.setModel("gpt-4o");
        original.setStreaming(true);

        SessionConfig cloned = original.clone();

        assertEquals(original.getSessionId(), cloned.getSessionId());
        assertEquals(original.getClientName(), cloned.getClientName());
        assertEquals(original.getModel(), cloned.getModel());
        assertEquals(original.isStreaming(), cloned.isStreaming());
    }

    @Test
    void sessionConfigListIndependence() {
        SessionConfig original = new SessionConfig();
        List<String> toolList = new ArrayList<>();
        toolList.add("grep");
        toolList.add("bash");
        original.setAvailableTools(toolList);

        SessionConfig cloned = original.clone();

        // Mutate the original list directly to test independence
        toolList.add("web");

        // The cloned config should be unaffected by mutations to the original list
        assertEquals(2, cloned.getAvailableTools().size());
        assertEquals(3, original.getAvailableTools().size());
    }

    @Test
    void sessionConfigAgentAndOnEventCloned() {
        Consumer<AbstractSessionEvent> handler = event -> {
        };
        SessionConfig original = new SessionConfig();
        original.setAgent("my-agent");
        original.setOnEvent(handler);

        SessionConfig cloned = original.clone();

        assertEquals("my-agent", cloned.getAgent());
        assertSame(handler, cloned.getOnEvent());
    }

    @Test
    void resumeSessionConfigCloneBasic() {
        ResumeSessionConfig original = new ResumeSessionConfig();
        original.setModel("o1");
        original.setStreaming(false);

        ResumeSessionConfig cloned = original.clone();

        assertEquals(original.getModel(), cloned.getModel());
        assertEquals(original.isStreaming(), cloned.isStreaming());
    }

    @Test
    void resumeSessionConfigAgentAndOnEventCloned() {
        Consumer<AbstractSessionEvent> handler = event -> {
        };
        ResumeSessionConfig original = new ResumeSessionConfig();
        original.setAgent("my-agent");
        original.setOnEvent(handler);

        ResumeSessionConfig cloned = original.clone();

        assertEquals("my-agent", cloned.getAgent());
        assertSame(handler, cloned.getOnEvent());
    }

    @Test
    void messageOptionsCloneBasic() {
        MessageOptions original = new MessageOptions();
        original.setPrompt("What is 2+2?");
        original.setMode("immediate");

        MessageOptions cloned = original.clone();

        assertEquals(original.getPrompt(), cloned.getPrompt());
        assertEquals(original.getMode(), cloned.getMode());
    }

    @Test
    void clonePreservesNullFields() {
        CopilotClientOptions opts = new CopilotClientOptions();
        CopilotClientOptions optsClone = opts.clone();
        assertNull(optsClone.getCliPath());

        SessionConfig cfg = new SessionConfig();
        SessionConfig cfgClone = cfg.clone();
        assertNull(cfgClone.getModel());

        MessageOptions msg = new MessageOptions();
        MessageOptions msgClone = msg.clone();
        assertNull(msgClone.getMode());
    }

    @Test
    @SuppressWarnings("deprecation")
    void copilotClientOptionsDeprecatedAutoRestart() {
        CopilotClientOptions opts = new CopilotClientOptions();
        assertFalse(opts.isAutoRestart());
        opts.setAutoRestart(true);
        assertTrue(opts.isAutoRestart());
    }

    @Test
    void copilotClientOptionsSetCliArgsNullClearsExisting() {
        CopilotClientOptions opts = new CopilotClientOptions();
        opts.setCliArgs(new String[]{"--flag1"});
        assertNotNull(opts.getCliArgs());

        // Setting null should clear the existing array
        opts.setCliArgs(null);
        assertNotNull(opts.getCliArgs());
        assertEquals(0, opts.getCliArgs().length);
    }

    @Test
    void copilotClientOptionsSetEnvironmentNullClearsExisting() {
        CopilotClientOptions opts = new CopilotClientOptions();
        opts.setEnvironment(Map.of("KEY", "VALUE"));
        assertNotNull(opts.getEnvironment());

        // Setting null should clear the existing map (clears in-place → returns empty
        // map)
        opts.setEnvironment(null);
        var env = opts.getEnvironment();
        assertTrue(env == null || env.isEmpty());
    }

    @Test
    @SuppressWarnings("deprecation")
    void copilotClientOptionsDeprecatedGithubToken() {
        CopilotClientOptions opts = new CopilotClientOptions();
        opts.setGithubToken("ghp_deprecated_token");
        assertEquals("ghp_deprecated_token", opts.getGithubToken());
        assertEquals("ghp_deprecated_token", opts.getGitHubToken());
    }

    @Test
    void copilotClientOptionsSetTelemetry() {
        var telemetry = new TelemetryConfig().setOtlpEndpoint("http://localhost:4318");
        var opts = new CopilotClientOptions();
        opts.setTelemetry(telemetry);
        assertSame(telemetry, opts.getTelemetry());
    }

    @Test
    void copilotClientOptionsSetUseLoggedInUserNull() {
        var opts = new CopilotClientOptions();
        opts.setUseLoggedInUser(null);
        // null → Boolean.FALSE
        assertEquals(Boolean.FALSE, opts.getUseLoggedInUser());
    }

    @Test
    void resumeSessionConfigAllSetters() {
        var config = new ResumeSessionConfig();

        var sysMsg = new SystemMessageConfig();
        config.setSystemMessage(sysMsg);
        assertSame(sysMsg, config.getSystemMessage());

        config.setAvailableTools(List.of("bash", "read_file"));
        assertEquals(List.of("bash", "read_file"), config.getAvailableTools());

        config.setExcludedTools(List.of("write_file"));
        assertEquals(List.of("write_file"), config.getExcludedTools());

        config.setReasoningEffort("high");
        assertEquals("high", config.getReasoningEffort());

        config.setWorkingDirectory("/project/src");
        assertEquals("/project/src", config.getWorkingDirectory());

        config.setConfigDir("/home/user/.config/copilot");
        assertEquals("/home/user/.config/copilot", config.getConfigDir());

        config.setSkillDirectories(List.of("/skills/custom"));
        assertEquals(List.of("/skills/custom"), config.getSkillDirectories());

        config.setDisabledSkills(List.of("some-skill"));
        assertEquals(List.of("some-skill"), config.getDisabledSkills());

        var infiniteConfig = new InfiniteSessionConfig().setEnabled(true);
        config.setInfiniteSessions(infiniteConfig);
        assertSame(infiniteConfig, config.getInfiniteSessions());
    }
}
