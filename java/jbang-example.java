
//DEPS com.github.copilot:copilot-sdk:0.1.0
import com.github.copilot.sdk.*;
import com.github.copilot.sdk.events.*;
import com.github.copilot.sdk.json.*;
import java.util.concurrent.CompletableFuture;

class CopilotSDK {
    public static void main(String[] args) throws Exception {
        // Create and start client
        try (var client = new CopilotClient()) {
            client.start().get();

            // Create a session
            var session = client.createSession(
                    new SessionConfig().setModel(CopilotModel.CLAUDE_SONNET_4_5.toString())).get();

            // Wait for response using session.idle event
            var done = new CompletableFuture<Void>();

            session.on(evt -> {
                if (evt instanceof AssistantMessageEvent msg) {
                    System.out.println(msg.getData().getContent());
                } else if (evt instanceof SessionIdleEvent) {
                    done.complete(null);
                }
            });

            // Send a message and wait for completion
            session.send(new MessageOptions().setPrompt("What is 2+2?")).get();
            done.get();
        }
    }
}
