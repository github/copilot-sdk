# Server-to-server tokens

Use a GitHub App installation access token to authenticate the Copilot SDK from automation without relying on a user's personal access token. This flow is intended for agents, CI/CD jobs, and backend services that make Copilot requests on behalf of an organization.

## When to use this flow

Use server-to-server tokens when your application needs:

* Organization-attributed Copilot usage and billing instead of user-attributed usage
* Automation that cannot depend on an interactive user sign-in
* A short-lived credential minted by a GitHub App installation
* Copilot SDK access from workflows, services, or agents that operate on repositories

For per-user identity or per-user billing, use the standard user OAuth or token-based authentication flows in [Authenticate Copilot SDK](./authenticate.md).

> [!NOTE]
> Server-to-server tokens do not bypass Copilot model policies. Requests authenticated with an organization installation token use the models allowed by that organization's Copilot policy.

## Prerequisites

Before you begin, make sure you have:

* A GitHub Enterprise Cloud organization that is enabled for the Copilot GitHub App server-to-server flow
* A GitHub App that your service owns
* The GitHub App's app ID and private key
* Permission to install the app on the organization that should be billed
* A repository that the Copilot request can be attributed to

If the organization or enterprise is not enabled for this flow, Copilot API requests made with the installation token return `401 Unauthorized` even when the GitHub App is configured correctly.

## How it works

1. Create or update a GitHub App.
1. Grant the app the **Copilot Requests** repository permission with **Read & write** access.
1. Install the app on the organization that should be billed.
1. Mint a repository-scoped installation access token that explicitly requests `copilot_requests: write`.
1. Pass the token to the Copilot CLI subprocess through `COPILOT_GITHUB_TOKEN`.
1. Create and use Copilot SDK sessions normally.

The token returned by GitHub starts with `ghs_` and expires after 1 hour.

## Create or update the GitHub App

Create a GitHub App by following [Creating a GitHub App](https://docs.github.com/en/apps/creating-github-apps).

When configuring app permissions:

* Under **Repository permissions**, set **Copilot Requests** to **Read & write**.
* Add any other repository permissions your app needs, such as **Contents**, **Issues**, or **Pull requests**.
* Leave **Where can this GitHub App be installed?** set to the account scope that fits your application.

If the app already exists, update it in **Settings** > **Developer settings** > **GitHub Apps** > your app > **Permissions & events**. Existing installations must re-approve the new **Copilot Requests** permission before tokens minted from those installations can call Copilot.

## Install the app on the organization

Install the GitHub App on the organization that should be attributed and billed for Copilot usage.

Organization installations are recommended because:

* Usage is attributed and billed to the organization instead of an individual user.
* Organization installations are the right shape for automation that acts on repositories owned by the organization.
* Organization installations can use organization-level Copilot policies and limits.

When choosing repository access, select **All repositories**. The current Copilot permission check requires the parent installation to have all-repository access. You still scope each minted token to specific repositories by passing `repository_ids` when you create the installation access token.

After installing the app, record the installation ID. You can find it in the installation URL or by calling `GET /app/installations` and filtering by `account.login`.

## Mint an installation access token

Follow [Generating an installation access token for a GitHub App](https://docs.github.com/en/apps/creating-github-apps/authenticating-with-a-github-app/generating-an-installation-access-token-for-a-github-app), with two required details for Copilot:

* Pass `repository_ids` to scope the token to one or more repositories.
* Pass `permissions` explicitly, including `"copilot_requests": "write"`.

If your automation is not naturally tied to a repository, use a stable attribution repository that the app can access, such as the organization's `.github` repository or a dedicated placeholder repository.

```http
POST https://api.github.com/app/installations/INSTALLATION_ID/access_tokens
Authorization: Bearer APP_JWT
Accept: application/vnd.github+json
Content-Type: application/json

{
  "repository_ids": [REPOSITORY_ID],
  "permissions": {
    "copilot_requests": "write",
    "metadata": "read"
  }
}
```

The response contains a `token` field that starts with `ghs_`. Confirm that the response includes:

* `"repository_selection": "selected"`
* `"permissions": { "copilot_requests": "write", ... }`

Add any other permissions your app needs to the `permissions` object. Requested permissions must be a subset of the permissions approved on the installation.

> [!WARNING]
> For Copilot server-to-server authentication, tokens minted without `repository_ids`, or without an explicit `permissions` object containing `copilot_requests: write`, are rejected by the Copilot API.

## Pass the token to the SDK

Do not pass a `ghs_` installation token with the SDK's `gitHubToken` or `github_token` option. Those options use the SDK's explicit user-token path, which calls user-oriented GitHub API endpoints that reject GitHub App installation tokens.

Instead, leave the explicit GitHub token option unset and pass the installation token to the spawned Copilot CLI process with `COPILOT_GITHUB_TOKEN`.

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

<!-- docs-validate: skip -->
```typescript
import { CopilotClient } from "@github/copilot-sdk";

const installationToken = await mintInstallationToken(); // Returns "ghs_..."

const client = new CopilotClient({
    env: {
        ...process.env,
        COPILOT_GITHUB_TOKEN: installationToken,
    },
    useLoggedInUser: false,
});

await client.start();

const session = await client.createSession({
    model: "claude-sonnet-4.6",
});
```

</details>

<details>
<summary><strong>Python</strong></summary>

<!-- docs-validate: skip -->
```python
import os

from copilot import CopilotClient

installation_token = mint_installation_token()  # Returns "ghs_..."

client = CopilotClient(
    env={**os.environ, "COPILOT_GITHUB_TOKEN": installation_token},
    use_logged_in_user=False,
)

await client.start()

session = await client.create_session(
    model="claude-sonnet-4.6",
)
```

</details>

<details>
<summary><strong>Go</strong></summary>

<!-- docs-validate: skip -->
```go
package main

import (
	"context"
	"log"
	"os"

	copilot "github.com/github/copilot-sdk/go"
)

func mintInstallationToken() string {
	return "ghs_..."
}

func main() {
	installationToken := mintInstallationToken()

	client := copilot.NewClient(&copilot.ClientOptions{
		Env: append(os.Environ(), "COPILOT_GITHUB_TOKEN="+installationToken),
		UseLoggedInUser: copilot.Bool(false),
	})

	ctx := context.Background()
	if err := client.Start(ctx); err != nil {
		log.Fatal(err)
	}
	defer client.Stop()

	session, err := client.CreateSession(ctx, &copilot.SessionConfig{
		Model: "claude-sonnet-4.6",
	})
	if err != nil {
		log.Fatal(err)
	}
	defer session.Disconnect()
}
```

</details>

<details>
<summary><strong>.NET</strong></summary>

<!-- docs-validate: skip -->
```csharp
using System.Collections;
using GitHub.Copilot;

var installationToken = MintInstallationToken(); // Returns "ghs_..."

var env = System.Environment.GetEnvironmentVariables()
    .Cast<DictionaryEntry>()
    .ToDictionary(
        entry => (string)entry.Key,
        entry => entry.Value?.ToString() ?? string.Empty);
env["COPILOT_GITHUB_TOKEN"] = installationToken;

await using var client = new CopilotClient(new CopilotClientOptions
{
    Environment = env,
    UseLoggedInUser = false,
});

await client.StartAsync();

await using var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "claude-sonnet-4.6",
});
```

</details>

<details>
<summary><strong>Java</strong></summary>

<!-- docs-validate: skip -->
```java
import com.github.copilot.CopilotClient;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.SessionConfig;
import java.util.HashMap;

String installationToken = mintInstallationToken(); // Returns "ghs_..."

var env = new HashMap<>(System.getenv());
env.put("COPILOT_GITHUB_TOKEN", installationToken);

try (var client = new CopilotClient(new CopilotClientOptions()
        .setEnvironment(env)
        .setUseLoggedInUser(false))) {
    client.start().get();

    var session = client.createSession(new SessionConfig()
        .setModel("claude-sonnet-4.6")).get();
}
```

</details>

> [!NOTE]
> These examples configure the environment for the Copilot CLI process that the SDK spawns. If you connect to an already-running CLI server with a URI connection, set `COPILOT_GITHUB_TOKEN` in that server process instead.

## Refresh and rotate tokens

Installation access tokens expire after 1 hour. Cache tokens only until shortly before their expiry, then mint a fresh token.

When you refresh the token, start a new Copilot SDK client with an updated `COPILOT_GITHUB_TOKEN` value. The Copilot CLI subprocess reads its environment when it starts and does not re-read the token during an existing session.

Rotate the GitHub App private key according to your organization's security policy. If a token is exposed, revoke the installation token and rotate the app private key.

## What gets billed

Copilot usage is attributed to the account that owns the GitHub App installation used to mint the token:

* Organization installation: usage is attributed and billed to the organization.
* User installation: usage is attributed to the individual user account.

Use an organization installation for direct organization billing and automation scenarios that should not depend on a user's Copilot plan or personal access token.

## Troubleshooting

| Symptom | What to check |
|---|---|
| `401 Unauthorized` before reaching Copilot | Confirm the organization or enterprise is enabled for the Copilot GitHub App server-to-server flow. |
| `403 Resource not accessible by integration` or an error that mentions user info | Confirm you did not pass the `ghs_` token with `gitHubToken` or `github_token`. Pass it with `COPILOT_GITHUB_TOKEN` in the spawned CLI environment. |
| `403 Forbidden` from the Copilot API | Confirm the token was minted with `repository_ids` and explicit `permissions` containing `"copilot_requests": "write"`. |
| `403 Forbidden` after using the required mint body | Confirm the GitHub App installation has **All repositories** access, then mint a fresh token. |
| The requested model is unavailable | Confirm the organization's Copilot model policy allows the model and that the bundled Copilot CLI version supports it. |
| Requests are billed to the wrong account | Confirm the installation ID belongs to the organization, not a user account, before minting the token. |

## Further reading

* [Authenticate Copilot SDK](./authenticate.md): other SDK authentication methods and priority order
* [Creating a GitHub App](https://docs.github.com/en/apps/creating-github-apps): GitHub App setup
* [Authenticating as a GitHub App](https://docs.github.com/en/apps/creating-github-apps/authenticating-with-a-github-app/about-authentication-with-a-github-app): app JWTs and installation tokens
* [Generating an installation access token for a GitHub App](https://docs.github.com/en/apps/creating-github-apps/authenticating-with-a-github-app/generating-an-installation-access-token-for-a-github-app): token minting API
* [Requests in GitHub Copilot](https://docs.github.com/en/copilot/concepts/billing/copilot-requests): Copilot request accounting
