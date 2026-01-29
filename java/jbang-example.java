
//DEPS io.github.copilot-community-sdk:copilot-sdk:1.0.5
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
            var sessionConfig = new SessionConfig().setModel("claude-sonnet-4.5");
            var session = client.createSession(sessionConfig).get();

            // Wait for response using session.idle event
            var done = new CompletableFuture<Void>();

            session.on(evt -> {
                if (evt instanceof AssistantMessageEvent msg) {
                    System.out.println(msg.getData().getContent());
                } else if (evt instanceof SessionUsageInfoEvent usage) {
                    var data = usage.getData();
                    System.out.println("\n--- Usage Metrics ---");
                    System.out.println("Current tokens: " + (int) data.getCurrentTokens());
                    System.out.println("Token limit: " + (int) data.getTokenLimit());
                    System.out.println("Messages count: " + (int) data.getMessagesLength());
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
