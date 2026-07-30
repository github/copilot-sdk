/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.generated.rpc.AccountAllUsers;
import com.github.copilot.generated.rpc.AccountLoginParams;
import com.github.copilot.generated.rpc.AccountLogoutParams;
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
}
