import com.github.copilot.CopilotClient;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.PermissionRequest;
import com.github.copilot.rpc.PermissionRequestResult;
import com.github.copilot.rpc.PermissionRequestResultKind;
import com.github.copilot.rpc.PreToolUseHookOutput;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.SessionHooks;

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
