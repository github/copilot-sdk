import { CopilotClient } from "@github/copilot-sdk";
import readline from "node:readline/promises";
import { stdin as input, stdout as output } from "node:process";

type DeviceCodeResponse = {
  device_code: string;
  user_code: string;
  verification_uri: string;
  expires_in: number;
  interval: number;
};

type OAuthTokenResponse = {
  access_token?: string;
  error?: string;
  error_description?: string;
  interval?: number;
};

type GitHubUser = {
  login: string;
  name: string | null;
};

const DEVICE_CODE_URL = "https://github.com/login/device/code";
const ACCESS_TOKEN_URL = "https://github.com/login/oauth/access_token";
const USER_URL = "https://api.github.com/user";

const CLIENT_ID = process.env.GITHUB_OAUTH_CLIENT_ID;

if (!CLIENT_ID) {
  console.error("Missing GITHUB_OAUTH_CLIENT_ID.");
  process.exit(1);
}

async function postJson<T>(url: string, body: Record<string, unknown>): Promise<T> {
  const response = await fetch(url, {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    throw new Error(`Request failed: ${response.status} ${response.statusText}`);
  }

  return (await response.json()) as T;
}

async function getJson<T>(url: string, token: string): Promise<T> {
  const response = await fetch(url, {
    headers: {
      Accept: "application/json",
      Authorization: `Bearer ${token}`,
      "User-Agent": "copilot-sdk-samples-auth-gh-app",
    },
  });

  if (!response.ok) {
    throw new Error(`GitHub API failed: ${response.status} ${response.statusText}`);
  }

  return (await response.json()) as T;
}

async function startDeviceFlow(): Promise<DeviceCodeResponse> {
  return postJson<DeviceCodeResponse>(DEVICE_CODE_URL, {
    client_id: CLIENT_ID,
    scope: "read:user",
  });
}

async function pollForAccessToken(deviceCode: string, intervalSeconds: number): Promise<string> {
  let interval = intervalSeconds;

  while (true) {
    await new Promise((resolve) => setTimeout(resolve, interval * 1000));

    const data = await postJson<OAuthTokenResponse>(ACCESS_TOKEN_URL, {
      client_id: CLIENT_ID,
      device_code: deviceCode,
      grant_type: "urn:ietf:params:oauth:grant-type:device_code",
    });

    if (data.access_token) return data.access_token;
    if (data.error === "authorization_pending") continue;
    if (data.error === "slow_down") {
      interval = data.interval ?? interval + 5;
      continue;
    }

    throw new Error(data.error_description ?? data.error ?? "OAuth token polling failed");
  }
}

async function main() {
  console.log("Starting GitHub OAuth device flow...");
  const device = await startDeviceFlow();

  console.log(`Open ${device.verification_uri} and enter code: ${device.user_code}`);
  const rl = readline.createInterface({ input, output });
  await rl.question("Press Enter after you authorize this app...");
  rl.close();

  const accessToken = await pollForAccessToken(device.device_code, device.interval);
  const user = await getJson<GitHubUser>(USER_URL, accessToken);
  console.log(`Authenticated as: ${user.login}${user.name ? ` (${user.name})` : ""}`);

  const client = new CopilotClient({
    ...(process.env.COPILOT_CLI_PATH && { cliPath: process.env.COPILOT_CLI_PATH }),
    githubToken: accessToken,
  });

  try {
    const session = await client.createSession({ model: "claude-haiku-4.5" });
    const response = await session.sendAndWait({
      prompt: "What is the capital of France?",
    });

    if (response) console.log(response.data.content);
    await session.destroy();
  } finally {
    await client.stop();
  }
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
