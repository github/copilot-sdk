import com.github.copilot.CopilotClient;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.SystemMessageConfig;
import com.github.copilot.SystemMessageMode;

import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        try (var client = new CopilotClient()) {
            client.start().get();

            var session1 = client.createSession(
                new SessionConfig()
                    .setModel("claude-haiku-4.5")
                    .setSystemMessage(new SystemMessageConfig()
                        .setMode(SystemMessageMode.REPLACE)
                        .setContent("You are a pirate. Always say Arrr!"))
                    .setAvailableTools(List.of()))
                .get();

            var session2 = client.createSession(
                new SessionConfig()
                    .setModel("claude-haiku-4.5")
                    .setSystemMessage(new SystemMessageConfig()
                        .setMode(SystemMessageMode.REPLACE)
                        .setContent("You are a robot. Always say BEEP BOOP!"))
                    .setAvailableTools(List.of()))
                .get();

            var response1 = session1.sendAndWait(
                new MessageOptions().setPrompt("What is the capital of France?"))
                .get();
            var response2 = session2.sendAndWait(
                new MessageOptions().setPrompt("What is the capital of France?"))
                .get();

            if (response1 != null) {
                System.out.println("Session 1 (pirate): " + response1.getData().content());
            }
            if (response2 != null) {
                System.out.println("Session 2 (robot): " + response2.getData().content());
            }
            client.stop().get();
        }
    }
}
