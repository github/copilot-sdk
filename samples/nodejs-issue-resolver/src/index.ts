import { CopilotClient } from "@github/copilot-sdk";
import * as dotenv from "dotenv";

dotenv.config();

async function runAgenticWorkflow() {
  let client: any;

  // Initialize the client
  try {
    client = new CopilotClient();
  } catch (error: any) {
    console.error("❌ Failed to initialize CopilotClient:", error?.message ?? error);
    process.exit(1);
  }

  try {
    // Start the connection with Copilot CLI
    await client.start();

    // Create a session (GPT-4o is recommended for agentic tasks)
    const session = await client.createSession({
      model: "gpt-4o",
    });

    // Real-time event monitoring for transparency during the agentic loop
    // We use (event: any) to support preview events like 'tool.execution_complete'
    session.on((event: any) => {
      if (event.type === "assistant.message_delta") {
        process.stdout.write(event.data.deltaContent);
      }

      else if (event.type === "tool.execution_start") {
        console.log(`\n\n🛠️  [AGENT ACTION]: ${event.data.toolName}`);
        if (event.data.arguments) {
          const args = typeof event.data.arguments === 'string'
            ? event.data.arguments
            : JSON.stringify(event.data.arguments);
          console.log(`   Parameters: ${args}`);
        }
      }

      else if (event.type === "tool.execution_complete") {
        const toolName = event.data?.toolName || "action";
        console.log(`✅ [ACTION COMPLETED]: ${toolName}`);
      }
    });

    // Get the task description from CLI arguments
    const userIssue = process.argv[2];
    if (!userIssue) {
      console.error("❌ Error: Please provide an issue description.");
      console.log('Example: npm start "Update the description in package.json"');
      process.exit(1);
    }

    console.log(`\n🚀 Agent starting task: "${userIssue}"`);
    console.log(`--------------------------------------------------`);

    // The Prompt: Guides the agent to be decisive and avoid the unstable 'edit' tool
    const result = await session.sendAndWait({
      prompt: `You are an autonomous senior developer.

      USER REQUEST: "${userIssue}"

      GUIDELINES:
      1. Explore the codebase to find relevant files.
      2. If you need to modify a file, ALWAYS use 'write_file' to save the entire content.
      3. DO NOT use the 'edit' tool (it avoids string-matching errors in the technical preview).
      4. Use 'glob', 'view', or 'grep' to navigate the directory structure.
      5. Once the task is done, provide a brief summary of the changes performed.`,
    }, 300000); // 5-minute timeout for complex file operations

    if (result && result.type === "assistant.message") {
      console.log("\n\n🏁 MISSION ACCOMPLISHED");
      console.log("Summary:", result.data.content);
    }

    // Graceful cleanup
    await session.destroy();
    await client.stop();

  } catch (error: any) {
    console.error("\n❌ Workflow Error:", error.message);
    process.exit(1);
  }
}

runAgenticWorkflow();
