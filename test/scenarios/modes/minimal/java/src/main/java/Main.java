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
                    .setAvailableTools(List.of())
                    .setSystemMessage(new SystemMessageConfig()
                        .setMode(SystemMessageMode.REPLACE)
                        .setContent("You have no tools. Respond with text only.")))
                .get();
            var response = session.sendAndWait(
                new MessageOptions().setPrompt(
                    "Use the grep tool to search for 'SDK' in README.md."))
                .get();
            if (response != null) {
                System.out.println("Response: " + response.getData().content());
            }
            System.out.println("Minimal mode test complete");
            client.stop().get();
        }
    }
}
