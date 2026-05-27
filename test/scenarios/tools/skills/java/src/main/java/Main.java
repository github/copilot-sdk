import com.github.copilot.CopilotClient;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.PreToolUseHookOutput;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.SessionHooks;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Main {
    public static void main(String[] args) throws Exception {
        var skillsDir = Path.of(System.getProperty("user.dir"), "..", "sample-skills")
            .toAbsolutePath().normalize().toString();

        try (var client = new CopilotClient()) {
            client.start().get();
            var session = client.createSession(
                new SessionConfig()
                    .setModel("claude-haiku-4.5")
                    .setSkillDirectories(List.of(skillsDir))
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                    .setHooks(new SessionHooks()
                        .setOnPreToolUse((input, invocation) ->
                            CompletableFuture.completedFuture(PreToolUseHookOutput.allow()))))
                .get();
            var response = session.sendAndWait(
                new MessageOptions().setPrompt("Use the greeting skill to greet someone named Alice."))
                .get();
            if (response != null) {
                System.out.println(response.getData().content());
            }
            System.out.println("\nSkill directories configured successfully");
            client.stop().get();
        }
    }
}
