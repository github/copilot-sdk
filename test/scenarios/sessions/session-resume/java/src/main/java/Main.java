import com.github.copilot.CopilotClient;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.SessionConfig;

import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        try (var client = new CopilotClient()) {
            client.start().get();

            // 1. Create a session
            var session = client.createSession(
                new SessionConfig()
                    .setModel("claude-haiku-4.5")
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                    .setAvailableTools(List.of()))
                .get();

            // 2. Send the secret word
            session.sendAndWait(
                new MessageOptions().setPrompt("Remember this: the secret word is PINEAPPLE."))
                .get();

            // 3. Get the session ID
            var sessionId = session.getSessionId();

            // 4. Resume the session with the same ID
            var resumed = client.resumeSession(sessionId,
                new ResumeSessionConfig()
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                .get();
            System.out.println("Session resumed");

            // 5. Ask for the secret word
            var response = resumed.sendAndWait(
                new MessageOptions().setPrompt("What was the secret word I told you?"))
                .get();
            if (response != null) {
                System.out.println(response.getData().content());
            }
            client.stop().get();
        }
    }
}
