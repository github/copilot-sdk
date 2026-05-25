import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.json.HookInvocation;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.PostToolUseHookOutput;
import com.github.copilot.sdk.json.PreToolUseHookOutput;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.SessionEndHookOutput;
import com.github.copilot.sdk.json.SessionHooks;
import com.github.copilot.sdk.json.SessionStartHookOutput;
import com.github.copilot.sdk.json.UserPromptSubmittedHookOutput;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class Main {
    public static void main(String[] args) throws Exception {
        var hookLog = new ArrayList<String>();

        try (var client = new CopilotClient()) {
            client.start().get();
            var session = client.createSession(
                new SessionConfig()
                    .setModel("claude-haiku-4.5")
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                    .setHooks(new SessionHooks()
                        .setOnSessionStart((input, invocation) -> {
                            hookLog.add("onSessionStart");
                            return CompletableFuture.completedFuture(null);
                        })
                        .setOnSessionEnd((input, invocation) -> {
                            hookLog.add("onSessionEnd");
                            return CompletableFuture.completedFuture(null);
                        })
                        .setOnPreToolUse((input, invocation) -> {
                            hookLog.add("onPreToolUse:" + input.getToolName());
                            return CompletableFuture.completedFuture(PreToolUseHookOutput.allow());
                        })
                        .setOnPostToolUse((input, invocation) -> {
                            hookLog.add("onPostToolUse:" + input.getToolName());
                            return CompletableFuture.completedFuture(null);
                        })
                        .setOnUserPromptSubmitted((input, invocation) -> {
                            hookLog.add("onUserPromptSubmitted");
                            return CompletableFuture.completedFuture(null);
                        })))
                .get();
            var response = session.sendAndWait(
                new MessageOptions().setPrompt(
                    "List the files in the current directory using the glob tool with pattern '*.md'."))
                .get();
            if (response != null) {
                System.out.println(response.getData().content());
            }
            System.out.println("\n--- Hook execution log ---");
            for (var entry : hookLog) {
                System.out.println("  " + entry);
            }
            System.out.println("\nTotal hooks fired: " + hookLog.size());
            client.stop().get();
        }
    }
}
