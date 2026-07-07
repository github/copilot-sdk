/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.generated.rpc.NamedProviderConfig;
import com.github.copilot.generated.rpc.ProviderConfigType;
import com.github.copilot.generated.rpc.ProviderConfigWireApi;
import com.github.copilot.generated.rpc.ProviderModelConfig;
import com.github.copilot.generated.rpc.SessionCompletionsRequestParams;
import com.github.copilot.generated.rpc.SessionMetadataGetContextHeaviestMessagesParams;
import com.github.copilot.generated.rpc.SessionModelSwitchToParams;
import com.github.copilot.generated.rpc.SessionProviderAddParams;
import com.github.copilot.generated.rpc.SessionToolsUpdateSubagentSettingsParams;
import com.github.copilot.generated.rpc.SessionVisibilitySetParams;
import com.github.copilot.generated.rpc.SessionVisibilityStatus;
import com.github.copilot.generated.rpc.SubagentSettingsEntry;
import com.github.copilot.generated.rpc.SubagentSettingsEntryContextTier;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;

class RpcSessionStateExtrasE2ETest {

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
    void testShouldAddByokProviderAndModelAtRuntime() throws Exception {
        ctx.configureForTest("rpc_session_state_extras", "should_add_byok_provider_and_model_at_runtime");

        try (var client = ctx.createClient()) {
            try (var session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get()) {
                var result = session.getRpc().provider.add(new SessionProviderAddParams(null,
                        List.of(new NamedProviderConfig("java-e2e-provider", ProviderConfigType.OPENAI,
                                ProviderConfigWireApi.COMPLETIONS, null, "https://models.example.test/v1",
                                "provider-key", null, null, Map.of("x-provider", "java"), null)),
                        List.of(new ProviderModelConfig("small", "java-e2e-provider", null, null, "Java Added Model",
                                4096.0, null, null, null))))
                        .get(30, TimeUnit.SECONDS);
                assertEquals(1, result.models().size());

                var selectionId = "java-e2e-provider/small";
                session.getRpc().model
                        .switchTo(new SessionModelSwitchToParams(null, selectionId, null, null, null, null, null))
                        .get(30, TimeUnit.SECONDS);
                var current = session.getRpc().model.getCurrent().get(30, TimeUnit.SECONDS);
                assertEquals(selectionId, current.modelId());
            }
        }
    }

    @Test
    void testShouldReturnEmptyCompletionsWhenHostDoesNotProvideThem() throws Exception {
        ctx.configureForTest("rpc_session_state_extras",
                "should_return_empty_completions_when_host_does_not_provide_them");

        try (var client = ctx.createClient()) {
            try (var session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get()) {
                var result = session.getRpc().completions
                        .request(new SessionCompletionsRequestParams(null, "Use @ to mention context", 5L))
                        .get(30, TimeUnit.SECONDS);
                assertTrue(result.items().isEmpty());
            }
        }
    }

    @Test
    void testShouldReportVisibilityAsUnsyncedForLocalSession() throws Exception {
        ctx.configureForTest("rpc_session_state_extras", "should_report_visibility_as_unsynced_for_local_session");

        try (var client = ctx.createClient()) {
            try (var session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get()) {
                var set = session.getRpc().visibility
                        .set(new SessionVisibilitySetParams(null, SessionVisibilityStatus.UNSHARED))
                        .get(30, TimeUnit.SECONDS);
                assertFalse(set.synced());
                assertNull(set.status());
                assertNull(set.shareUrl());

                var get = session.getRpc().visibility.get().get(30, TimeUnit.SECONDS);
                assertFalse(get.synced());
                assertNull(get.status());
                assertNull(get.shareUrl());
            }
        }
    }

    @Test
    void testShouldGetContextAttributionAndHeaviestMessagesAfterTurn() throws Exception {
        ctx.configureForTest("rpc_session_state_extras",
                "should_get_context_attribution_and_heaviest_messages_after_turn");

        try (var client = ctx.createClient()) {
            try (var session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get()) {
                var answer = session.sendAndWait(new MessageOptions().setPrompt("Say CONTEXT_METADATA_OK exactly."))
                        .get(60, TimeUnit.SECONDS);
                assertTrue(answer.getData().content().contains("CONTEXT_METADATA_OK"));

                var attribution = session.getRpc().metadata.getContextAttribution().get(30, TimeUnit.SECONDS);
                assertNotNull(attribution.contextAttribution());
                var heaviest = session.getRpc().metadata
                        .getContextHeaviestMessages(new SessionMetadataGetContextHeaviestMessagesParams(null, 5L))
                        .get(30, TimeUnit.SECONDS);
                assertTrue(heaviest.totalTokens() >= 0);
            }
        }
    }

    @Test
    void testShouldUpdateAndClearLiveSubagentSettings() throws Exception {
        ctx.configureForTest("rpc_session_state_extras", "should_update_and_clear_live_subagent_settings");

        try (var client = ctx.createClient()) {
            try (var session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get()) {
                session.getRpc().tools.updateSubagentSettings(new SessionToolsUpdateSubagentSettingsParams(null,
                        new SessionToolsUpdateSubagentSettingsParams.SessionToolsUpdateSubagentSettingsParamsSubagents(
                                Map.of("general-purpose",
                                        new SubagentSettingsEntry("gpt-5-mini", "low",
                                                SubagentSettingsEntryContextTier.LONG_CONTEXT)),
                                List.of("legacy-agent"), null, null)))
                        .get(30, TimeUnit.SECONDS);
                session.getRpc().tools.updateSubagentSettings(new SessionToolsUpdateSubagentSettingsParams(null, null))
                        .get(30, TimeUnit.SECONDS);
            }
        }
    }
}
