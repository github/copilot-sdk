import com.github.copilot.CopilotClient;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.SystemMessageConfig;
import com.github.copilot.SystemMessageMode;

import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        var systemPrompt = """
            You are a minimal assistant with no tools available.
            You cannot execute code, read files, edit files, search, or perform any actions.
            You can only respond with text based on your training data.
            If asked about your capabilities or tools, clearly state that you have no tools available.
            """;

        try (var client = new CopilotClient()) {
            client.start().get();
            var session = client.createSession(
                new SessionConfig()
                    .setModel("claude-haiku-4.5")
                    .setSystemMessage(new SystemMessageConfig()
                        .setMode(SystemMessageMode.REPLACE)
                        .setContent(systemPrompt))
                    .setAvailableTools(List.of()))
                .get();
            var response = session.sendAndWait(
                new MessageOptions().setPrompt("Use the bash tool to run 'echo hello'."))
                .get();
            if (response != null) {
                System.out.println(response.getData().content());
            }
            client.stop().get();
        }
    }
}
