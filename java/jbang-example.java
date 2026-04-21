!
//DEPS com.github:copilot-sdk-java:0.3.0-java-preview.1-java-preview.0
import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.generated.AssistantMessageEvent;
import com.github.copilot.sdk.generated.SessionUsageInfoEvent;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.SessionConfig;

import static java.lang.System.out;

class CopilotSDK {
    public static void main(String[] args) throws Exception {
        // Create and start client
        try (var client = new CopilotClient()) {
            client.start().get();

            // Create a session
            var session = client.createSession(
                new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setModel("claude-sonnet-4.5")).get();

            // Handle assistant message events
            session.on(AssistantMessageEvent.class, msg -> {
                out.println(msg.getData().content());
            });

            // Handle session usage info events
            session.on(SessionUsageInfoEvent.class, usage -> {
                var data = usage.getData();
                out.println("\n--- Usage Metrics ---");
                out.println("Current tokens: " + data.currentTokens().intValue());
                out.println("Token limit: " + data.tokenLimit().intValue());
                out.println("Messages count: " + data.messagesLength().intValue());
            });

            // Send a message
            var completable = session.sendAndWait(new MessageOptions().setPrompt("What is 2+2?"));
            // and wait for completion
            completable.get();
        }
    }
}
