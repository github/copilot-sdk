import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.PreToolUseHookOutput;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.SessionHooks;

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
