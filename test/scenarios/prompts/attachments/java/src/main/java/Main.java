import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.json.Attachment;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.SystemMessageConfig;
import com.github.copilot.sdk.SystemMessageMode;

import java.nio.file.Path;
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
                        .setContent("You are a helpful assistant. Answer questions about attached files concisely."))
                    .setAvailableTools(List.of()))
                .get();

            var sampleFile = Path.of(System.getProperty("user.dir"), "..", "sample-data.txt")
                .toAbsolutePath().normalize().toString();

            var response = session.sendAndWait(
                new MessageOptions()
                    .setPrompt("What languages are listed in the attached file?")
                    .setAttachments(List.of(
                        new Attachment("file", sampleFile, "sample-data.txt"))))
                .get();
            if (response != null) {
                System.out.println(response.getData().content());
            }
            client.stop().get();
        }
    }
}
