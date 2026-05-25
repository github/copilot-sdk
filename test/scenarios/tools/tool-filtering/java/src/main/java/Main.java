import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.SystemMessageConfig;
import com.github.copilot.sdk.SystemMessageMode;

import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        try (var client = new CopilotClient()) {
            client.start().get();
            var session = client.createSession(
                new SessionConfig()
                    .setModel("claude-haiku-4.5")
                    .setSystemMessage(new SystemMessageConfig()
                        .setMode(SystemMessageMode.REPLACE)
                        .setContent("You are a helpful assistant. You have access to a limited set of tools. "
                            + "When asked about your tools, list exactly which tools you have available."))
                    .setAvailableTools(List.of("grep", "glob", "view")))
                .get();
            var response = session.sendAndWait(
                new MessageOptions().setPrompt("What tools do you have available? List each one by name."))
                .get();
            if (response != null) {
                System.out.println(response.getData().content());
            }
            client.stop().get();
        }
    }
}
