import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.json.InfiniteSessionConfig;
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
                        .setContent("You are a helpful assistant. Answer concisely in one sentence."))
                    .setInfiniteSessions(new InfiniteSessionConfig()
                        .setEnabled(true)
                        .setBackgroundCompactionThreshold(0.80)
                        .setBufferExhaustionThreshold(0.95)))
                .get();

            var prompts = List.of(
                "What is the capital of France?",
                "What is the capital of Japan?",
                "What is the capital of Brazil?");

            for (var prompt : prompts) {
                var response = session.sendAndWait(
                    new MessageOptions().setPrompt(prompt))
                    .get();
                if (response != null) {
                    System.out.println("Q: " + prompt);
                    System.out.println("A: " + response.getData().content() + "\n");
                }
            }
            System.out.println("Infinite sessions test complete — all messages processed successfully");
            client.stop().get();
        }
    }
}
