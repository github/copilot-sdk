// Source: auth/index.md:94
import { CopilotClient } from "@github/copilot-sdk";

const client = new CopilotClient({
    githubToken: userAccessToken,  // Token from OAuth flow
    useLoggedInUser: false,        // Don't use stored CLI credentials
});