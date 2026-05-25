import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.PermissionRequest;
import com.github.copilot.sdk.json.PermissionRequestResult;
import com.github.copilot.sdk.json.PermissionRequestResultKind;
import com.github.copilot.sdk.json.PreToolUseHookOutput;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.SessionHooks;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class Main {
    public static void main(String[] args) throws Exception {
        var permissionLog = new ArrayList<String>();

        try (var client = new CopilotClient()) {
            client.start().get();
            var session = client.createSession(
                new SessionConfig()
                    .setModel("claude-haiku-4.5")
                    .setOnPermissionRequest((request, invocation) -> {
                        permissionLog.add("approved:" + request.getKind());
                        return CompletableFuture.completedFuture(
                            new PermissionRequestResult()
                                .setKind(PermissionRequestResultKind.APPROVED));
                    })
                    .setHooks(new SessionHooks()
                        .setOnPreToolUse((input, invocation) ->
                            CompletableFuture.completedFuture(PreToolUseHookOutput.allow()))))
                .get();
            var response = session.sendAndWait(
                new MessageOptions().setPrompt(
                    "List the files in the current directory using glob with pattern '*.md'."))
                .get();
            if (response != null) {
                System.out.println(response.getData().content());
            }
            System.out.println("\n--- Permission request log ---");
            for (var entry : permissionLog) {
                System.out.println("  " + entry);
            }
            System.out.println("\nTotal permission requests: " + permissionLog.size());
            client.stop().get();
        }
    }
}
